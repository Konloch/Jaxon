package gui.displays.windows;

import formats.fonts.AFont;
import gui.Window;
import gui.components.TextField;
import kernel.Kernel;
import kernel.hardware.keyboard.Key;
import kernel.trace.logging.Logger;

public class Editor extends Window {
    private TextField _textField;

    public Editor(
            String title,
            int x,
            int y,
            int width,
            int height,
            int border,
            int charSpacing,
            int lineSpacing,
            AFont font) {
        super(title, x, y, width, height, true);

        int bg = Kernel.Display.Rgb(20, 20, 20);
        int fg = Kernel.Display.Rgb(255, 255, 255);
        _textField = new TextField(
                0,
                0,
                ContentWidth,
                ContentHeight,
                border,
                charSpacing,
                lineSpacing,
                fg,
                bg,
                true,
                font);
    }

    public void DrawContent() {
        if (_textField.NeedsRedraw()) {
            _textField.Draw();
        }
        RenderTarget.Blit(ContentRelativeX, ContentRelativeY, _textField.RenderTarget, false);
        ClearDirty();
    }

    @Override
    public boolean NeedsRedraw() {
        return _textField.NeedsRedraw() || super.NeedsRedraw();
    }

    @Override
    public void OnKeyPressed(char key) {
        switch (key) {
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
                if (Key.Ascii(key) != 0) {
                    _textField.Write((byte) key);
                }
                break;
        }
    }

    @Override
    public boolean RelativeLeftClickAt(int relX, int relY) {
        if (super.RelativeLeftClickAt(relX, relY)) {
            return true;
        }

        int cellW = relX / (_textField.Font.Width() + _textField.SpacingW);
        int cx = Math.Clamp(cellW, 0, _textField.LineLength - 1);

        int cellH = relY / (_textField.Font.Height() + _textField.SpacingH);
        int cy = Math.Clamp(cellH, 0, _textField.LineCount - 1);

        _textField.SetCursor(cx, cy);
        Logger.Info("Cursor", "set to".append(cx).append(" ").append(cy));
        return true;
    }

    private void MoveCursorUp() {
        int x = _textField.GetCursorX();
        int y = _textField.GetCursorY();
        if (y > 0) {
            _textField.SetCursor(x, y - 1);
        }
    }

    private void MoveCursorDown() {
        int x = _textField.GetCursorX();
        int y = _textField.GetCursorY();
        if (y < _textField.LineCount - 1) {
            _textField.SetCursor(x, y + 1);
        }
    }

    private void MoveCursorLeft() {
        int x = _textField.GetCursorX();
        int y = _textField.GetCursorY();
        if (x > 0) {
            _textField.SetCursor(x - 1, y);
        }
    }

    private void MoveCursorRight() {
        int x = _textField.GetCursorX();
        int y = _textField.GetCursorY();
        if (x < _textField.LineLength - 1) {
            _textField.SetCursor(x + 1, y);
        }
    }

    @Override
    public void MoveBy(int dragDiffX, int dragDiffY) {
        super.MoveBy(dragDiffX, dragDiffY);
        // _textField.DragBy(dragDiffX, dragDiffY);
    }

    @Override
    public void Update() {
    }
}
