package kernel;

import arch.x86;
import gui.WindowManager;
import kernel.display.GraphicsContext;
import kernel.display.vesa.VESAGraphics;
import kernel.display.vesa.VESAMode;
import kernel.display.vesa.VesaQuery;
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
import java.util.vector.VecVesaMode;

public class Kernel
{
	public static final int RESOLUTION = 0;
	
	public static GraphicsContext Display;
	public static WindowManager WindowManager;
	
	public static void main()
	{
		Logger.logSerial("Initializing Kernel..\n");
		
		MemoryManager.initialize();
		Logger.logSerial("Initialized Memory Manager\n");
		
		Logger.initialize(Logger.TRACE, 100, false);
		Logger.info("BOOT", "Initialized Logger");
		
		SymbolResolution.initialize();
		Logger.info("BOOT", "Initialized Symbol Resolution");
		
		IDT.Initialize();
		Logger.info("BOOT", "Initialized Interrupt Descriptor Table");
		
		MAGIC.doStaticInit();
		Logger.info("BOOT", "Initialized Static Initializers");
		
		GarbageCollector.initialize();
		Logger.info("BOOT", "Initialized Garbage Collector");
		
		MemoryManager.disableGarbageCollection();
		Logger.info("BOOT", "Disabled Garbage Collection");
		
		VirtualMemory.enableVirtualMemory();
		Logger.info("BOOT", "Enabled Virtual Memory");
		
		PrintAllPciDevices();
		
		PIT.Initialize();
		Logger.info("BOOT", "Initialized PIT");
		
		PIT.SetRate(1000);
		Logger.info("BOOT", "Set PIT Rate to 1000Hz");
		
		KeyboardController.Initialize();
		Logger.info("BOOT", "Initialized PS2 Keyboard Controller");
		
		KeyboardController.SetLayout(new QWERTZ());
		Logger.info("BOOT", "Set Keyboard Layout to QWERTZ");
		
		MouseController.Initialize();
		Logger.info("BOOT", "Initialized PS2 Mouse Controller");
		
		IDT.Enable();
		Logger.info("BOOT", "Enabled Interrupts");
		
		Scheduler.initialize();
		Logger.info("BOOT", "Initialized Scheduler");
		
		VecVesaMode modes = VesaQuery.AvailableModes();
		PrintAllVesaModes(modes);
		VESAMode mode;
		switch (RESOLUTION)
		{
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
		Logger.info("BOOT", "Initialized Display");
		
		Display.ClearScreen();
		
		WindowManager = new WindowManager(Display);
		WindowManager.register();
		Logger.info("BOOT", "Initialized WindowManager");
		
		Scheduler.run();
	}
	
	private static void PrintAllPciDevices()
	{
		Logger.info("BOOT", "Detecting PCI Devices..");
		LazyPciDeviceReader reader = new LazyPciDeviceReader();
		while (reader.HasNext())
		{
			PciDevice device = reader.Next();
			if (device == null)
				continue;
			Logger.info("BOOT", "Found Device ".append(device.debug()));
		}
	}
	
	private static void PrintAllVesaModes(VecVesaMode modes)
	{
		for (int i = 0; i < modes.size(); i++)
		{
			VESAMode mode = modes.get(i);
			Logger.info("BOOT", "Mode ".append(i).append(": ").append(mode.debug()));
		}
	}
	
	public static void panic(String msg)
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		int eip = x86.eipForFunction(ebp);
		Bluescreen.show("PANIC", msg, ebp, eip);
		while (true)
		{
		}
	}
	
	public static void panic(int i)
	{
		panic(Integer.toString(i));
	}
}
