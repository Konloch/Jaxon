package keyboard;

import interrupts.InterruptHandler;
import kernel.Kernel;

class KeyboardInterruptHandler extends InterruptHandler
{
	/**
	 * The following bytes are required for a complete scan code
	 * are.
	 */
	private int remaining;
	
	/**
	 * Previous parts of a scan code.
	 */
	private int scanCodeBuffer;
	
	/**
	 * The last byte values.
	 */
	private int resentBuffer;
	
	@Override
	public void onInterrupt(int number, boolean hasErrorCode, int errorCode)
	{
		int b = MAGIC.rIOs8(0x60) & 0xFF; // Unsigned conversion
		
		// Breakpoint exception with Ctrl+Shift+Esc
		resentBuffer = ((resentBuffer << 8) | b) & 0xFFFFFF;
		if (resentBuffer == 0x1D2A01 || resentBuffer == 0x2A1D01)
		{
			MAGIC.inline(0xCC);
		}
		
		// Cancel current task and reset scheduler
		if ((resentBuffer & 0xFFFF) == 0x1D01 && Kernel.scheduler != null)
		{
			// Determine EBP from ISR
			int ebp = 0;
			MAGIC.inline(0x89, 0x6D);
			MAGIC.inlineOffset(1, ebp); //mov [ebp+xx],ebp
			ebp = MAGIC.rMem32(ebp); // Exclude interrupt handler
			
			Kernel.scheduler.stopCurrent();
			Kernel.scheduler.reset(ebp);
			return;
		}
		
		if (remaining > 0)
		{
			scanCodeBuffer = (scanCodeBuffer << 8) | b;
			remaining--;
		}
		else
		{
			if (b >= 0xE2)
				return;
			
			scanCodeBuffer = b;
			remaining = b - 0xDF;
		}
		
		if (remaining <= 0)
			Keyboard.initstance().buffer.push(scanCodeBuffer);
	}
}
