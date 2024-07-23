package apps.tests;

import scheduling.Task;
import timer.Timer;
import video.Printer;

/**
 * A task that outputs characters on the screen in an endless loop.
 */
public class NonCooperativeTask extends Task
{
	private final int delay = 500;
	
	private long lastChangedTime = 0;
	
	private int total = 5039;
	
	private final int constant = 39916801;
	
	@Override
	protected void onSchedule()
	{
		while (true)
		{
			if (Timer.getUpTime() - this.lastChangedTime > delay)
			{
				this.lastChangedTime = Timer.getUpTime();
				
				// Berechne neuen Wert
				this.total = this.total * this.constant + ((int) (this.lastChangedTime ^ (this.lastChangedTime >> 32)));
				char c = (char) ((this.total % 94) + 33);
				
				Printer.directPrintChar(c, 38, 10, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 39, 10, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 40, 10, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 41, 10, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 38, 11, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 39, 11, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 40, 11, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 41, 11, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 38, 12, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 39, 12, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 40, 12, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 41, 12, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 38, 13, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 39, 13, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 40, 13, Printer.WHITE, Printer.BLACK);
				Printer.directPrintChar(c, 41, 13, Printer.WHITE, Printer.BLACK);
			}
		}
	}
	
	@Override
	protected void onStop()
	{
		Printer.directPrintChar(' ', 38, 10, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 39, 10, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 40, 10, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 41, 10, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 38, 11, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 39, 11, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 40, 11, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 41, 11, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 38, 12, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 39, 12, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 40, 12, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 41, 12, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 38, 13, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 39, 13, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 40, 13, Printer.WHITE, Printer.BLACK);
		Printer.directPrintChar(' ', 41, 13, Printer.WHITE, Printer.BLACK);
	}
}
