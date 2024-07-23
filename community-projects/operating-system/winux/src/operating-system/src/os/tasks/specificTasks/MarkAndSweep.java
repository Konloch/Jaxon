package os.tasks.specificTasks;

import devices.StaticV24;
import os.tasks.CommandTask;
import rte.DynamicRuntime;
import rte.EmptyObject;
import rte.ImageHelper;

public class MarkAndSweep extends CommandTask {

    private static final int negBit = 1 << 31;

    @SJC.Inline
    private static void mark(Object o) {
        o._r_scalarSize |= negBit;
    }

    @SJC.Inline
    private static void unmark(Object o) {
        o._r_scalarSize &= ~negBit;
    }

    @SJC.Inline
    private static boolean isMarked(Object o) {
        return (o._r_scalarSize & negBit) != 0;
    }

    private EmptyObject lastEmptyObject;

    public MarkAndSweep() {
        super("sweep", "Apply mark and sweep", null);
    }

    @Override
    public void run() {
        Object o;
        int total, part;
        int imageEnd = MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4);

        // "Clear Interrupt Flag", disable interrupts
        MAGIC.inline(0xFA);

        StaticV24.printHex(DynamicRuntime.firstEmptyObject._r_scalarSize, 8);
        StaticV24.println();

        /*
         * MARK
         */
        StaticV24.println("Marking objects...");
        o = ImageHelper.getFirstObject();
        total = 0;
        while(o != null) {
            mark(o);
            o = o._r_next;
            total++;
        }

        StaticV24.print("Marked ");
        StaticV24.print(total);
        StaticV24.println(" objects");

        /*
         * TRAVERSE
         */
        StaticV24.println("Checking which ones can be sweeped...");
        o = ImageHelper.getFirstObject();
        part = 0;
        while(o != null && MAGIC.cast2Ref(o) < imageEnd) {
            part += traverseObject(o);
            o = o._r_next;
        }
        StaticV24.print(part);
        StaticV24.println(" objects will stay");

        /*
         * SWEEP
         */
        StaticV24.println("Sweeping...");
        lastEmptyObject = DynamicRuntime.firstEmptyObject;
        while(lastEmptyObject.next != null)
            lastEmptyObject = lastEmptyObject.next;

        part = sweep();

        StaticV24.print("Sweeped ");
        StaticV24.print(part);
        StaticV24.print(" of ");
        StaticV24.print(total);
        StaticV24.println(" objects");
        // "Set Interrupt Flag", enable interrupts
        MAGIC.inline(0xFB);


        out.print(part);
        out.print(" of ");
        out.print(total);
        out.println(" objects sweeped.");
        setDone(true);
    }

    /**
     * If object is marked, go through each of its relocEntries and recursively unmark them as well.
     *
     * @param o Marked object. If o is unmarked, it is ignored.
     * @return Number of objects unmarked. If o is unmarked, it returns 0, else at least 1.
     */
    private int traverseObject(Object o) {
        int i, addr, relocRef, count;

        if(!isMarked(o)) {
            // already unmarked, nothing to traverse (since all objects below are also unmarked)
            return 0;
        }

        addr = MAGIC.cast2Ref(o);

        unmark(o);

        count = 1;
        for(i = 3; i < o._r_relocEntries; i++) {
            // Object layout (each > is 8 byte)
            // > ...
            // > Inst-Reloc 2
            // > Inst-Reloc 1
            // > _r_next
            // > _r_type
            // > _r_relocEntries <- pointing to this
            // > _r_scalarSize
            // > Inst-Ska 1
            // > Inst-Ska 2
            // > ...
            count += traverseObject(MAGIC.cast2Obj(MAGIC.rMem32(addr - i * MAGIC.ptrSize)));
        }

        return count;
    }

    /**
     * Resize sweepable object depending on how many of its neighboring objects can also be sweeped.
     *
     * (!) The negative bit is removed in this process (!)
     *
     * _r_next and _r_scalarSize are therefore automatically adjusted.
     *
     * @return number of neighbouring objects sweeped (including {@code current}, so at least 1)
     */
    private int checkNeighbouringMarks(Object current) {
        Object o;
        int oSize;
        int endOfObject = ImageHelper.endAddress(current);
        int startOfObject = ImageHelper.startAddress(current);

        int sweeped = 1;
        unmark(current);
        for(o = current._r_next; o != null; o = o._r_next) {

            if(isMarked(o) && (ImageHelper.endAddress(o) == startOfObject || ImageHelper.startAddress(o) == endOfObject)) {
                current._r_next = o._r_next;

                unmark(o);
                oSize = (o._r_relocEntries * MAGIC.ptrSize) + o._r_scalarSize;
                current._r_scalarSize += oSize;
                endOfObject += oSize;
                sweeped++;
            }
        }

        return sweeped;
    }

    /**
     * Sweep all sweepable objects. Objects are sweepable that have the negative bit set in _r_scalarSize.
     *
     * @return number of objects sweeped
     */
    private int sweep() {
        Object current;
        int sweeped = 0;

        current = ImageHelper.getFirstObject();
        while(current != null) {
            if(isMarked(current)) {
                sweeped += checkNeighbouringMarks(current);
                addEmptyObject(current);
            }
            current = current._r_next;
        }
        return sweeped;
    }

    /**
     * Turn object into EmptyObject
     */
    private void addEmptyObject(Object obj) {
        int address = ImageHelper.startAddress(obj) + (MAGIC.getInstRelocEntries("EmptyObject") * MAGIC.ptrSize);
        int scalarSize = ImageHelper.endAddress(obj) - address;
        EmptyObject emptyObj;

        for (int i = ImageHelper.startAddress(obj); i < ImageHelper.endAddress(obj); i+=4) {
            MAGIC.wMem32(i, 0);
        }

        obj = MAGIC.cast2Obj(address);
        MAGIC.assign(obj._r_relocEntries, MAGIC.getInstRelocEntries("EmptyObject"));
        MAGIC.assign(obj._r_type, MAGIC.clssDesc("EmptyObject"));

        // make sure to reserve the entire memory available
        MAGIC.assign(obj._r_scalarSize, scalarSize);


        emptyObj = (EmptyObject) obj;
        emptyObj.prev = emptyObj;
        emptyObj.next = null;

        lastEmptyObject.next = emptyObj;
        lastEmptyObject = emptyObj;
    }

    @Override
    public void setup(String[] args) {
        setDone(false);
    }
}
