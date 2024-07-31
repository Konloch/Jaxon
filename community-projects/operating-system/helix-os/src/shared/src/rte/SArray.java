package rte;

/*
 * ScalarSize = 20 (12 from fields and 8 from Object)
 * RelocEntries = 3 (1 from fields and 2 from Object)
 */
@SJC.GenDesc
public class SArray {
    public final int length = 0;
    public final int _r_dim = 0;
    public final int _r_stdType = 0;
    public final Object _r_unitType = null;

    @SJC.Inline
    public static int getScalarSize() {
        return MAGIC.getInstScalarSize("SArray");
    }

    @SJC.Inline
    public static int getRelocEntries() {
        return MAGIC.getInstRelocEntries("SArray");
    }
}