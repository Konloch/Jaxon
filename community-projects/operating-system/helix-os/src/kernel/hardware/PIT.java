package kernel.hardware;

import kernel.interrupt.IDT;
import kernel.interrupt.PIC;

/**
 * The Programmable Interval Timer class represents a hardware timer used
 * for generating periodic interrupts.
 */
public class PIT
{
	public static final int IRQ_PIT = 0;
	private static final int PIT_A = 0x40;
	private static final int PIT_CTRL = 0x43;
	private static final byte PIT_SET = (byte) 0x36;
	private static final double INTERNAL_CLOCK_SPEED = 1193131.666;
	private static double _rateHz = 18.2;
	
	public static void initialize()
	{
		int dscAddr = MAGIC.cast2Ref(MAGIC.clssDesc("PIT"));
		int handlerOffset = IDT.codeOffset(dscAddr, MAGIC.mthdOff("PIT", "TimerHandler"));
		IDT.registerIrqHandler(IRQ_PIT, handlerOffset);
	}
	
	@SJC.Interrupt
	public static void timerHandler()
	{
		Timer.doTick();
		PIC.acknowledge(IRQ_PIT);
	}
	
	/**
	 * Sets the rate of the PIT timer.
	 * Not really accurate. Either set to 100 or 1000 Hz.
	 *
	 * @param hz The desired rate in Hz.
	 */
	public static void setRate(int hz)
	{
		short divisor = (short) (INTERNAL_CLOCK_SPEED / hz);
		MAGIC.wIOs8(PIT_CTRL, PIT_SET);
		MAGIC.wIOs8(PIT_A, (byte) (divisor & 0xFF));
		MAGIC.wIOs8(0x40, (byte) ((divisor >> 8) & 0xFF));
		_rateHz = hz;
	}
	
	/**
	 * Returns the current rate of the PIT timer in Hz.
	 *
	 * @return The current rate in Hz.
	 */
	@SJC.Inline
	public static double rateHz()
	{
		return _rateHz;
	}
	
}
