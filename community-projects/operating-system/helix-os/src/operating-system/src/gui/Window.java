package gui;

import formats.fonts.AFont;
import formats.fonts.Font9x16;
import gui.components.TextField;
import gui.components.button.BitmapButton;
import gui.components.button.Button;
import gui.components.button.ButtonClickedEventArgs;
import gui.components.button.IButtonListener;
import gui.images.CloseIcon;
import kernel.Kernel;
import kernel.display.Bitmap;
import kernel.schedule.Task;
import kernel.trace.logging.Logger;
import java.util.vector.Vec;

public abstract class Window extends Task implements IButtonListener
{
	public final int frameSize;
	public final int titleBarSize;
	public int x;
	public int y;
	public final int contentRelativeX;
	public final int contentRelativeY;
	public int contentWidth;
	public int contentHeight;
	public TextField title;
	
	private Button _btnClose;
	
	public int width;
	public int height;
	public boolean isSelected;
	protected boolean _needsRedraw;
	
	public final int COL_BORDER;
	public final int COL_TITLEBAR;
	public final int COL_TITLEBAR_SELECTED;
	public final int COL_TITLE;
	
	public Bitmap renderTarget;
	
	protected Vec _widgets;
	protected boolean _isSelectable = true;
	protected boolean _isDraggable = true;
	private boolean _withFrame;
	
	private static final String BTN_WINDOW_CLOSE = "Btn_Window_Close";
	
	public Window(String title, int x, int y, int width, int height, boolean withFrame)
	{
		super(title);
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		_needsRedraw = true;
		_widgets = new Vec();
		_withFrame = withFrame;
		
		renderTarget = new Bitmap(width, height, false);
		
		COL_BORDER = Kernel.Display.Rgb(180, 180, 180);
		COL_TITLEBAR = Kernel.Display.Rgb(80, 80, 80);
		COL_TITLEBAR_SELECTED = Kernel.Display.Rgb(170, 190, 250);
		COL_TITLE = Kernel.Display.Rgb(255, 255, 255);
		frameSize = 4;
		titleBarSize = 20;
		
		if (_withFrame)
		{
			_btnClose = new BitmapButton(BTN_WINDOW_CLOSE, width - titleBarSize, 2, titleBarSize - 4, CloseIcon.Load(), this);
			addWidget(_btnClose);
		}
		
		contentRelativeX = frameSize;
		contentRelativeY = frameSize + titleBarSize;
		contentWidth = this.width - frameSize * 2;
		contentHeight = this.height - frameSize * 2 - titleBarSize;
		AFont font = Font9x16.Instance;
		int shiftRight = 5;
		this.title = new TextField(2, (titleBarSize - font.height()) / 2, this.width - shiftRight, font.height(), 0, 0, 0, COL_TITLE, COL_TITLEBAR, false, font);
		this.title.write(title);
	}
	
	@Override
	public void run()
	{
		update();
	}
	
	public boolean onButtonClicked(ButtonClickedEventArgs button)
	{
		Logger.info("Window", "Button event: ".append(button.debug()));
		if (button.buttonName == BTN_WINDOW_CLOSE)
		{
			Kernel.WindowManager.RemoveWindow(this);
			return true;
		}
		
		return false;
	}
	
	public abstract void update();
	
	public void draw()
	{
		if (_withFrame)
		{
			drawFrame();
			drawTitleBar();
		}
		
		drawContent();
		drawWidgets();
		clearDirty();
	}
	
	public abstract void drawContent();
	
	public void drawFrame()
	{
		if (isSelected)
			renderTarget.Rectangle(0, 0, width, height, COL_TITLEBAR_SELECTED);
		else
			renderTarget.Rectangle(0, 0, width, height, COL_BORDER);
	}
	
	public void drawTitleBar()
	{
		renderTarget.Rectangle(0, 0, width - 1, titleBarSize, COL_TITLEBAR);
		
		title.draw();
		renderTarget.Blit(title.x, title.y, title.renderTarget, false);
		renderTarget.Blit(_btnClose.x, _btnClose.y, _btnClose.renderTarget, false);
	}
	
	public boolean containsTitlebar(int x, int y)
	{
		return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + titleBarSize;
	}
	
	public boolean isSelectable()
	{
		return _isSelectable;
	}
	
	public boolean isDraggable()
	{
		return _isDraggable;
	}
	
	public void moveBy(int dragDiffX, int dragDiffY)
	{
		x += dragDiffX;
		y += dragDiffY;
		setDirty();
	}
	
	public boolean contains(int x, int y)
	{
		return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
	}
	
	public boolean needsRedraw()
	{
		return _needsRedraw;
	}
	
	public void setDirty()
	{
		_needsRedraw = true;
	}
	
	public void clearDirty()
	{
		_needsRedraw = false;
	}
	
	public void setSelected(boolean selected)
	{
		isSelected = selected;
	}
	
	public boolean isSelected()
	{
		return isSelected;
	}
	
	// Interactions
	
	public void onKeyPressed(char keyCode)
	{
	}
	
	public void onKeyReleased(char keyCode)
	{
	}
	
	public boolean absoluteLeftClickAt(int absX, int absY)
	{
		int relX = absX - x;
		int relY = absY - y;
		return relativeLeftClickAt(relX, relY);
	}
	
	public boolean relativeLeftClickAt(int relX, int relY)
	{
		return handleWidgetClicks(relX, relY);
	}
	
	protected boolean handleWidgetClicks(int relX, int relY)
	{
		for (int i = 0; i < _widgets.size(); i++)
		{
			Widget widget = (Widget) _widgets.get(i);
			if (widget.contains(relX, relY))
			{
				Logger.info("WM", "Clicked at ".append(widget.name));
				if (widget instanceof Button)
				{
					Logger.info("WM", "Button clicked");
					Button button = (Button) widget;
					button.click();
				}
				return true;
			}
		}
		return false;
	}
	
	protected void addWidget(Widget widget)
	{
		_widgets.add(widget);
	}
	
	protected void removeWidget(Widget widget)
	{
		_widgets.remove(widget);
	}
	
	protected void drawWidgets()
	{
		for (int i = 0; i < _widgets.size(); i++)
		{
			Widget widget = (Widget) _widgets.get(i);
			widget.draw();
			renderTarget.Blit(widget.x, widget.y, widget.renderTarget, false);
		}
	}
}
