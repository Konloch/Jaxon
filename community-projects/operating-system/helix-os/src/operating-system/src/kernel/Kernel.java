package kernel;

import arch.x86;
import gui.WindowManager;
import kernel.display.vesa.VESAGraphics;
import kernel.display.vesa.VESAMode;
import kernel.display.vesa.VesaQuery;
import kernel.display.GraphicsContext;
import kernel.hardware.PIT;
import kernel.hardware.keyboard.KeyboardController;
import kernel.hardware.keyboard.layout.QWERTZ;
import kernel.hardware.mouse.MouseController;
import kernel.hardware.pci.LazyPciDeviceReader;
import kernel.hardware.pci.PciDevice;
import kernel.interrupt.IDT;
import kernel.memory.GarbageCollector;
import kernel.memory.MemoryManager;
import kernel.memory.VirtualMemory;
import kernel.schedule.Scheduler;
import kernel.trace.Bluescreen;
import kernel.trace.SymbolResolution;
import kernel.trace.logging.Logger;
import util.vector.VecVesaMode;

public class Kernel {
    public static final int RESOLUTION = 0;

    public static GraphicsContext Display;
    public static WindowManager WindowManager;

    public static void main() {
        Logger.LogSerial("Initializing Kernel..\n");

        MemoryManager.Initialize();
        Logger.LogSerial("Initialized Memory Manager\n");

        Logger.Initialize(Logger.TRACE, 100, false);
        Logger.Info("BOOT", "Initialized Logger");

        SymbolResolution.Initialize();
        Logger.Info("BOOT", "Initialized Symbol Resolution");

        IDT.Initialize();
        Logger.Info("BOOT", "Initialized Interrupt Descriptor Table");

        MAGIC.doStaticInit();
        Logger.Info("BOOT", "Initialized Static Initializers");

        GarbageCollector.Initialize();
        Logger.Info("BOOT", "Initialized Garbage Collector");

        MemoryManager.DisableGarbageCollection();
        Logger.Info("BOOT", "Disabled Garbage Collection");

        VirtualMemory.EnableVirtualMemory();
        Logger.Info("BOOT", "Enabled Virtual Memory");

        PrintAllPciDevices();

        PIT.Initialize();
        Logger.Info("BOOT", "Initialized PIT");

        PIT.SetRate(1000);
        Logger.Info("BOOT", "Set PIT Rate to 1000Hz");

        KeyboardController.Initialize();
        Logger.Info("BOOT", "Initialized PS2 Keyboard Controller");

        KeyboardController.SetLayout(new QWERTZ());
        Logger.Info("BOOT", "Set Keyboard Layout to QWERTZ");

        MouseController.Initialize();
        Logger.Info("BOOT", "Initialized PS2 Mouse Controller");

        IDT.Enable();
        Logger.Info("BOOT", "Enabled Interrupts");

        Scheduler.Initialize();
        Logger.Info("BOOT", "Initialized Scheduler");

        VecVesaMode modes = VesaQuery.AvailableModes();
        PrintAllVesaModes(modes);
        VESAMode mode;
        switch (RESOLUTION) {
            case 0:
                mode = VesaQuery.GetMode(modes, 1280, 800, 32, true);
                break;
            case 1:
                mode = VesaQuery.GetMode(modes, 1440, 900, 32, true);
                break;
            default:
                panic("Invalid Resolution value");
                return;
        }

        Display = new VESAGraphics(mode);
        Display.Activate();
        Logger.Info("BOOT", "Initialized Display");

        Display.ClearScreen();

        WindowManager = new WindowManager(Display);
        WindowManager.Register();
        Logger.Info("BOOT", "Initialized WindowManager");

        Scheduler.Run();
    }

    private static void PrintAllPciDevices() {
        Logger.Info("BOOT", "Detecting PCI Devices..");
        LazyPciDeviceReader reader = new LazyPciDeviceReader();
        while (reader.HasNext()) {
            PciDevice device = reader.Next();
            if (device == null)
                continue;
            Logger.Info("BOOT", "Found Device ".append(device.Debug()));
        }
    }

    private static void PrintAllVesaModes(VecVesaMode modes) {
        for (int i = 0; i < modes.Size(); i++) {
            VESAMode mode = modes.Get(i);
            Logger.Info("BOOT", "Mode ".append(i).append(": ").append(mode.Debug()));
        }
    }

    public static void panic(String msg) {
        int ebp = 0;
        MAGIC.inline(0x89, 0x6D);
        MAGIC.inlineOffset(1, ebp);
        int eip = x86.eipForFunction(ebp);
        Bluescreen.Show("PANIC", msg, ebp, eip);
        while (true) {
        }
    }

    public static void panic(int i) {
        panic(Integer.toString(i));
    }
}
