package kernel.memory;

public class BootableImage extends STRUCT {
    /**
     * / Start in memory. Points to itself and equals MAGIC.imageBase
     */
    @SJC(offset = 0)
    public int memoryStart;

    /**
     * Size of the boot image.
     */
    @SJC(offset = 4)
    public int memorySize;

    /**
     * Class descriptor address of kernel.Kernel
     */
    @SJC(offset = 8)
    public int classDescKernel;

    /**
     * Address of first code byte in start method (kernel.Kernel.main)
     */
    @SJC(offset = 12)
    public int startMethodCodeStart;

    /**
     * Address of first object
     */
    @SJC(offset = 16)
    public int firstHeapObject;

    /**
     * Address of RAM-init object in embedded mode
     */
    @SJC(offset = 20)
    public int ramInitObjectAddress;

    /**
     * Relative start of code in function (equals MAGIC.codeStart)
     */
    @SJC(offset = 24)
    public int relativeCodeStartInMethod;

    /**
     * lo to hi: 0xAA relocBytes stackClearBits 0x55 (arch parameters incl.
     * endianness)
     */
    @SJC(offset = 28)
    public int archParameters;
}
