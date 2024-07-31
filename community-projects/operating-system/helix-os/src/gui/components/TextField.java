package gui.components;

import formats.fonts.AFont;
import gui.Widget;
import kernel.hardware.keyboard.Key;
import java.lang.StringBuilder;

public class TextField extends Widget
{
	public int SpacingBorder;
	public int SpacingW;
	public int SpacingH;
	public int LineLength;
	public int LineCount;
	public AFont Font;
	
	protected int _cursorX;
	protected int _cursorY;
	
	protected int _bg;
	protected int _fg;
	
	protected byte[][] _characters;
	protected int[][] _characterColors;
	
	protected boolean _enableCursor;
	
	public TextField(int x, int y, int width, int height, int borderSpacing, int charSpacing, int lineSpacing, int fg, int bg, boolean enableCursor, AFont font)
	{
		super("component_textfield", x, y, width, height);
		
		_cursorX = 0;
		_cursorY = 0;
		_fg = fg;
		_bg = bg;
		Font = font;
		SpacingBorder = borderSpacing;
		SpacingW = charSpacing + font.spacingW();
		SpacingH = lineSpacing + font.spacingH();
		LineLength = (this.width - borderSpacing * 2) / (font.width() + SpacingW);
		LineCount = (this.height - borderSpacing * 2) / (font.height() + SpacingH);
		_characters = new byte[LineCount][LineLength];
		_characterColors = new int[LineCount][LineLength];
		_enableCursor = enableCursor;
	}
	
	public void SetCursor(int x, int y)
	{
		this._cursorX = x;
		this._cursorY = y;
		setDirty();
	}
	
	public int GetCursorX()
	{
		return _cursorX;
	}
	
	public int GetCursorY()
	{
		return _cursorY;
	}
	
	public void SetBrushColor(int color)
	{
		this._fg = color;
	}
	
	public void Write(byte c)
	{
		if (_cursorX >= LineLength)
		{
			NewLine();
		}
		if (_cursorY >= LineCount)
		{
			NewLine();
		}
		
		_characters[_cursorY][_cursorX] = c;
		_characterColors[_cursorY][_cursorX] = _fg;
		_cursorX++;
		setDirty();
	}
	
	public void NewLine()
	{
		_cursorX = 0;
		_cursorY++;
		if (_cursorY >= LineCount)
		{
			Scroll();
			_cursorY--;
		}
		setDirty();
	}
	
	public void Backspace()
	{
		if (_cursorX > 0)
		{
			_cursorX--;
			_characters[_cursorY][_cursorX] = (byte) 0;
		}
		else
		{
			if (_cursorY > 0)
			{
				_cursorY--;
				int lastCharInLine = 0;
				while (lastCharInLine < LineLength && _characters[_cursorY][lastCharInLine] != 0)
				{
					lastCharInLine++;
				}
				_cursorX = Math.clamp(lastCharInLine, 0, LineLength - 1);
				_characters[_cursorY][_cursorX] = (byte) 0;
			}
		}
		setDirty();
	}
	
	public void Write(String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			byte c = (byte) s.get(i);
			if (c == '\n')
			{
				NewLine();
			}
			else
			{
				Write(c);
			}
		}
		setDirty();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < LineCount; i++)
		{
			for (int j = 0; j < LineLength; j++)
			{
				sb.append((char) _characters[i][j]);
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	public void Scroll()
	{
		for (int i = 0; i < LineCount - 1; i++)
		{
			for (int j = 0; j < LineLength; j++)
			{
				_characters[i][j] = _characters[i + 1][j];
				_characterColors[i][j] = _characterColors[i + 1][j];
			}
		}
		for (int j = 0; j < LineLength; j++)
		{
			_characters[LineCount - 1][j] = (byte) ' ';
			_characterColors[LineCount - 1][j] = _bg;
		}
		setDirty();
	}
	
	public void ClearText()
	{
		for (int i = 0; i < LineCount; i++)
		{
			for (int j = 0; j < LineLength; j++)
			{
				_characters[i][j] = (byte) '\0';
			}
		}
		SetCursor(0, 0);
		setDirty();
	}
	
	public void ClearLine(int line)
	{
		for (int j = 0; j < LineLength; j++)
		{
			_characters[line][j] = (byte) 0;
		}
		setDirty();
	}
	
	public void DrawCursor()
	{
		int xFactor = Font.width() + SpacingW;
		int yFactor = Font.height() + SpacingH;
		int xOffset = SpacingBorder;
		int yOffset = SpacingBorder;
		
		int x = xOffset + _cursorX * xFactor;
		int y = yOffset + _cursorY * yFactor;
		
		renderTarget.rectangle(x, y, 2, Font.height(), _fg);
	}
	
	@Override
	public void draw()
	{
		renderTarget.rectangle(0, 0, width, height, _bg);
		
		int xFactor = Font.width() + SpacingW;
		int yFactor = Font.height() + SpacingH;
		int xOffset = SpacingBorder;
		int yOffset = SpacingBorder;
		
		for (int i = 0; i < LineCount; i++)
		{
			for (int j = 0; j < LineLength; j++)
			{
				char character = (char) _characters[i][j];
				int characterColor = _characterColors[i][j];
				
				// Skip rendering if the character is not visible
				if (characterColor == _bg || Key.Ascii(character) == 0)
				{
					continue;
				}
				int x = xOffset + j * xFactor;
				int y = yOffset + i * yFactor;
				
				Font.renderToBitmap(renderTarget, x, y, character, characterColor);
			}
		}
		if (_enableCursor)
		{
			DrawCursor();
		}
		clearDirty();
	}
}
