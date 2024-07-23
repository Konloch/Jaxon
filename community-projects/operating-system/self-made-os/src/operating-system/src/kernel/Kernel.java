package kernel;

import graphics.Console;
import graphics.ConsoleColors;
import hardware.Random;
import hardware.Serial;
import hardware.Time;
import rte.DynamicRuntime;
import sysutils.Scheduler;
import sysutils.exec.ExecutableStore;

public class Kernel
{
	private static final int GDTBASE = 0x10000;
	
	public static void main()
	{
		Serial.print("starting up... ");
		DynamicRuntime.initializeEmptyObjects();
		Serial.print("initialized empty objects... ");
		Interrupts.prepareInterrupts();
		Serial.print("initialized virtual memory... ");
		VirtualMemory.enableVirtualMemory();
		Serial.print("initialized interrupts... ");
		ExecutableStore.initializeStore();
		MAGIC.doStaticInit();
		//testConsole();
		Serial.print("initialized static variables\n");
		//set interrupt flag ERST WENN PICs
		MAGIC.inline(0xFB);
		Scheduler.init();
		Serial.print("initialized scheduler... starting scheduling.\n");
		//SETUP COMPLETE
		Random.srand((int) Time.getSystemTime());
		Scheduler.startScheduling();
	}
	
	public static void testConsole()
	{
		
		Console.clearConsole();
		Console.println("hallo :)");
		Console.setColor(ConsoleColors.FG_BLUE, ConsoleColors.BG_GREEN, false);
		Console.println();
		Console.println('a');
		Console.setCursor(5, 4);
		Console.println("ich bin versetzt :o");
		Console.print(12345);
		Console.print(-1234);
		Console.print(-678L);
		Console.println();
		Console.setColor(ConsoleColors.FG_WHITE, ConsoleColors.BG_BLACK, false);
		Console.println("rueckwaerts".reverse());
		Console.println(-12839L);
		Console.println(4321);
		Console.println();
		Console.printHex((byte) 0xF2);
		Console.println();
		Console.printHex((short) 0x4F0F); //???
		Console.println();
		Console.printHex(0xFFFF2442);
		Console.println();
		Console.printHex(0x0123456789ABCDEFL);
		while (true)
			;
	}
	
	public static void biosFun()
	{
		BIOS.enterGraphicsMode();
		for (int i = 0xA0000; i < 0xA0000 + 64000; i++)
		{
			MAGIC.wMem8(i, (byte) (i & 0x11));
		}
		Time.sleep(2000);
		BIOS.enterTextMode();
	}
	
	private static void enableVirtualMemory()
	{
	
	}
}
