package sysutils.exec;

import graphics.Console;
import sysutils.Scheduler;
import tests.StringBufferTest;

public class Test extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new Test();
			}
			
			@Override
			public String getName()
			{
				return "TEST";
			}
		});
	}
	
	@Override
	public int execute()
	{
		StringBufferTest.test();
		//https://wiki.osdev.org/VGA_Fonts#Get_from_VGA_RAM_directly
		//clear even/odd
		MAGIC.wIOs8(0x3CE, (byte) 0x5);
		//map VGA mem
		MAGIC.wIOs8(0x3CE, (byte) (0x406 & 0xFF));
		//set bplane 2
		MAGIC.wIOs8(0x3C4, (byte) (0x402 & 0xFF));
		//clear even/odd
		MAGIC.wIOs8(0x3C4, (byte) (0x604 & 0xFF));
		//try changing font of all characters
		int addr = 0xA0000;
		for (int i = 0; i < 256; i++)
		{
			MAGIC.wMem32(addr, 0xFFFFFFFF);
			addr += 4;
		}
		
		//restore
		MAGIC.wIOs8(0x3C4, (byte) (0x302 & 0xFF));
		MAGIC.wIOs8(0x3C4, (byte) (0x204 & 0xFF));
		MAGIC.wIOs8(0x3CE8, (byte) (0x1005 & 0xFF));
		MAGIC.wIOs8(0x3CE8, (byte) (0xE06 & 0xFF));
		Console.print("@AB");
		Scheduler.markTaskAsFinished(this);
		return 0;
	}
}
