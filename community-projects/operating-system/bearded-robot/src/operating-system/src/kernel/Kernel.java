package kernel;

import apps.BusyCrocodile;
import apps.Menu;
import bios.BIOS;
import dbg.Debugging;
import interrupts.Interrupts;
import keyboard.Keyboard;
import memory.Memory;
import rte.GarbageCollector;
import scheduling.Scheduler;
import timer.Timer;
import video.Printer;

@SuppressWarnings("UnusedDeclaration")
public class Kernel
{
	public static Scheduler scheduler;
	
	public static void main()
	{
		Printer.fillScreen(Printer.BLACK);
		Memory.init();
		Interrupts.init();
		Timer.init();
		Debugging.init();
		
		Interrupts.enableIRQs();
		
		scheduler = new Scheduler();
		scheduler.addTask(new BusyCrocodile(), true);
		scheduler.addTask(Keyboard.initstance(), true);
		scheduler.addTask(new Menu(), true);
		//scheduler.addTask(new KeyboardTest());
		//scheduler.addTask(new MemoryMonitor());
		scheduler.addTask(new GarbageCollector(), true);
		scheduler.start();
	}
	
	// Phase 3b
	public static void testMode13h()
	{
		// Switch to graphics mode.
		BIOS.regs.EAX = 0x0013;
		BIOS.rint(0x10);
		
		// Draw a house in the dark.
		for (int y = 0; y < 200; y++)
		{
			for (int x = 0; x < 320; x++)
			{
				if (y < 100)
				{
					if (x > 200 && x < 250 && y > 30)
					{
						if ((x > 210 && x < 220 && y > 40 && y < 50) || (x > 230 && x < 240 && y > 40 && y < 50) || (x > 210 && x < 220 && y > 60 && y < 70) || (x > 230 && x < 240 && y > 60 && y < 70) || (x > 230 && x < 240 && y > 80 && y < 90))
							MAGIC.wMem8(0xA0000 + x + 320 * y, (byte) 0x2C);
						else if (x > 210 && x < 220 && y > 80)
							MAGIC.wMem8(0xA0000 + x + 320 * y, (byte) 0xb8);
						else
							MAGIC.wMem8(0xA0000 + x + 320 * y, (byte) 0x13);
					}
					else
						MAGIC.wMem8(0xA0000 + x + 320 * y, (byte) 0x01);
				}
				else
					MAGIC.wMem8(0xA0000 + x + 320 * y, (byte) 0xbf);
			}
		}
		
		// HACK: 'Stop'; effect does not occur with computers that are too powerful
		// occurs.
		// TODO: Timer-based waiting
		for (int i = 0; i < 1000000000; i++)
		{
		}
		
		// Switch back to text mode
		BIOS.regs.EAX = 0x0003;
		BIOS.rint(0x10);
		
		Printer p = new Printer();
		p.println("End");
	}
}
