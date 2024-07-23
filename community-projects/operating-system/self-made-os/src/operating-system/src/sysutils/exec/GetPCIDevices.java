package sysutils.exec;

import graphics.Console;
import hardware.pci.PCIController;
import hardware.pci.PCIDevice;
import sysutils.Scheduler;

class GetPCIDevices extends Executable
{
	private static final String[] baseClassCodes = {"old device", "mass storage", "network controller", "display controller", "multimedia device", "memory controller", "bridge", "communication controller", "system peripheral", "input device", "docking station", "processor", "serial bus", "wireless coms device", "intelligent controller", "satellite communication"};
	
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new GetPCIDevices();
			}
			
			@Override
			public String getName()
			{
				return "lspci";
			}
		});
	}
	
	@Override
	public int execute()
	{
		PCIDevice[] devs = PCIController.getRecognizedDevices();
		Console.print("Found ");
		Console.print(devs.length);
		Console.print(" devices:");
		Console.print('\n');
		Console.print("Device: Bus, Dev, Func, Class, Subclass, Vendor, device ");
		Console.print("");
		Console.print("");
		Console.print('\n');
		for (int i = 0; i < devs.length; i++)
		{
			PCIDevice dev = devs[i];
			Console.print(i);
			Console.print(":      ");
			Console.print(dev.bus);
			Console.print(",   ");
			Console.print(dev.device);
			Console.print(",   ");
			Console.print(dev.function);
			Console.print(",    ");
			Console.print(baseClassCodes[dev.baseClass]);
			Console.print(", ");
			Console.printHex(dev.subClass);
			Console.print(", ");
			Console.print(dev.vendorID);
			Console.print(",");
			Console.print(dev.devID);
			Console.print('\n');
		}
		Scheduler.markTaskAsFinished(this);
		return 0;
	}
	
}
