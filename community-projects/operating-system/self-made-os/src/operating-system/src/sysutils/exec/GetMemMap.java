package sysutils.exec;

import graphics.Console;
import kernel.BIOS;
import sysutils.Scheduler;

class GetMemMap extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new GetMemMap();
			}
			
			@Override
			public String getName()
			{
				return "memmap";
			}
		});
	}
	
	@Override
	public int execute()
	{
		Console.print("Memory segments according to BIOS:\n");
		int conIndex = 0;
		do
		{
			BIOS.BIOSMemSeg seg = BIOS.getMemMap(conIndex);
			conIndex = BIOS.regs.EBX;
			print(seg);
		} while (conIndex != 0);
		Scheduler.markTaskAsFinished(this);
		return 0;
	}
	
	private void print(BIOS.BIOSMemSeg seg)
	{
		Console.printHex(seg.baseAddr);
		Console.print('-');
		Console.printHex(seg.len + seg.baseAddr - 1);
		Console.print('-');
		if (seg.type != 1)
		{
			Console.print("reserved");
		}
		else
		{
			Console.print("free");
		}
		Console.print('\n');
	}
	
}
