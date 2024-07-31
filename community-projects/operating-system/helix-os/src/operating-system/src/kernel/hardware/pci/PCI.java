package kernel.hardware.pci;

import kernel.Kernel;
import util.BitHelper;

public class PCI {
    public static final int MAX_DEVICES = 32;
    public static final int MAX_BUS = 256;
    public static final int MAX_FUNCTIONS = 8;

    public static final int CONFIG_ADDRESS = 0x0CF8;
    public static final int CONFIG_DATA = 0x0CFC;

    public static PciDevice Read(int busIdx, int deviceIdx, int functionIdx) {
        int addrReg0 = BuildAddress(0, functionIdx, deviceIdx, busIdx);
        int addrReg1 = BuildAddress(1, functionIdx, deviceIdx, busIdx);
        int addrReg2 = BuildAddress(2, functionIdx, deviceIdx, busIdx);
        int addrReg3 = BuildAddress(3, functionIdx, deviceIdx, busIdx);

        MAGIC.wIOs32(CONFIG_ADDRESS, addrReg0);
        int dataReg0 = MAGIC.rIOs32(CONFIG_DATA);

        if (dataReg0 == 0 || dataReg0 == -1) {
            return null;
        }

        MAGIC.wIOs32(CONFIG_ADDRESS, addrReg1);
        int dataReg1 = MAGIC.rIOs32(CONFIG_DATA);
        MAGIC.wIOs32(CONFIG_ADDRESS, addrReg2);
        int dataReg2 = MAGIC.rIOs32(CONFIG_DATA);
        MAGIC.wIOs32(CONFIG_ADDRESS, addrReg3);
        int dataReg3 = MAGIC.rIOs32(CONFIG_DATA);

        int vendorId = BitHelper.GetRange(dataReg0, 0, 16);
        int deviceId = BitHelper.GetRange(dataReg0, 16, 16);
        int command = BitHelper.GetRange(dataReg1, 0, 16);
        int status = BitHelper.GetRange(dataReg1, 16, 16);
        int revision = BitHelper.GetRange(dataReg2, 0, 8);
        int itf = BitHelper.GetRange(dataReg2, 8, 8);
        int subclasscode = BitHelper.GetRange(dataReg2, 16, 8);
        int baseclasscode = BitHelper.GetRange(dataReg2, 24, 8);
        int cls = BitHelper.GetRange(dataReg3, 0, 8);
        int latency = BitHelper.GetRange(dataReg3, 8, 8);
        int header = BitHelper.GetRange(dataReg3, 16, 8);
        int bist = BitHelper.GetRange(dataReg3, 24, 8);

        PciDevice device = new PciDevice(
                busIdx, deviceIdx, functionIdx,
                vendorId, deviceId, command, status,
                revision, itf, subclasscode, baseclasscode,
                cls, latency, header, bist);
        return device;
    }

    public static int BuildAddress(int register, int function, int device, int bus) {
        if (register < 0 || register > 63
                || bus < 0 || bus > 255
                || device < 0 || device > 31
                || function < 0 || function > 7) {
            Kernel.panic("PCI build Addr invalid Params");
        }

        int addr = 0;
        addr = BitHelper.SetRange(addr, 0, 2, 0);
        addr = BitHelper.SetRange(addr, 2, 6, register);
        addr = BitHelper.SetRange(addr, 8, 3, function);
        addr = BitHelper.SetRange(addr, 11, 5, device);
        addr = BitHelper.SetRange(addr, 16, 8, bus);
        addr = BitHelper.SetRange(addr, 24, 8, 0x80);

        return addr;
    }
}