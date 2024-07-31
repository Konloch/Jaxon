package kernel.interrupt;

/**
 * Programmable Interrupt Controller
 */
public class PIC {
    public final static int MASTER = 0x20;
    public final static int SLAVE = 0xA0;

    public static void Initialize() {
        ProgrammChip(MASTER, 0x20, 0x04); // init offset and slave config of master
        ProgrammChip(SLAVE, 0x28, 0x02); // init offset and slave config of slave
    }

    @SJC.Inline
    public static void Acknowledge(int irq) {
        if (irq >= 8) {
            MAGIC.wIOs8(SLAVE, (byte) 0x20);
        }
        MAGIC.wIOs8(MASTER, (byte) 0x20);
    }

    private static void ProgrammChip(int port, int offset, int icw3) {
        MAGIC.wIOs8(port++, (byte) 0x11); // ICW1
        MAGIC.wIOs8(port, (byte) offset); // ICW2
        MAGIC.wIOs8(port, (byte) icw3); // ICW3
    }
}
