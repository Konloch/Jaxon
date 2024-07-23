package dbg;

import interrupts.Interrupts;

public class Debugging
{
	private static final BluescreenExceptionHandler blueScreenHandler = new BluescreenExceptionHandler();
	
	public static void init()
	{
		Interrupts.HANDLERS[3] = blueScreenHandler;
		Interrupts.HANDLERS[5] = blueScreenHandler;
		Interrupts.HANDLERS[14] = blueScreenHandler;
	}
}
