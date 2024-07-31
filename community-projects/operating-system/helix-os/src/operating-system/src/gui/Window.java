package gui;

import formats.fonts.AFont;
import formats.fonts.Font9x16;
import gui.components.TextField;
import gui.components.button.Button;
import gui.components.button.ButtonClickedEventArgs;
import gui.components.button.IButtonListener;
import gui.components.button.BitmapButton;
import gui.images.CloseIcon;
import kernel.Kernel;
import kernel.display.Bitmap;
import kernel.schedule.Task;
import kernel.trace.logging.Logger;
import util.vector.Vec;

public abstract class Window extends Task implements IButtonListener {
    public final int FrameSize;
    public final int TitleBarSize;
    public int X;
    public int Y;
    public final int ContentRelativeX;
    public final int ContentRelativeY;
    public int ContentWidth;
    public int ContentHeight;
    public TextField Title;

    private Button _btnClose;

    public int Width;
    public int Height;
    public boolean IsSelected;
    protected boolean _needsRedraw;

    public final int COL_BORDER;
    public final int COL_TITLEBAR;
    public final int COL_TITLEBAR_SELECTED;
    public final int COL_TITLE;

    public Bitmap RenderTarget;

    protected Vec _widgets;
    protected boolean _isSelectable = true;
    protected boolean _isDraggable = true;
    private boolean _withFrame;

    private static final String BTN_WINDOW_CLOSE = "Btn_Window_Close";

    public Window(String title, int x, int y, int width, int height, boolean withFrame) {
        super(title);
        X = x;
        Y = y;
        Width = width;
        Height = height;
        _needsRedraw = true;
        _widgets = new Vec();
        _withFrame = withFrame;

        RenderTarget = new Bitmap(width, height, false);

        COL_BORDER = Kernel.Display.Rgb(180, 180, 180);
        COL_TITLEBAR = Kernel.Display.Rgb(80, 80, 80);
        COL_TITLEBAR_SELECTED = Kernel.Display.Rgb(170, 190, 250);
        COL_TITLE = Kernel.Display.Rgb(255, 255, 255);
        FrameSize = 4;
        TitleBarSize = 20;

        if (_withFrame) {
            _btnClose = new BitmapButton(BTN_WINDOW_CLOSE, width - TitleBarSize, 2, TitleBarSize - 4, CloseIcon.Load(),
                    this);
            AddWidget(_btnClose);
        }

        ContentRelativeX = FrameSize;
        ContentRelativeY = FrameSize + TitleBarSize;
        ContentWidth = Width - FrameSize * 2;
        ContentHeight = Height - FrameSize * 2 - TitleBarSize;
        AFont font = Font9x16.Instance;
        int shiftRight = 5;
        Title = new TextField(
                2,
                (TitleBarSize - font.Height()) / 2,
                Width - shiftRight,
                font.Height(),
                0,
                0,
                0,
                COL_TITLE,
                COL_TITLEBAR,
                false,
                font);
        Title.Write(title);
    }

    @Override
    public void Run() {
        Update();
    }

    public boolean OnButtonClicked(ButtonClickedEventArgs button) {
        Logger.Info("Window", "Button event: ".append(button.Debug()));
        if (button.ButtonName == BTN_WINDOW_CLOSE) {
            Kernel.WindowManager.RemoveWindow(this);
            return true;
        }
        return false;
    }

    public abstract void Update();

    public void Draw() {
        if (_withFrame) {
            DrawFrame();
            DrawTitleBar();
        }
        DrawContent();
        DrawWidgets();
        ClearDirty();
    }

    public abstract void DrawContent();

    public void DrawFrame() {
        if (IsSelected) {
            RenderTarget.Rectangle(0, 0, Width, Height, COL_TITLEBAR_SELECTED);
        } else {
            RenderTarget.Rectangle(0, 0, Width, Height, COL_BORDER);
        }
    }

    public void DrawTitleBar() {
        RenderTarget.Rectangle(0, 0, Width - 1, TitleBarSize, COL_TITLEBAR);

        Title.Draw();
        RenderTarget.Blit(Title.X, Title.Y, Title.RenderTarget, false);
        RenderTarget.Blit(_btnClose.X, _btnClose.Y, _btnClose.RenderTarget, false);
    }

    public boolean ContainsTitlebar(int x, int y) {
        return x >= X && x <= X + Width && y >= Y && y <= Y + TitleBarSize;
    }

    public boolean IsSelectable() {
        return _isSelectable;
    }

    public boolean IsDraggable() {
        return _isDraggable;
    }

    public void MoveBy(int dragDiffX, int dragDiffY) {
        X += dragDiffX;
        Y += dragDiffY;
        SetDirty();
    }

    public boolean Contains(int x, int y) {
        return x >= X && x <= X + Width && y >= Y && y <= Y + Height;
    }

    public boolean NeedsRedraw() {
        return _needsRedraw;
    }

    public void SetDirty() {
        _needsRedraw = true;
    }

    public void ClearDirty() {
        _needsRedraw = false;
    }

    public void SetSelected(boolean selected) {
        IsSelected = selected;
    }

    public boolean IsSelected() {
        return IsSelected;
    }

    // Interactions

    public void OnKeyPressed(char keyCode) {
    }

    public void OnKeyReleased(char keyCode) {
    }

    public boolean AbsoluteLeftClickAt(int absX, int absY) {
        int relX = absX - X;
        int relY = absY - Y;
        return RelativeLeftClickAt(relX, relY);
    }

    public boolean RelativeLeftClickAt(int relX, int relY) {
        return HandleWidgetClicks(relX, relY);
    }

    protected boolean HandleWidgetClicks(int relX, int relY) {
        for (int i = 0; i < _widgets.Size(); i++) {
            Widget widget = (Widget) _widgets.Get(i);
            if (widget.Contains(relX, relY)) {
                Logger.Info("WM", "Clicked at ".append(widget.Name));
                if (widget instanceof Button) {
                    Logger.Info("WM", "Button clicked");
                    Button button = (Button) widget;
                    button.Click();
                }
                return true;
            }
        }
        return false;
    }

    protected void AddWidget(Widget widget) {
        _widgets.Add(widget);
    }

    protected void RemoveWidget(Widget widget) {
        _widgets.Remove(widget);
    }

    protected void DrawWidgets() {
        for (int i = 0; i < _widgets.Size(); i++) {
            Widget widget = (Widget) _widgets.Get(i);
            widget.Draw();
            RenderTarget.Blit(widget.X, widget.Y, widget.RenderTarget, false);
        }
    }
}
