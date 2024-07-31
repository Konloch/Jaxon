package gui.components;

import formats.fonts.AFont;
import gui.Widget;
import kernel.hardware.keyboard.Key;
import util.StrBuilder;

public class TextField extends Widget {
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

    public TextField(
            int x,
            int y,
            int width,
            int height,
            int borderSpacing,
            int charSpacing,
            int lineSpacing,
            int fg,
            int bg,
            boolean enableCursor,
            AFont font) {
        super("component_textfield", x, y, width, height);

        _cursorX = 0;
        _cursorY = 0;
        _fg = fg;
        _bg = bg;
        Font = font;
        SpacingBorder = borderSpacing;
        SpacingW = charSpacing + font.SpacingW();
        SpacingH = lineSpacing + font.SpacingH();
        LineLength = (Width - borderSpacing * 2) / (font.Width() + SpacingW);
        LineCount = (Height - borderSpacing * 2) / (font.Height() + SpacingH);
        _characters = new byte[LineCount][LineLength];
        _characterColors = new int[LineCount][LineLength];
        _enableCursor = enableCursor;
    }

    public void SetCursor(int x, int y) {
        this._cursorX = x;
        this._cursorY = y;
        SetDirty();
    }

    public int GetCursorX() {
        return _cursorX;
    }

    public int GetCursorY() {
        return _cursorY;
    }

    public void SetBrushColor(int color) {
        this._fg = color;
    }

    public void Write(byte c) {
        if (_cursorX >= LineLength) {
            NewLine();
        }
        if (_cursorY >= LineCount) {
            NewLine();
        }

        _characters[_cursorY][_cursorX] = c;
        _characterColors[_cursorY][_cursorX] = _fg;
        _cursorX++;
        SetDirty();
    }

    public void NewLine() {
        _cursorX = 0;
        _cursorY++;
        if (_cursorY >= LineCount) {
            Scroll();
            _cursorY--;
        }
        SetDirty();
    }

    public void Backspace() {
        if (_cursorX > 0) {
            _cursorX--;
            _characters[_cursorY][_cursorX] = (byte) 0;
        } else {
            if (_cursorY > 0) {
                _cursorY--;
                int lastCharInLine = 0;
                while (lastCharInLine < LineLength && _characters[_cursorY][lastCharInLine] != 0) {
                    lastCharInLine++;
                }
                _cursorX = Math.Clamp(lastCharInLine, 0, LineLength - 1);
                _characters[_cursorY][_cursorX] = (byte) 0;
            }
        }
        SetDirty();
    }

    public void Write(String s) {
        for (int i = 0; i < s.length(); i++) {
            byte c = (byte) s.get(i);
            if (c == '\n') {
                NewLine();
            } else {
                Write(c);
            }
        }
        SetDirty();
    }

    public String toString() {
        StrBuilder sb = new StrBuilder();
        for (int i = 0; i < LineCount; i++) {
            for (int j = 0; j < LineLength; j++) {
                sb.Append((char) _characters[i][j]);
            }
            sb.Append('\n');
        }
        return sb.toString();
    }

    public void Scroll() {
        for (int i = 0; i < LineCount - 1; i++) {
            for (int j = 0; j < LineLength; j++) {
                _characters[i][j] = _characters[i + 1][j];
                _characterColors[i][j] = _characterColors[i + 1][j];
            }
        }
        for (int j = 0; j < LineLength; j++) {
            _characters[LineCount - 1][j] = (byte) ' ';
            _characterColors[LineCount - 1][j] = _bg;
        }
        SetDirty();
    }

    public void ClearText() {
        for (int i = 0; i < LineCount; i++) {
            for (int j = 0; j < LineLength; j++) {
                _characters[i][j] = (byte) '\0';
            }
        }
        SetCursor(0, 0);
        SetDirty();
    }

    public void ClearLine(int line) {
        for (int j = 0; j < LineLength; j++) {
            _characters[line][j] = (byte) 0;
        }
        SetDirty();
    }

    public void DrawCursor() {
        int xFactor = Font.Width() + SpacingW;
        int yFactor = Font.Height() + SpacingH;
        int xOffset = SpacingBorder;
        int yOffset = SpacingBorder;

        int x = xOffset + _cursorX * xFactor;
        int y = yOffset + _cursorY * yFactor;

        RenderTarget.Rectangle(x, y, 2, Font.Height(), _fg);
    }

    @Override
    public void Draw() {
        RenderTarget.Rectangle(0, 0, Width, Height, _bg);

        int xFactor = Font.Width() + SpacingW;
        int yFactor = Font.Height() + SpacingH;
        int xOffset = SpacingBorder;
        int yOffset = SpacingBorder;

        for (int i = 0; i < LineCount; i++) {
            for (int j = 0; j < LineLength; j++) {
                char character = (char) _characters[i][j];
                int characterColor = _characterColors[i][j];

                // Skip rendering if the character is not visible
                if (characterColor == _bg || Key.Ascii(character) == 0) {
                    continue;
                }
                int x = xOffset + j * xFactor;
                int y = yOffset + i * yFactor;

                Font.RenderToBitmap(RenderTarget, x, y, character, characterColor);
            }
        }
        if (_enableCursor) {
            DrawCursor();
        }
        ClearDirty();
    }
}
