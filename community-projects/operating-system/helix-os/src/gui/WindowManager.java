package gui;

import gui.displays.windows.Desktop;
import gui.images.CursorHand;
import gui.images.CursorModern;
import kernel.display.Bitmap;
import kernel.display.GraphicsContext;
import kernel.hardware.Timer;
import kernel.hardware.keyboard.Key;
import kernel.hardware.keyboard.KeyEvent;
import kernel.hardware.keyboard.KeyboardController;
import kernel.hardware.mouse.MouseController;
import kernel.hardware.mouse.MouseEvent;
import kernel.schedule.EndlessTask;
import kernel.schedule.Scheduler;
import kernel.schedule.Task;
import kernel.trace.logging.Logger;
import java.util.vector.VecWindow;

public class WindowManager extends Task
{
	static public int infoAvgRenderTimeMs = 0;
	private int _drawTicksAvgN = 50;
	private int _drawTicksAvgCycle = 0;
	private int _drawTicksAvgSum = 0;
	
	private GraphicsContext _ctx;
	private VecWindow _widgets;
	private Window _selectedWindow;
	private int _lastUpdate = 0;
	
	private Bitmap _cursorHand;
	private Bitmap _cursorModern;
	private Bitmap _cursorCurrent;
	
	private int _lastMouseX;
	private int _lastMouseY;
	private boolean _leftButtonAlreadyDown = false;
	private boolean _is_dragging = false;
	
	private boolean _ctrlDown = false;
	
	private Desktop _desktop;
	
	public WindowManager(GraphicsContext ctx)
	{
		super("_win_window_manager");
		_widgets = new VecWindow();
		this._ctx = ctx;
		_cursorModern = CursorModern.load();
		_cursorHand = CursorHand.load();
		_cursorCurrent = _cursorModern;
		_lastMouseX = ctx.width() / 2;
		_lastMouseY = ctx.height() / 2;
		_desktop = new Desktop("Desktop");
		_desktop.draw();
	}
	
	public void addWindow(Window window)
	{
		_widgets.add(window);
		Scheduler.addTask(window);
		
		if (_selectedWindow == null && window.isSelectable())
			setSelectedTo(window);
	}
	
	public void removeWindow(Window window)
	{
		_widgets.remove(window);
		Scheduler.removeTask(window);
	}
	
	public void staticDisplayFor(int ms)
	{
		if (ms == 0)
			return;
		
		drawWindows();
		_ctx.swap();
		Timer.sleep(ms);
		_ctx.clearScreen();
	}
	
	@Override
	public void run()
	{
		distributeKeyEvents();
		distributeMouseEvents();
		int start = Timer.ticks();
		
		if (!isUpdateTime())
			return;
		
		_lastUpdate = Timer.ticks();
		
		drawWindows();
		drawCursor();
		_ctx.swap();
		
		int end = Timer.ticks();
		int renderTime = Timer.ticksToMs(end - start);
		_drawTicksAvgSum += renderTime;
		if (_drawTicksAvgCycle >= _drawTicksAvgN)
		{
			infoAvgRenderTimeMs = _drawTicksAvgSum / _drawTicksAvgN;
			_drawTicksAvgSum = 0;
			_drawTicksAvgCycle = 0;
		}
		_drawTicksAvgCycle++;
	}
	
	private boolean isUpdateTime()
	{
		int now = Timer.ticks();
		return Timer.ticksToMs(now - _lastUpdate) >= 1000 / 60;
	}
	
	private void drawWindows()
	{
		if (_ctx == null)
			return;
		
		_ctx.clearScreen();
		_ctx.bitmap(0, 0, _desktop.renderTarget, false);
		for (int i = 0; i < _widgets.size(); i++)
		{
			Window window = _widgets.get(i);
			if (window == null)
				continue;
			
			// redraw window content only if needed
			if (window.needsRedraw())
				window.draw();
			
			// but always blit the window
			_ctx.bitmap(window.x, window.y, window.renderTarget, false);
		}
	}
	
	private void drawCursor()
	{
		if (_ctx == null)
			return;
		
		if (!_ctx.contains(_lastMouseX, _lastMouseY))
			return;
		
		if (!_ctx.contains(_lastMouseX + _cursorCurrent.width, _lastMouseY + _cursorCurrent.height))
			return;
		
		_ctx.bitmap(_lastMouseX, _lastMouseY, _cursorCurrent, true);
	}
	
	private void distributeKeyEvents()
	{
		if (_selectedWindow == null)
			return;
		
		while (KeyboardController.hasNewEvent())
		{
			KeyEvent keyEvent = KeyboardController.readEvent();
			if (keyEvent != null)
			{
				Logger.trace("WIN", "Handling ".append(keyEvent.debug()));
				if (keyEvent.isDown)
				{
					if (consumedInternalOnKeyPressed(keyEvent.key))
						continue;
					
					_selectedWindow.onKeyPressed(keyEvent.key);
				}
				else
				{
					if (consumedInternalOnKeyReleased(keyEvent.key))
						continue;
					
					_selectedWindow.onKeyReleased(keyEvent.key);
				}
			}
		}
	}
	
	private MouseEvent _mouseEvent = new MouseEvent();
	
	public void distributeMouseEvents()
	{
		if (MouseController.readEvent(_mouseEvent))
			processMouseEvent(_mouseEvent);
	}
	
	private void processMouseEvent(MouseEvent event)
	{
		if (event.xDelta != 0 || event.yDelta != 0)
		{
			setDirtyAt(_lastMouseX, _lastMouseY);
			setDirtyAt(_lastMouseX + _cursorCurrent.width / 2, _lastMouseY + _cursorCurrent.height / 2);
			setDirtyAt(_lastMouseX + _cursorCurrent.width, _drawTicksAvgCycle + _cursorCurrent.height);
			
			_lastMouseX += event.xDelta;
			_lastMouseY -= event.yDelta;
			
			_lastMouseX = Math.clamp(_lastMouseX, 0, _ctx.width());
			_lastMouseY = Math.clamp(_lastMouseY, 0, _ctx.height());
			
			if (_is_dragging && _selectedWindow != null && _selectedWindow.isDraggable())
				_selectedWindow.moveBy(event.xDelta, -event.yDelta);
		}
		
		if (event.leftButtonPressed())
		{
			if (_leftButtonAlreadyDown)
			{
				if (!_is_dragging)
					startDrag();
				
			}
			else
			{
				Logger.trace("WIN", "Mouse Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
				setSelectedAt(_lastMouseX, _lastMouseY);
				boolean consumedBySelected = false;
				if (_selectedWindow != null)
				{
					if (_selectedWindow.contains(_lastMouseX, _lastMouseY))
						consumedBySelected = _selectedWindow.absoluteLeftClickAt(_lastMouseX, _lastMouseY);
				}
				
				if (!consumedBySelected)
					_desktop.absoluteLeftClickAt(_lastMouseX, _lastMouseY);
				_leftButtonAlreadyDown = true;
			}
		}
		else
		{
			stopDrag();
			_leftButtonAlreadyDown = false;
		}
		
		if (event.rightButtonPressed())
			Logger.trace("WIN", "Mouse Right Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
		
		if (event.middleButtonPressed())
			Logger.trace("WIN", "Mouse Middle Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
	}
	
	private void startDrag()
	{
		_cursorCurrent = _cursorHand;
		_is_dragging = true;
	}
	
	private void stopDrag()
	{
		_cursorCurrent = _cursorModern;
		_is_dragging = false;
	}
	
	private void setSelectedAt(int x, int y)
	{
		for (int i = _widgets.size() - 1; i >= 0; i--)
		{
			Window window = _widgets.get(i);
			
			if (window == null || !window.isSelectable())
				continue;
			
			if (window.contains(x, y))
			{
				setSelectedTo(window);
				return;
			}
		}
	}
	
	private void setSelectedTo(Window window)
	{
		Logger.trace("WIN", "Selected ".append(window.name));
		
		if (_selectedWindow != null)
			_selectedWindow.setSelected(false);
		
		_selectedWindow = window;
		_selectedWindow.setSelected(true);
		
		// Move to front by z order
		_widgets.remove(window);
		_widgets.add(window);
	}
	
	private void setDirtyAt(int x, int y)
	{
		for (int i = _widgets.size() - 1; i >= 0; i--)
		{
			Window window = _widgets.get(i);
			if (window == null)
				continue;
			
			if (window.contains(x, y))
				window.setDirty();
		}
	}
	
	private boolean consumedInternalOnKeyPressed(char keyCode)
	{
		switch (keyCode)
		{
			case Key.F9:
				new EndlessTask().register();
				return true;
				
			case Key.LCTRL:
				_ctrlDown = true;
				return true;
				
			default:
				return false;
		}
	}
	
	private boolean consumedInternalOnKeyReleased(char keyCode)
	{
		switch (keyCode)
		{
			case Key.LCTRL:
				_ctrlDown = false;
				return true;
				
			default:
				return false;
		}
	}
}
