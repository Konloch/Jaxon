package apps.tests;

import keyboard.Keyboard;
import keyboard.KeyboardListener;
import scheduling.Task;
import video.Printer;

/**
 * Test class for keyboard functionality.
 */
public class KeyboardTest extends Task
{
	private final Listener listener = new Listener();
	
	@Override
	protected void onStart()
	{
		Printer.directPrintString("[Shift] [Caps] [Ctrl] [Alt] [Return] [Backspace] [Tab] [Num]", 0, 0, Printer.WHITE, Printer.BLACK);
		Printer.directPrintString("--------------------------------------------------------------------------------", 0, 1, Printer.WHITE, Printer.BLACK);
		Keyboard.initstance().addListener(this.listener);
	}
	
	@Override
	protected void onSchedule()
	{
	
	}
	
	@Override
	protected void onStop()
	{
		Keyboard.initstance().removeListener(this.listener);
	}
	
	private static class Listener extends KeyboardListener
	{
		
		@Override
		public void onKeyDown(int value, int keyCode, boolean isChar, int flags)
		{
			if (isChar)
			{
				switch (value)
				{
					case 9:
						Printer.directPrintString("[Tab]", 49, 0, Printer.BLACK, Printer.WHITE);
						break;
					default:
						Printer.directPrintChar((char) value, 0, 2, Printer.BLACK, Printer.WHITE);
						break;
				}
			}
			else
			{
				switch (value)
				{
					case Keyboard.SHIFT_LEFT:
					case Keyboard.SHIFT_RIGHT:
						Printer.directPrintString("[Shift]", 0, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.CAPS_LOCK:
						Printer.directPrintString("[Caps]", 8, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.CTRL:
						Printer.directPrintString("[Ctrl]", 15, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.ALT:
						Printer.directPrintString("[Alt]", 22, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.RETURN:
						Printer.directPrintString("[Return]", 28, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.NUM_LOCK:
						Printer.directPrintString("[Num]", 55, 0, Printer.BLACK, Printer.WHITE);
						break;
					case Keyboard.BACKSPACE:
						Printer.directPrintString("[Backspace]", 37, 0, Printer.BLACK, Printer.WHITE);
						break;
				}
			}
		}
		
		@Override
		public void onKeyUp(int value, int keyCode, boolean isChar, int flags)
		{
			if (isChar)
			{
				switch (value)
				{
					case 9:
						Printer.directPrintString("[Tab]", 49, 0, Printer.WHITE, Printer.BLACK);
						break;
					default:
						Printer.directPrintChar((char) value, 0, 2, Printer.WHITE, Printer.BLACK);
						break;
				}
			}
			else
			{
				switch (value)
				{
					case Keyboard.SHIFT_LEFT:
					case Keyboard.SHIFT_RIGHT:
						Printer.directPrintString("[Shift]", 0, 0, Printer.WHITE, Printer.BLACK);
						break;
					case Keyboard.CAPS_LOCK:
						if ((flags & Keyboard.FLAG_CAPS_LOCK) == Keyboard.FLAG_CAPS_LOCK)
						{
							Printer.directPrintString("[Caps]", 8, 0, Printer.RED, Printer.BLACK);
						}
						else
						{
							Printer.directPrintString("[Caps]", 8, 0, Printer.WHITE, Printer.BLACK);
						}
						break;
					case Keyboard.CTRL:
						Printer.directPrintString("[Ctrl]", 15, 0, Printer.WHITE, Printer.BLACK);
						break;
					case Keyboard.ALT:
						Printer.directPrintString("[Alt]", 22, 0, Printer.WHITE, Printer.BLACK);
						break;
					case Keyboard.RETURN:
						Printer.directPrintString("[Return]", 28, 0, Printer.WHITE, Printer.BLACK);
						break;
					case Keyboard.NUM_LOCK:
						if ((flags & Keyboard.FLAG_NUM_LOCK) == Keyboard.FLAG_NUM_LOCK)
						{
							Printer.directPrintString("[Num]", 55, 0, Printer.RED, Printer.BLACK);
						}
						else
						{
							Printer.directPrintString("[Num]", 55, 0, Printer.WHITE, Printer.BLACK);
						}
						break;
					case Keyboard.BACKSPACE:
						Printer.directPrintString("[Backspace]", 37, 0, Printer.WHITE, Printer.BLACK);
						break;
				}
			}
		}
	}
}
