package kernel.bios.call;

import kernel.bios.BIOS;

public class DisplayModes extends BIOS {
    public static void ActivateGraphicsMode() {
        BIOS.Registers.EAX = 0x0013;
        BIOS.rint(0x10);
    }

    public static void ActivateTextMode() {
        BIOS.Registers.EAX = 0x0003;
        BIOS.rint(0x10);
    }

    public static void SetVesaMode(int modeNr) {
        Registers.EAX = 0x4F02;
        Registers.EBX = modeNr;
        BIOS.rint(0x10);
    }
}
