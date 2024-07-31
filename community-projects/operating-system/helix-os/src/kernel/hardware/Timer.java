package kernel.hardware;

import arch.x86;

public class Timer
{
	private static int _tickCount = 0;
	
	@SJC.Inline
	public static void doTick()
	{
		_tickCount++;
		if (_tickCount < 0)
			_tickCount = 0;
	}
	
	@SJC.Inline
	public static int ticks()
	{
		return _tickCount;
	}
	
	public static void sleep(int ms)
	{
		double rate = PIT.rateHz();
		int ticks = (int) (rate / 1000.0 * (double) ms);
		int start = ticks();
		while (ticks() - start < ticks)
		{
			// wait until next interrupt fires
			x86.hlt();
		}
	}
	
	@SJC.Inline
	public static int tickDifferenceMs(int start, int end)
	{
		double rate = PIT.rateHz();
		return (int) ((end - start) / rate * 1000.0);
	}
	
	@SJC.Inline
	public static int tickDifferenceMs(int ticks)
	{
		return tickDifferenceMs(0, ticks);
	}
	
	@SJC.Inline
	public static int ticksToMs(int ticks)
	{
		double rate = PIT.rateHz();
		return (int) (ticks / rate * 1000.0);
	}
}
