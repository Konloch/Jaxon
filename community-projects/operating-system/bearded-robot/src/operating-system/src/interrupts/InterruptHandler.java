package interrupts;

/**
 * Base class for all interrupt handler classes. A class that extends this
 * class can be notified by ISRs.
 */
public abstract class InterruptHandler
{
	public abstract void onInterrupt(int number, boolean hasErrorCode, int errorCode);
}
