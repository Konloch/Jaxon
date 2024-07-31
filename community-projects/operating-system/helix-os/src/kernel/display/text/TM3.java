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
	public void SetCursor(int line, int column)
	{
		_cursorPos = Index1d(line, column);
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void SetCursorPos(int idx)
	{
		_cursorPos = idx;
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public int CursorPos()
	{
		return _cursorPos;
	}
	
	@SJC.Inline
	public int CurrentLine()
	{
		return _cursorPos / LINE_LENGTH;
	}
	
	@SJC.Inline
	public void Print(byte b)
	{
		SetCharacterByte(b);
		UpdateCursorCaretDisplay();
	}
	
	public void Print(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			SetCharacterByte((byte) str.get(i));
		}
		UpdateCursorCaretDisplay();
	}
	
	public void Print(char[] chars)
	{
		for (char c : chars)
		{
			SetCharacterByte((byte) c);
		}
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void Print(char c)
	{
		SetCharacterByte((byte) c);
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void Print(boolean b)
	{
		Print(b ? "true" : "false");
	}
	
	public void Print(int n, int base)
	{
		int max_len = MAX_CURSOR - _cursorPos;
		int old_pos = _cursorPos;
		_cursorPos += NoAllocConv.iToA(MAGIC.cast2Ref(VidMem) + _cursorPos * 2, 2, max_len, n, base);
		
		// Set color for the printed number
		for (int i = old_pos; i < _cursorPos; i++)
		{
			VidMem.Cells[i].Color = Brush.Color();
		}
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void Print(int n)
	{
		Print(n, 10);
	}
	
	@SJC.Inline
	public void Println()
	{
		_cursorPos = sNewLine(_cursorPos);
		ShiftIfOutOfBounds();
		UpdateCursorCaretDisplay();
	}
	
	@SJC.Inline
	public void Println(byte b)
	{
		Print(b);
		Println();
	}
	
	public void Println(char[] chars)
	{
		Print(chars);
		Println();
	}
	
	@SJC.Inline
	public void Println(boolean b)
	{
		Print(b);
		Println();
	}
	
	@SJC.Inline
	public void Println(char c)
	{
		Print(c);
		Println();
	}
	
	@SJC.Inline
	public void Println(String str)
	{
		Print(str);
		Println();
	}
	
	@SJC.Inline
	public void Println(int n, int base)
	{
		Print(n, base);
		Println();
	}
	
	@SJC.Inline
	public void Println(int n)
	{
		Print(n);
		Println();
	}
	
	@SJC.Inline
	public static int sPrint(char c, int position, int color)
	{
		VidMem.Cells[position].Character = (byte) c;
		VidMem.Cells[position].Color = (byte) color;
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
		{
			sPrint((char) s.get(i), position + i, color);
		}
		return position + s.length();
	}
	
	public static int sPrint(String s, int position, int color, int maxLen)
	{
		int len = Math.min(s.length(), maxLen - 3);
		for (int i = 0; i < len; i++)
		{
			sPrint((char) s.get(i), position + i, color);
		}
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
		{
			VidMem.Cells[position + i].Color = (byte) color;
		}
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
			{
				VidMem.Cells[position + len + shiftCharsBy - i].Character = VidMem.Cells[position + len - i].Character;
			}
			
			for (int i = 0; i < shiftCharsBy; i++)
			{
				VidMem.Cells[position + i].Character = (byte) leftpadChar;
			}
			
			len += shiftCharsBy;
		}
		
		for (int i = 0; i < len; i++)
		{
			VidMem.Cells[position + i].Color = (byte) color;
		}
		
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
	public static int Line(int cursor)
	{
		return cursor / LINE_LENGTH;
	}
	
	/**
	 * https://wiki.osdev.org/Text_Mode_Cursor#Moving_the_Cursor_2
	 */
	public static void SetCursorCaret(int pos)
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0F);
		MAGIC.wIOs8(0x3D5, (byte) (pos & 0xFF));
		MAGIC.wIOs8(0x3D4, (byte) 0x0E);
		MAGIC.wIOs8(0x3D5, (byte) ((pos >> 8) & 0xFF));
	}
	
	@SJC.Inline
	public static void DisableCursorCaret()
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0A);
		MAGIC.wIOs8(0x3D5, (byte) 0x20);
	}
	
	@SJC.Inline
	public void ClearScreen()
	{
		sClearScreen();
		_cursorPos = 0;
	}
	
	@SJC.Inline
	public static void sClearScreen()
	{
		byte colClear = TM3Color.Set(TM3Color.GREY, TM3Color.BLACK);
		for (int i = 0; i < LINE_COUNT; i++)
		{
			SetLine(i, (byte) ' ', colClear);
		}
	}
	
	public static void ShiftLines()
	{
		for (int line = BUFFER_START; line < BUFFER_END; line += LINE_SIZE_BYTES)
		{
			Memory.Memcopy(line + LINE_SIZE_BYTES, line, LINE_SIZE_BYTES);
		}
		
		byte clearColor = TM3Color.Set(TM3Color.GREY, TM3Color.BLACK);
		SetLine(LINE_COUNT - 1, (byte) ' ', clearColor);
	}
	
	public static void SetLine(int line, byte character, byte color)
	{
		int lineStart = line * LINE_LENGTH;
		int lineEnd = lineStart + LINE_LENGTH;
		for (int i = lineStart; i < lineEnd; i++)
		{
			VidMem.Cells[i].Character = character;
			VidMem.Cells[i].Color = color;
		}
	}
	
	public static int LineStart(int line)
	{
		return line * LINE_LENGTH;
	}
	
	/**
	 * Sets the character byte at the current cursor position in the video memory.
	 * If the cursor position exceeds the maximum limit, it performs a scroll down
	 * operation and adjusts the cursor position accordingly.
	 */
	private void SetCharacterByte(byte b)
	{
		if (b == '\n')
		{
			_cursorPos = sNewLine(_cursorPos);
			ShiftIfOutOfBounds();
			return;
		}
		ShiftIfOutOfBounds();
		VidMem.Cells[_cursorPos].Character = b;
		VidMem.Cells[_cursorPos].Color = Brush.Color();
		_cursorPos += 1;
	}
	
	/**
	 * Updates the cursor caret position if it has changed.
	 * Used to avoid unnecessary I/O operations to update the cursor position.
	 */
	private void UpdateCursorCaretDisplay()
	{
		if (_onScreenCursorPos != _cursorPos)
		{
			SetCursorCaret(_cursorPos);
			_onScreenCursorPos = _cursorPos;
		}
	}
	
	@SJC.Inline
	private static int Index1d(int line, int column)
	{
		return (line * LINE_LENGTH) + column;
	}
	
	/**
	 * Checks if the cursor is out of bounds and shifts the lines if necessary.
	 * If the cursor is out of bounds, the lines are shifted and the cursor is
	 * updated accordingly.
	 */
	@SJC.Inline
	private boolean ShiftIfOutOfBounds()
	{
		if (_cursorPos >= MAX_CURSOR)
		{
			ShiftLines();
			_cursorPos -= LINE_LENGTH;
			UpdateCursorCaretDisplay();
			
			return true;
		}
		return false;
	}
}
