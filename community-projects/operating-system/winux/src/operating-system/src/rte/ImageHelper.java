package rte;

public class ImageHelper {

    /**
     * Determine if object is baked into image or generated dynamically during runtime.
     *
     * @param o Object to check
     * @return true if o is static, false if o is created dynamically
     */
    @SJC.Inline
    public static boolean isStatic(Object o) {
        return MAGIC.cast2Ref(o) < getImageEnd();
    }

    /**
     * Count all objects that are generated statically, aka "baked" into the image.
     * Number shouldn't change.
     *
     * @return number of static objects
     */
    public static int countStaticObjects() {
        int count = 0;
        Object obj = getFirstObject();
        int imgEnd = getImageEnd();
        while(obj != null) {
            if(MAGIC.cast2Ref(obj) < imgEnd)
                count++;
            obj = obj._r_next;
        }

        return count;
    }

    /**
     * Count all objects generated at runtime, aka added above the image.
     * Number changes during runtime.
     *
     * @return number of runtime objects
     */
    public static int countRuntimeObjects() {
        int count = 0;
        Object obj = getFirstObject();
        int imgEnd = getImageEnd();
        while(obj != null) {
            if(MAGIC.cast2Ref(obj) >= imgEnd)
                count++;
            obj = obj._r_next;
        }

        return count;
    }

    /**
     * Count all objects, static and runtime.
     *
     * @return number of objects linked together
     */
    public static int countAllObjects() {
        int count = 0;
        Object obj = MAGIC.cast2Obj(MAGIC.rMem32(MAGIC.imageBase + 16));
        while(obj != null) {
            count++;
            obj = obj._r_next;
        }

        return count;
    }

    /**
     * Points to the first address not used by the image.
     */
    @SJC.Inline
    public static int getImageEnd() {
        return MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4);
    }

    /**
     * Get first object of image.
     * From here every other object (including EmptyObject) can be traversed with _r_next.
     *
     * @return first object in image
     */
    @SJC.Inline
    public static Object getFirstObject() {
        return MAGIC.cast2Obj(MAGIC.rMem32(MAGIC.imageBase + 16));
    }

    /**
     * Calculate actual start address of object by subtracting its relocEntries.
     */
    @SJC.Inline
    public static int startAddress(Object o) {
        return startAddress(MAGIC.cast2Ref(o), o._r_relocEntries);
    }

    /**
     * Calculate actual start address of object by subtracting its relocEntries (times its pointer size).
     *
     * @param ref Reference address to object. Call with {@link MAGIC#cast2Ref(Object)}
     * @param relocEntries Reloc entries in object. Call with {@code obj._r_relocEntries}
     * @return Start address of object
     */
    @SJC.Inline
    public static int startAddress(int ref, int relocEntries) {
        return ref - (relocEntries * MAGIC.ptrSize);
    }

    /**
     * Calculate end address of object by adding its scalarSize to its reference pointer.
     */
    @SJC.Inline
    public static int endAddress(Object o) {
        return endAddress(MAGIC.cast2Ref(o), o._r_scalarSize);
    }

    /**
     * Calculate end address of object by adding its scalarSize (+3 and last to bits set to 0)
     * to its reference pointer.
     *
     * @param ref Reference address to object. Call with {@link MAGIC#cast2Ref(Object)}
     * @param scalarSize Reloc entries in object. Call with {@code obj._r_relocEntries}
     * @return Start address of object
     */
    @SJC.Inline
    public static int endAddress(int ref, int scalarSize) {
        return ref + ((scalarSize+3)&~3);
    }

    /**
     * Find address that aligns to 4kb, thus setting the last 12 bits to 0.
     */
    @SJC.Inline
    public static int align4kBAddress(int address) {
        return (address + 0xFFF) & ~0xFFF;
    }
}
