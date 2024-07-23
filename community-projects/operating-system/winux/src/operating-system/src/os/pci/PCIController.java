package os.pci;

public class PCIController {

    // 8 bit = 2^8
    public static final int BUSSES = 256;
    // 5 bit = 2^5
    public static final int DEVS = 32;
    // 3 bit = 2^3
    public static final int FCTS = 8;

    // write into address register...
    public static final int PCI_ADDRESS = 0x0CF8;

    // ...immediately receive data
    public static final int PCI_DATA = 0x0CFC;

    public static String getBaseClassCodeDescription(int code) {
        switch(code) {
            case 0x00: return "old device";
            case 0x01: return "mass storage";
            case 0x02: return "network controller";
            case 0x03: return "display controller";
            case 0x04: return "multimedia device";
            case 0x05: return "memory controller";
            case 0x06: return "bridge";
            case 0x07: return "communication controller";
            case 0x08: return "system periphery";
            case 0x09: return "input device";
            case 0x0A: return "docking station";
            case 0x0B: return "processor unit";
            case 0x0C: return "serial bus";
            case 0x0D: return "remote communication device";
            case 0x0E: return "intelligent controller";
            case 0x0F: return "satellite communication";
            default: return "unknown";
        }
    }

    /**
     * Write to PCI address
     *
     *    31          | 30        24 | 23     16 | 15      11 | 10    8 | 7             2 | 1                     0
     * Enable bit (1) | Reserved (0) |   Bus #   |  Device #  |  Fct #  | Register Offset | Offset cont. (always 0)
     *
     * @param busAddress 0-255: bus address
     * @param devNumber 0-31: device address on bus
     * @param funcAddress 0-7: function address of a device
     * @param register 0-63: register address of function
     */
    @SJC.Inline
    public static void writeAddress(int busAddress, int devNumber, int funcAddress, int register) {
        MAGIC.wIOs32(
                PCI_ADDRESS,
                0x80000000 | (busAddress << 16) | (devNumber << 11) | (funcAddress << 8) | (register << 2)
        );
    }

    @SJC.Inline
    public static int readData() {
        return MAGIC.rIOs32(PCI_DATA);
    }

    public static PCIDevice getDeviceAt(int bus, int dev, int fct) {
        /*
        Returned registers
        CC = ClassCode
                        +-------------+-------------+-------------+-------------+
                        | 31       24 | 23       16 | 15       08 | 07       00 |
                        +-------------+-------------+-------------+-------------+
        register = 0b00 |         Device ID         |         Vendor ID         |
        register = 0b01 |           Status          |          Command          |
        register = 0b10 |    BaseCC   |    SubCC    |  Interface |   Revision   |
        register = 0b11 |     BIST    |    Header   |   Latency  |     CLG      |
                        +-------------+-------------+-------------+-------------+
         */
        PCIDevice result;
        int data;

        writeAddress(bus, dev, fct, 0);
        data = readData();

        if(data == 0 || data == -1)
            return null;

        // register 0b00
        result = new PCIDevice();
        result.deviceID = data >>> 16;
        result.vendorID = data & 0xFFFF;

        // register 0b01
        writeAddress(bus, dev, fct, 1);
        data = readData();
        result.status = data >>> 16;
        result.command = data & 0xFFFF;

        // register 0b10
        writeAddress(bus, dev, fct, 2);
        data = readData();
        result.baseClassCode = data >>> 24;
        result.subClassCode = (data >>> 16) & 0xFF;
        result.interf = (data >>> 8) & 0xFF;
        result.revision = data & 0xFF;

        // register 0b11
        writeAddress(bus, dev, fct, 3);
        data = readData();
        result.bist = data >>> 24;
        result.header = (data >>> 16) & 0xFF;
        result.latency = (data >>> 8) & 0xFF;
        result.clg = data & 0xFF;

        // remaining data
        result.bus = bus;
        result.device = dev;
        result.function = fct;

        return result;
    }


    public static class DevGenerator {
        // 32   16 | 15   08 | 07   03 | 02   00
        //  empty  |   bus   |   dev   |   fct
        private int looper = 0;
        private static final int stopAt = 1 << 16;

        public PCIDevice next() {
            PCIDevice dev;
            while(looper < stopAt) {
                dev = getDeviceAt((looper >>> 8) & 0xFF, (looper >>> 3) & 0x1F, looper & 0x07);
                looper++;
                if(dev != null)
                    return dev;
            }
            return null;
        }
    }
}
