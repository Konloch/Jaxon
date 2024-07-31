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
import util.vector.VecWindow;

public class WindowManager extends Task {
    static public int InfoAvgRenderTimeMs = 0;
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

    @SuppressWarnings("unused")
    private boolean _ctrlDown = false;

    private Desktop _desktop;

    public WindowManager(GraphicsContext ctx) {
        super("_win_window_manager");
        _widgets = new VecWindow();
        this._ctx = ctx;
        _cursorModern = CursorModern.Load();
        _cursorHand = CursorHand.Load();
        _cursorCurrent = _cursorModern;
        _lastMouseX = ctx.Width() / 2;
        _lastMouseY = ctx.Height() / 2;
        _desktop = new Desktop("Desktop");
        _desktop.Draw();
    }

    public void AddWindow(Window window) {
        _widgets.add(window);
        Scheduler.AddTask(window);

        if (_selectedWindow == null && window.IsSelectable()) {
            SetSelectedTo(window);
        }
    }

    public void RemoveWindow(Window window) {
        _widgets.remove(window);
        Scheduler.RemoveTask(window);
    }

    public void StaticDisplayFor(int ms) {
        if (ms == 0) {
            return;
        }
        DrawWindows();
        _ctx.Swap();
        Timer.Sleep(ms);
        _ctx.ClearScreen();
    }

    @Override
    public void Run() {
        DistributeKeyEvents();
        DistributeMouseEvents();
        int start = Timer.Ticks();

        if (!IsUpdateTime()) {
            return;
        }

        _lastUpdate = Timer.Ticks();

        DrawWindows();
        DrawCursor();
        _ctx.Swap();

        int end = Timer.Ticks();
        int renderTime = Timer.TicksToMs(end - start);
        _drawTicksAvgSum += renderTime;
        if (_drawTicksAvgCycle >= _drawTicksAvgN) {
            InfoAvgRenderTimeMs = _drawTicksAvgSum / _drawTicksAvgN;
            _drawTicksAvgSum = 0;
            _drawTicksAvgCycle = 0;
        }
        _drawTicksAvgCycle++;
    }

    private boolean IsUpdateTime() {
        int now = Timer.Ticks();
        return Timer.TicksToMs(now - _lastUpdate) >= 1000 / 60;
    }

    private void DrawWindows() {
        if (_ctx == null) {
            return;
        }

        _ctx.ClearScreen();
        _ctx.Bitmap(0, 0, _desktop.RenderTarget, false);
        for (int i = 0; i < _widgets.size(); i++) {
            Window window = _widgets.get(i);
            if (window == null) {
                continue;
            }

            // redraw window content only if needed
            if (window.NeedsRedraw()) {
                window.Draw();
            }
            // but always blit the window
            _ctx.Bitmap(window.X, window.Y, window.RenderTarget, false);
        }
    }

    private void DrawCursor() {
        if (_ctx == null) {
            return;
        }

        if (!_ctx.Contains(_lastMouseX, _lastMouseY))
            return;
        if (!_ctx.Contains(_lastMouseX + _cursorCurrent.Width, _lastMouseY + _cursorCurrent.Height))
            return;

        _ctx.Bitmap(_lastMouseX, _lastMouseY, _cursorCurrent, true);
    }

    private void DistributeKeyEvents() {
        if (_selectedWindow == null) {
            return;
        }

        while (KeyboardController.HasNewEvent()) {
            KeyEvent keyEvent = KeyboardController.ReadEvent();
            if (keyEvent != null) {
                Logger.Trace("WIN", "Handling ".append(keyEvent.Debug()));
                if (keyEvent.IsDown) {
                    if (ConsumedInternalOnKeyPressed(keyEvent.Key)) {
                        continue;
                    }
                    _selectedWindow.OnKeyPressed(keyEvent.Key);
                } else {
                    if (ConsumedInternalOnKeyReleased(keyEvent.Key)) {
                        continue;
                    }
                    _selectedWindow.OnKeyReleased(keyEvent.Key);
                }
            }
        }
    }

    private MouseEvent _mouseEvent = new MouseEvent();

    public void DistributeMouseEvents() {
        if (MouseController.ReadEvent(_mouseEvent)) {
            ProcessMouseEvent(_mouseEvent);
        }
    }

    private void ProcessMouseEvent(MouseEvent event) {
        if (event.X_Delta != 0 || event.Y_Delta != 0) {
            SetDirtyAt(_lastMouseX, _lastMouseY);
            SetDirtyAt(_lastMouseX + _cursorCurrent.Width / 2, _lastMouseY + _cursorCurrent.Height / 2);
            SetDirtyAt(_lastMouseX + _cursorCurrent.Width, _drawTicksAvgCycle + _cursorCurrent.Height);

            _lastMouseX += event.X_Delta;
            _lastMouseY -= event.Y_Delta;

            _lastMouseX = Math.Clamp(_lastMouseX, 0, _ctx.Width());
            _lastMouseY = Math.Clamp(_lastMouseY, 0, _ctx.Height());

            if (_is_dragging && _selectedWindow != null && _selectedWindow.IsDraggable()) {
                _selectedWindow.MoveBy(event.X_Delta, -event.Y_Delta);
            }
        }

        if (event.LeftButtonPressed()) {
            if (_leftButtonAlreadyDown) {
                if (!_is_dragging) {
                    StartDrag();
                }

            } else {
                Logger.Trace("WIN", "Mouse Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
                SetSelectedAt(_lastMouseX, _lastMouseY);
                boolean consumedBySelected = false;
                if (_selectedWindow != null) {
                    if (_selectedWindow.Contains(_lastMouseX, _lastMouseY)) {
                        consumedBySelected = _selectedWindow.AbsoluteLeftClickAt(_lastMouseX, _lastMouseY);
                    }
                }
                if (!consumedBySelected) {
                    _desktop.AbsoluteLeftClickAt(_lastMouseX, _lastMouseY);
                }
                _leftButtonAlreadyDown = true;
            }
        } else {
            StopDrag();
            _leftButtonAlreadyDown = false;
        }

        if (event.RightButtonPressed()) {
            Logger.Trace("WIN", "Mouse Right Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
        }

        if (event.MiddleButtonPressed()) {
            Logger.Trace("WIN", "Mouse Middle Click at ".append(_lastMouseX).append(", ").append(_lastMouseY));
        }
    }

    private void StartDrag() {
        _cursorCurrent = _cursorHand;
        _is_dragging = true;
    }

    private void StopDrag() {
        _cursorCurrent = _cursorModern;
        _is_dragging = false;
    }

    private void SetSelectedAt(int x, int y) {
        for (int i = _widgets.size() - 1; i >= 0; i--) {
            Window window = _widgets.get(i);
            if (window == null || !window.IsSelectable()) {
                continue;
            }
            if (window.Contains(x, y)) {
                SetSelectedTo(window);
                return;
            }
        }
    }

    private void SetSelectedTo(Window window) {
        Logger.Trace("WIN", "Selected ".append(window.Name));
        if (_selectedWindow != null) {
            _selectedWindow.SetSelected(false);
        }
        _selectedWindow = window;
        _selectedWindow.SetSelected(true);

        // Move to front by z order
        _widgets.remove(window);
        _widgets.add(window);
    }

    private void SetDirtyAt(int x, int y) {
        for (int i = _widgets.size() - 1; i >= 0; i--) {
            Window window = _widgets.get(i);
            if (window == null) {
                continue;
            }
            if (window.Contains(x, y)) {
                window.SetDirty();
            }
        }
    }

    private boolean ConsumedInternalOnKeyPressed(char keyCode) {
        switch (keyCode) {
            case Key.F9:
                new EndlessTask().Register();
                return true;
            case Key.LCTRL:
                _ctrlDown = true;
                return true;
            default:
                return false;
        }
    }

    private boolean ConsumedInternalOnKeyReleased(char keyCode) {
        switch (keyCode) {
            case Key.LCTRL:
                _ctrlDown = false;
                return true;
            default:
                return false;
        }
    }
}
