package os.interrupt;

public class StackTraverser {

    private static final int STACK_ROOT = 0x9BFFC;

    private static int ebp;
    private static int eip;

    @SJC.Inline
    public static void reset(int ebp) {
        StackTraverser.ebp = ebp;

        // get current ebp offset
        // mov [ebp+xx],ebp
        MAGIC.inline(0x89,0x6D);
        MAGIC.inlineOffset(1,ebp);

//        // 9 PUSHA register values
        StackTraverser.eip = MAGIC.rMem32(ebp + 4);
//        StackTraverser.ebp = MAGIC.rMem32(ebp);
    }

    @SJC.Inline
    public static boolean next() {
        // read "normal mode"
        int oldEbp = ebp;
        ebp = MAGIC.rMem32(ebp);
        eip = MAGIC.rMem32(ebp + 4);
        return ebp <= STACK_ROOT && ebp >= 0 && oldEbp < ebp;
    }

    @SJC.Inline
    public static int getEbp() {
        return ebp;
    }

    @SJC.Inline
    public static int getEip() {
        return eip;
    }
}
