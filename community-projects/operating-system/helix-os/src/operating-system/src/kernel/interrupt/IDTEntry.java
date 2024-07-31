package kernel.interrupt;

/**
 * Represents an entry in the Interrupt Descriptor Table.
 * Each entry is 64 bits long.
 */
public class IDTEntry extends STRUCT {
    /**
     * The lower 16 bits of the interrupt handler's offset.
     * Bits 0-15
     */
    @SJC(offset = 0)
    public short offsetLow;

    /**
     * Identifies the segment in the GDT that contains the interrupt handler
     * code.
     * Bits 16-31
     */
    @SJC(offset = 2)
    public short selector;

    /**
     * Reserved. This field must be zero.
     * Bits 32-39
     */
    @SJC(offset = 4)
    public byte zero;

    /**
     * The type and attributes field contains a variety of flags that describe the
     * type of gate the descriptor represents.
     * Fields:
     * P (Present) - Bit 7
     * DPL (Descriptor Privilege Level) - Bits 5-6
     * Always 0 - Bit 4
     * D (Size of gate) - Bit 3
     * 1 - Bit 2
     * 1 - Bit 1
     * 0 - Bit 0
     * 
     * Bits 40-47
     */
    @SJC(offset = 5)
    public byte typeAttr;

    /**
     * The higher 16 bits of the interrupt handler's offset.
     * Bits 48-63
     */
    @SJC(offset = 6)
    public short offsetHigh;
}
