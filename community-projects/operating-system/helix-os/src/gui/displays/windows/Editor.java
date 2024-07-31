package gui.displays.windows;

import formats.fonts.AFont;
import gui.Window;
import gui.components.TextField;
import kernel.Kernel;
import kernel.hardware.keyboard.Key;
import kernel.trace.logging.Logger;

public class Editor extends Window
{
	private TextField _textField;
	
	public Editor(String title, int x, int y, int width, int height, int border, int charSpacing, int lineSpacing, AFont font)
	{
		super(title, x, y, width, height, true);
		
		int bg = Kernel.Display.rgb(20, 20, 20);
		int fg = Kernel.Display.rgb(255, 255, 255);
		_textField = new TextField(0, 0, contentWidth, contentHeight, border, charSpacing, lineSpacing, fg, bg, true, font);
	}
	
	public void drawContent()
	{
		if (_textField.needsRedraw())
		{
			_textField.draw();
		}
		renderTarget.blit(contentRelativeX, contentRelativeY, _textField.renderTarget, false);
		clearDirty();
	}
	
	@Override
	public boolean needsRedraw()
	{
		return _textField.needsRedraw() || super.needsRedraw();
	}
	
	@Override
	public void onKeyPressed(char key)
	{
		switch (key)
		{
			case '\n':
				_textField.NewLine();
				break;
			case '\b':
				_textField.Backspace();
				break;
			case Key.ARROW_UP:
				MoveCursorUp();
				break;
			case Key.ARROW_DOWN:
				MoveCursorDown();
				break;
			case Key.ARROW_LEFT:
				MoveCursorLeft();
				break;
			case Key.ARROW_RIGHT:
				MoveCursorRight();
				break;
			default:
				if (Key.ascii(key) != 0)
				{
					_textField.Write((byte) key);
				}
				break;
		}
	}
	
	@Override
	public boolean relativeLeftClickAt(int relX, int relY)
	{
		if (super.relativeLeftClickAt(relX, relY))
		{
			return true;
		}
		
		int cellW = relX / (_textField.Font.width() + _textField.SpacingW);
		int cx = Math.clamp(cellW, 0, _textField.LineLength - 1);
		
		int cellH = relY / (_textField.Font.height() + _textField.SpacingH);
		int cy = Math.clamp(cellH, 0, _textField.LineCount - 1);
		
		_textField.SetCursor(cx, cy);
		Logger.info("Cursor", "set to".append(cx).append(" ").append(cy));
		return true;
	}
	
	private void MoveCursorUp()
	{
		int x = _textField.GetCursorX();
		int y = _textField.GetCursorY();
		if (y > 0)
		{
			_textField.SetCursor(x, y - 1);
		}
	}
	
	private void MoveCursorDown()
	{
		int x = _textField.GetCursorX();
		int y = _textField.GetCursorY();
		if (y < _textField.LineCount - 1)
		{
			_textField.SetCursor(x, y + 1);
		}
	}
	
	private void MoveCursorLeft()
	{
		int x = _textField.GetCursorX();
		int y = _textField.GetCursorY();
		if (x > 0)
		{
			_textField.SetCursor(x - 1, y);
		}
	}
	
	private void MoveCursorRight()
	{
		int x = _textField.GetCursorX();
		int y = _textField.GetCursorY();
		if (x < _textField.LineLength - 1)
		{
			_textField.SetCursor(x + 1, y);
		}
	}
	
	@Override
	public void moveBy(int dragDiffX, int dragDiffY)
	{
		super.moveBy(dragDiffX, dragDiffY);
		// _textField.DragBy(dragDiffX, dragDiffY);
	}
	
	@Override
	public void update()
	{
	}
}
