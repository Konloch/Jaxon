package kernel.hardware;

import arch.x86;

public class Timer
{
	private static int _tickCount = 0;
	
	@SJC.Inline
	public static void DoTick()
	{
		_tickCount++;
		if (_tickCount < 0)
		{
			_tickCount = 0;
		}
	}
	
	@SJC.Inline
	public static int Ticks()
	{
		return _tickCount;
	}
	
	public static void Sleep(int ms)
	{
		double rate = PIT.RateHz();
		int ticks = (int) (rate / 1000.0 * (double) ms);
		int start = Ticks();
		while (Ticks() - start < ticks)
		{
			// wait until next interrupt fires
			x86.hlt();
		}
	}
	
	@SJC.Inline
	public static int TickDifferenceMs(int start, int end)
	{
		double rate = PIT.RateHz();
		return (int) ((end - start) / rate * 1000.0);
	}
	
	@SJC.Inline
	public static int TickDifferenceMs(int ticks)
	{
		return TickDifferenceMs(0, ticks);
	}
	
	@SJC.Inline
	public static int TicksToMs(int ticks)
	{
		double rate = PIT.RateHz();
		return (int) (ticks / rate * 1000.0);
	}
}
