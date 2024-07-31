package kernel.display.text;

import kernel.MemoryLayout;
import kernel.memory.Memory;
import java.util.NoAllocConv;

public class TM3
{
	private static final TMMemory VidMem = (TMMemory) MAGIC.cast2Struct(MemoryLayout.VGA_TEXT_BUFFER_START);
	
	public static final int LINE_LENGTH = 80;
	public static final int LINE_COUNT = 25;
	public static final int MAX_CURSOR = LINE_LENGTH * LINE_COUNT;
	
	private static final int BUFFER_START = MemoryLayout.VGA_TEXT_BUFFER_START;
	private static final int BUFFER_SIZE_BYTES = MAX_CURSOR * 2;
	private static final int BUFFER_END = BUFFER_START + BUFFER_SIZE_BYTES;
	private static final int LINE_SIZE_BYTES = LINE_LENGTH * 2;
	
	private int _cursorPos;
	private static int _onScreenCursorPos;
	
	public TM3Brush Brush;
	
	public TM3()
	{
		this._cursorPos = 0;
		this.Brush = new TM3Brush();
	}
	
	@SJC.Inline
	public void setCursor(int line, int column)
	{
		_cursorPos = index1D(line, column);
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void setCursorPos(int idx)
	{
		_cursorPos = idx;
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public int cursorPos()
	{
		return _cursorPos;
	}
	
	@SJC.Inline
	public int currentLine()
	{
		return _cursorPos / LINE_LENGTH;
	}
	
	@SJC.Inline
	public void print(byte b)
	{
		setCharacterByte(b);
		updateCursorCaretDisplay();
	}
	
	public void print(String str)
	{
		for (int i = 0; i < str.length(); i++)
			setCharacterByte((byte) str.get(i));
		
		updateCursorCaretDisplay();
	}
	
	public void print(char[] chars)
	{
		for (char c : chars)
			setCharacterByte((byte) c);
		
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void print(char c)
	{
		setCharacterByte((byte) c);
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void print(boolean b)
	{
		print(b ? "true" : "false");
	}
	
	public void print(int n, int base)
	{
		int max_len = MAX_CURSOR - _cursorPos;
		int old_pos = _cursorPos;
		_cursorPos += NoAllocConv.iToA(MAGIC.cast2Ref(VidMem) + _cursorPos * 2, 2, max_len, n, base);
		
		// Set color for the printed number
		for (int i = old_pos; i < _cursorPos; i++)
			VidMem.cells[i].color = Brush.color();
		
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void print(int n)
	{
		print(n, 10);
	}
	
	@SJC.Inline
	public void printLn()
	{
		_cursorPos = sNewLine(_cursorPos);
		shiftIfOutOfBounds();
		updateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void printLn(byte b)
	{
		print(b);
		printLn();
	}
	
	public void printLn(char[] chars)
	{
		print(chars);
		printLn();
	}
	
	@SJC.Inline
	public void printLn(boolean b)
	{
		print(b);
		printLn();
	}
	
	@SJC.Inline
	public void printLn(char c)
	{
		print(c);
		printLn();
	}
	
	@SJC.Inline
	public void printLn(String str)
	{
		print(str);
		printLn();
	}
	
	@SJC.Inline
	public void printLn(int n, int base)
	{
		print(n, base);
		printLn();
	}
	
	@SJC.Inline
	public void printLn(int n)
	{
		print(n);
		printLn();
	}
	
	@SJC.Inline
	public static int sPrint(char c, int position, int color)
	{
		VidMem.cells[position].character = (byte) c;
		VidMem.cells[position].color = (byte) color;
		return position + 1;
	}
	
	@SJC.Inline
	public static int sPrintln(char c, int position, int color)
	{
		int newPos = sPrint(c, position, color);
		return sNewLine(newPos);
	}
	
	public static int sPrint(String s, int position, int color)
	{
		for (int i = 0; i < s.length(); i++)
			sPrint((char) s.get(i), position + i, color);
		
		return position + s.length();
	}
	
	public static int sPrint(String s, int position, int color, int maxLen)
	{
		int len = Math.min(s.length(), maxLen - 3);
		for (int i = 0; i < len; i++)
			sPrint((char) s.get(i), position + i, color);
		
		if (len < s.length())
		{
			sPrint('.', position + len, color);
			sPrint('.', position + len + 1, color);
			sPrint('.', position + len + 2, color);
		}
		
		return position + len;
	}
	
	@SJC.Inline
	public static int sPrintln(String s, int position, int color)
	{
		int newPos = sPrint(s, position, color);
		return sNewLine(newPos);
	}
	
	public static int sPrint(int n, int base, int position, int color)
	{
		int max_len = MAX_CURSOR - position;
		int len = NoAllocConv.iToA(MAGIC.cast2Ref(VidMem) + position * 2, 2, max_len, n, base);
		
		for (int i = 0; i < len; i++)
			VidMem.cells[position + i].color = (byte) color;
		
		return position + len;
	}
	
	public static int sPrint(int n, int base, int leftpadBy, char leftpadChar, int position, int color)
	{
		int max_len = MAX_CURSOR - position;
		int len = NoAllocConv.iToA(MAGIC.cast2Ref(VidMem) + position * 2, 2, max_len, n, base);
		
		if (len < leftpadBy)
		{
			int shiftCharsBy = leftpadBy - len;
			for (int i = 0; i <= len; i++)
				VidMem.cells[position + len + shiftCharsBy - i].character = VidMem.cells[position + len - i].character;
			
			for (int i = 0; i < shiftCharsBy; i++)
				VidMem.cells[position + i].character = (byte) leftpadChar;
			
			len += shiftCharsBy;
		}
		
		for (int i = 0; i < len; i++)
			VidMem.cells[position + i].color = (byte) color;
		
		return position + len;
	}
	
	@SJC.Inline
	public static int sPrintln(int n, int base, int leftpadBy, char leftpadChar, int position, int color)
	{
		int newPos = sPrint(n, base, leftpadBy, leftpadChar, position, color);
		return sNewLine(newPos);
	}
	
	@SJC.Inline
	public static int sNewLine(int cursor)
	{
		return (cursor / LINE_LENGTH + 1) * LINE_LENGTH;
	}
	
	@SJC.Inline
	public static int line(int cursor)
	{
		return cursor / LINE_LENGTH;
	}
	
	/**
	 * https://wiki.osdev.org/Text_Mode_Cursor#Moving_the_Cursor_2
	 */
	public static void setCursorCaret(int pos)
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0F);
		MAGIC.wIOs8(0x3D5, (byte) (pos & 0xFF));
		MAGIC.wIOs8(0x3D4, (byte) 0x0E);
		MAGIC.wIOs8(0x3D5, (byte) ((pos >> 8) & 0xFF));
	}
	
	@SJC.Inline
	public static void disableCursorCaret()
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0A);
		MAGIC.wIOs8(0x3D5, (byte) 0x20);
	}
	
	@SJC.Inline
	public void clearScreen()
	{
		sClearScreen();
		_cursorPos = 0;
	}
	
	@SJC.Inline
	public static void sClearScreen()
	{
		byte colClear = TM3Color.set(TM3Color.GREY, TM3Color.BLACK);
		for (int i = 0; i < LINE_COUNT; i++)
			setLine(i, (byte) ' ', colClear);
	}
	
	public static void shiftLines()
	{
		for (int line = BUFFER_START; line < BUFFER_END; line += LINE_SIZE_BYTES)
			Memory.Memcopy(line + LINE_SIZE_BYTES, line, LINE_SIZE_BYTES);
		
		byte clearColor = TM3Color.set(TM3Color.GREY, TM3Color.BLACK);
		setLine(LINE_COUNT - 1, (byte) ' ', clearColor);
	}
	
	public static void setLine(int line, byte character, byte color)
	{
		int lineStart = line * LINE_LENGTH;
		int lineEnd = lineStart + LINE_LENGTH;
		
		for (int i = lineStart; i < lineEnd; i++)
		{
			VidMem.cells[i].character = character;
			VidMem.cells[i].color = color;
		}
	}
	
	public static int lineStart(int line)
	{
		return line * LINE_LENGTH;
	}
	
	/**
	 * Sets the character byte at the current cursor position in the video memory.
	 * If the cursor position exceeds the maximum limit, it performs a scroll down
	 * operation and adjusts the cursor position accordingly.
	 */
	private void setCharacterByte(byte b)
	{
		if (b == '\n')
		{
			_cursorPos = sNewLine(_cursorPos);
			shiftIfOutOfBounds();
			return;
		}
		
		shiftIfOutOfBounds();
		VidMem.cells[_cursorPos].character = b;
		VidMem.cells[_cursorPos].color = Brush.color();
		_cursorPos += 1;
	}
	
	/**
	 * Updates the cursor caret position if it has changed.
	 * Used to avoid unnecessary I/O operations to update the cursor position.
	 */
	private void updateCursorCaretDisplay()
	{
		if (_onScreenCursorPos != _cursorPos)
		{
			setCursorCaret(_cursorPos);
			_onScreenCursorPos = _cursorPos;
		}
	}
	
	@SJC.Inline
	private static int index1D(int line, int column)
	{
		return (line * LINE_LENGTH) + column;
	}
	
	/**
	 * Checks if the cursor is out of bounds and shifts the lines if necessary.
	 * If the cursor is out of bounds, the lines are shifted and the cursor is
	 * updated accordingly.
	 */
	@SJC.Inline
	private boolean shiftIfOutOfBounds()
	{
		if (_cursorPos >= MAX_CURSOR)
		{
			shiftLines();
			_cursorPos -= LINE_LENGTH;
			updateCursorCaretDisplay();
			
			return true;
		}
		
		return false;
	}
}
