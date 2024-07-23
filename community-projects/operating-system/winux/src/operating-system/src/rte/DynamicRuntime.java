package rte;

import devices.StaticV24;
import os.virtualMemory.VirtualMemory;

public class DynamicRuntime {

    //private static int nextFreeAddress = (MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase+4)+0xFFF)&~0xFFF;
    //private static int nextFreeAddress = MAGIC.rMem32(MAGIC.imageBase + 16);
//    private static int nextFreeAddress;
//    private static Object prev = null;

    public static EmptyObject firstEmptyObject = null;

    private static Object findLastObject() {
        Object obj = MAGIC.cast2Obj(MAGIC.rMem32(MAGIC.imageBase + 16));
        while(obj._r_next != null) {
            obj = obj._r_next;
        }
        return obj;
    }

    public static void initEmptyObjects(int firstFreeAddress) {
        int from, to, i;
        Object newObject;
        EmptyObject emptyObj, lastEmptyObj;

        firstFreeAddress = (MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4) + 0xFFF) & ~0xFFF;
        lastEmptyObj = null;

        StaticV24.print("EmptyObject relocs: ");
        StaticV24.println(MAGIC.getInstRelocEntries("EmptyObject"));
        StaticV24.print("firstFreeAddress: ");
        StaticV24.printHexln(firstFreeAddress);



        BIOS.SMG.reset(0x8000);

        while(BIOS.SMG.next()) {
            if(!BIOS.SMG.isTypeFree())
                continue;

            from = (int) BIOS.SMG.baseAddress;
            to = from + (int) BIOS.SMG.length;

            if(to < firstFreeAddress)
                continue;

            if(from < firstFreeAddress)
                from = firstFreeAddress;

            for(i = from; i < to; i += 4)
                MAGIC.wMem32(i, 0);

            // allocate object
            newObject = MAGIC.cast2Obj(from + (MAGIC.getInstRelocEntries("EmptyObject") << 2));
            MAGIC.assign(newObject._r_relocEntries, MAGIC.getInstRelocEntries("EmptyObject"));
            MAGIC.assign(newObject._r_type, MAGIC.clssDesc("EmptyObject"));

            // make sure to reserve the entire memory available
            MAGIC.assign(newObject._r_scalarSize, (int) BIOS.SMG.length);

            emptyObj = (EmptyObject) newObject;
            emptyObj.prev = emptyObj;
            if(firstEmptyObject == null) {
                firstEmptyObject = emptyObj;
                MAGIC.assign(findLastObject()._r_next, newObject);
            } else {
                if(lastEmptyObj == null) {
                    // this should never occur
                    lastEmptyObj = firstEmptyObject;
                    while(lastEmptyObj.next != null)
                        lastEmptyObj = lastEmptyObj.next;
                }

                lastEmptyObj.next = emptyObj;
            }

            lastEmptyObj = emptyObj;
        }
    }

    public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type) {
        EmptyObject space;
        Object o;

        if(firstEmptyObject == null) {
            // one can only wish...
//            int nextFreeAddress = VirtualMemory.initVirtualMemory();
//            initEmptyObjects(nextFreeAddress);

            initEmptyObjects(ImageHelper.align4kBAddress(ImageHelper.getImageEnd()));
        }

        // do we take the risk that we may not have found any free disk space and firstEmptyObject therefore is null?
        // ...... yes.

        // find empty object that is big enough
        space = firstEmptyObject;

        while(space != null) {
            o = space.addObject(scalarSize, relocEntries, type);
            if(o != null)
                return o;

            space = space.next;
        }

        // couldn't find EmptyObject with enough space
        MAGIC.inline(0xCC);
        return null;

//        int start, rs, i;
//
//        // generated object
//        Object me;
//
//        if (nextFreeAddress == 0) {
//            nextFreeAddress = (MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4) + 0xFFF) & ~0xFFF;
//        }
//
//        rs = relocEntries << 2;
//        scalarSize = (scalarSize + 3) & ~3;
//
//        // memory boundaries
//        start = nextFreeAddress;
//        nextFreeAddress += rs + scalarSize;
//
//        // initialize with 0
//        for (i = start; i < nextFreeAddress; i += 4)
//            MAGIC.wMem32(i, 0);
//
//        // initialize object
//        me = MAGIC.cast2Obj(start + rs);
//        MAGIC.assign(me._r_relocEntries, relocEntries);
//        MAGIC.assign(me._r_scalarSize, scalarSize);
//        MAGIC.assign(me._r_type, type);
//
//        if (prev != null) {
//            MAGIC.assign(prev._r_next, me);
//        }
//        prev = me;
//
//        return me;
    }

    public static SArray newArray(int length, int arrDim, int entrySize, int stdType,
                                  SClassDesc unitType) { //unitType is not for sure of type SClassDesc
        int scS, rlE;
        SArray me;

        if (stdType == 0 && unitType._r_type != MAGIC.clssDesc("SClassDesc"))
            MAGIC.inline(0xCC); //check type of unitType, we don't support interface arrays
        scS = MAGIC.getInstScalarSize("SArray");
        rlE = MAGIC.getInstRelocEntries("SArray");
        if (arrDim > 1 || entrySize < 0) rlE += length;
        else scS += length * entrySize;
        me = (SArray) newInstance(scS, rlE, MAGIC.clssDesc("SArray"));
        MAGIC.assign(me.length, length);
        MAGIC.assign(me._r_dim, arrDim);
        MAGIC.assign(me._r_stdType, stdType);
        MAGIC.assign(me._r_unitType, unitType);
        return me;
    }

    public static void newMultArray(SArray[] parent, int curLevel, int destLevel,
                                    int length, int arrDim, int entrySize, int stdType, SClassDesc clssType) {
        int i;

        if (curLevel + 1 < destLevel) { //step down one level
            curLevel++;
            for (i = 0; i < parent.length; i++) {
                newMultArray((SArray[]) ((Object) parent[i]), curLevel, destLevel,
                        length, arrDim, entrySize, stdType, clssType);
            }
        } else { //create the new entries
            destLevel = arrDim - curLevel;
            for (i = 0; i < parent.length; i++) {
                parent[i] = newArray(length, destLevel, entrySize, stdType, clssType);
            }
        }
    }

    public static boolean isInstance(Object o, SClassDesc dest, boolean asCast) {
        SClassDesc check;

        if (o == null) {
            if (asCast) return true; //null matches all
            return false; //null is not an instance
        }
        check = o._r_type;
        while (check != null) {
            if (check == dest) return true;
            check = check.parent;
        }
        if (asCast) MAGIC.inline(0xCC);
        return false;
    }

    public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast) {
        SIntfMap check;

        if (o == null) return null;
        check = o._r_type.implementations;
        while (check != null) {
            if (check.owner == dest) return check;
            check = check.next;
        }
        if (asCast) MAGIC.inline(0xCC);
        return null;
    }

    public static boolean isArray(SArray o, int stdType, SClassDesc clssType, int arrDim, boolean asCast) {
        SClassDesc clss;

        //in fact o is of type "Object", _r_type has to be checked below - but this check is faster than "instanceof" and conversion
        if (o == null) {
            if (asCast) return true; //null matches all
            return false; //null is not an instance
        }
        if (o._r_type != MAGIC.clssDesc("SArray")) { //will never match independently of arrDim
            if (asCast) MAGIC.inline(0xCC);
            return false;
        }
        if (clssType == MAGIC.clssDesc("SArray")) { //special test for arrays
            if (o._r_unitType == MAGIC.clssDesc("SArray"))
                arrDim--; //an array of SArrays, make next test to ">=" instead of ">"
            if (o._r_dim > arrDim) return true; //at least one level has to be left to have an object of type SArray
            if (asCast) MAGIC.inline(0xCC);
            return false;
        }
        //no specials, check arrDim and check for standard type
        if (o._r_stdType != stdType || o._r_dim < arrDim) { //check standard types and array dimension
            if (asCast) MAGIC.inline(0xCC);
            return false;
        }
        if (stdType != 0) {
            if (o._r_dim == arrDim) return true; //array of standard-type matching
            if (asCast) MAGIC.inline(0xCC);
            return false;
        }
        //array of objects, make deep-check for class type (PicOS does not support interface arrays)
        if (o._r_unitType._r_type != MAGIC.clssDesc("SClassDesc")) MAGIC.inline(0xCC);
        clss = o._r_unitType;
        while (clss != null) {
            if (clss == clssType) return true;
            clss = clss.parent;
        }
        if (asCast) MAGIC.inline(0xCC);
        return false;
    }

    public static void checkArrayStore(SArray dest, SArray newEntry) {
        if (dest._r_dim > 1) isArray(newEntry, dest._r_stdType, dest._r_unitType, dest._r_dim - 1, true);
        else if (dest._r_unitType == null) MAGIC.inline(0xCC);
        else isInstance(newEntry, dest._r_unitType, true);
    }


    public static int countEmptyObjects() {
        int count = 0;
        if(firstEmptyObject != null) {
            count++;
            EmptyObject iter = firstEmptyObject;

            while (iter.next != null) {
                count++;
                iter = iter.next;
            }
        }
        return count;
    }

    public static void nullException() {
        StaticV24.println("np");
        MAGIC.inline(0xCC);
    }
}
