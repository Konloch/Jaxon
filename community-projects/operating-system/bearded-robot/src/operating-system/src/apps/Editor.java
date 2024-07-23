package apps;

import bios.BIOS;
import keyboard.Keyboard;
import keyboard.KeyboardListener;
import scheduling.Task;
import video.Printer;

/**
 * A simple editor application.
 */
public class Editor extends Task
{
	/**
	 * The listener that is notified of keyboard events.
	 */
	private final Listener listener = new Listener(this);
	
	/**
	 * Printer for outputting the editor content.
	 */
	private final Printer printer = new Printer();
	
	/**
	 * The first EditorChar of the editor content.
	 */
	private final EditorChar firstChar = new EditorChar();
	
	/**
	 * The last EditorChar of the editor content.
	 */
	private EditorChar lastChar = firstChar;
	
	/**
	 * The EditorChar of the editor content on which the cursor is positioned.
	 */
	private EditorChar nowChar = firstChar;
	
	@Override
	protected void onStart()
	{
		// Cursor anzeigen
		BIOS.regs.EAX = 0x01 << 8;
		BIOS.regs.ECX = 0x0007;
		
		BIOS.rint(0x10);
		
		Keyboard.initstance().addListener(this.listener);
	}
	
	@Override
	protected void onSchedule()
	{
		printer.setCursor(0, 2);
		
		// Zeichenausgabe
		EditorChar c = firstChar;
		while (c != null)
		{
			if (c == nowChar)
			{
				// Cursor setzen
				BIOS.regs.EAX = 0x02 << 8;
				BIOS.regs.EBX = 0;
				BIOS.regs.EDX = (printer.getCursorY() << 8) | printer.getCursorX();
				
				BIOS.rint(0x10);
			}
			printer.setColor(Printer.WHITE, Printer.BLACK);
			
			if (c.value == '\n')
			{
				printer.print(' ');
				printer.println();
			}
			else
			{
				printer.print(c.value);
			}
			
			c = c.next;
		}
		
		printer.setColor(Printer.WHITE, Printer.BLACK);
	}
	
	@Override
	protected void onStop()
	{
		Keyboard.initstance().removeListener(this.listener);
		
		// Cursor ausblenden
		BIOS.regs.EAX = 0x01 << 8;
		BIOS.regs.ECX = 0x2607;
		
		BIOS.rint(0x10);
	}
	
	/**
	 * A listener that can be used to receive keyboard events.
	 */
	private static class Listener extends KeyboardListener
	{
		
		private final Editor editor;
		
		public Listener(Editor editor)
		{
			this.editor = editor;
		}
		
		@Override
		public void onKeyDown(int value, int keyCode, boolean isChar, int flags)
		{
			if (!isChar && value == Keyboard.RETURN)
			{
				isChar = true;
				value = '\n';
			}
			
			if (isChar)
			{
				this.editor.nowChar.value = (char) value;
				if (this.editor.nowChar.next == null)
				{
					this.editor.lastChar = new EditorChar();
					this.editor.nowChar.next = this.editor.lastChar;
					this.editor.lastChar.previous = this.editor.nowChar;
					this.editor.nowChar = this.editor.lastChar;
				}
				else
				{
					this.editor.nowChar = this.editor.nowChar.next;
				}
			}
			else
			{
				switch (value)
				{
					case Keyboard.BACKSPACE:
						if (this.editor.nowChar.previous != null)
						{
							this.editor.nowChar.previous.value = this.editor.nowChar.value;
							this.editor.nowChar.previous.next = this.editor.nowChar.next;
							if (this.editor.nowChar.next != null)
								this.editor.nowChar.next.previous = this.editor.nowChar.previous;
							this.editor.nowChar = this.editor.nowChar.previous;
						}
						break;
				}
			}
		}
		
		@Override
		public void onKeyUp(int value, int keyCode, boolean isChar, int flags)
		{
		
		}
	}
	
	/**
	 * A char wrapper that enables the double concatenation of chars.
	 */
	private static class EditorChar
	{
		public EditorChar next;
		public EditorChar previous;
		public char value = ' ';
	}
}
