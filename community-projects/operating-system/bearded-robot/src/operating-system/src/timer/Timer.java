package timer;

import interrupts.Interrupts;

public class Timer
{
	static long upTime = 0;
	
	public static void init()
	{
		// One interrupt per millisecond (1193182Hz / 1000Hz = 1193)
		MAGIC.wIOs8(0x43, (byte) 0x36); // Select channel 0
		MAGIC.wIOs8(0x40, (byte) (1193 & 0xFF)); // lower divisor byte
		MAGIC.wIOs8(0x40, (byte) ((1193 >> 8) & 0xFF)); // upper divisor byte
		
		Interrupts.HANDLERS[32] = new TimerInterruptHandler();
	}
	
	public static long getUpTime()
	{
		return upTime;
	}
}
