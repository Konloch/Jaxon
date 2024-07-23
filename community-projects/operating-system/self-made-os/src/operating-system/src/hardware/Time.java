package hardware;


public class Time
{
	private static long systemTime = 0;
	
	public static long getSystemTime()
	{
		return systemTime;
	}
	
	public static void increaseSystemTime(long ms)
	{
		systemTime += ms;
		if (systemTime < 0)
			systemTime = 0;
	}
	
	public static void sleep(long ms)
	{
		long now = systemTime;
		//noinspection StatementWithEmptyBody
		while (systemTime < now + ms);
		
	}
	
	//reads and returns clock counter
	public static long readTSC()
	{
		long res = 0l;
		MAGIC.inline(0x0F, 0x31); //rdtsc
		MAGIC.inline(0x89, 0x55);
		MAGIC.inlineOffset(1, res, 4);
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, res, 0);
		return res;
	}
	
	//reads and returns RTC time in seconds
	public static long readRTC()
	{
		MAGIC.wIOs8(0x70, (byte) 4);
		int hour = MAGIC.rIOs8(0x71);
		MAGIC.wIOs8(0x70, (byte) 2);
		int minute = MAGIC.rIOs8(0x71);
		MAGIC.wIOs8(0x70, (byte) 0);
		int second = MAGIC.rIOs8(0x71);
		return (long) ((hour * 60 + minute) * 60 + second);
	}
}
