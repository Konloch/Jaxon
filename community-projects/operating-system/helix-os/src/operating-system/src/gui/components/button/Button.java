package gui.components.button;

import formats.fonts.AFont;
import gui.Widget;

public class Button extends Widget {
    public AFont Font;

    protected int _bg;
    protected int _fg;
    protected String _text;
    protected String _name;

    public IButtonListener EventListener;

    public Button(
            String name,
            int x,
            int y,
            int width,
            int height,
            int fg,
            int bg,
            String text,
            AFont font,
            IButtonListener listener) {
        super(name, x, y, width, height);
        _fg = fg;
        _bg = bg;
        Font = font;
        EventListener = listener;
        _text = text;
        _name = name;

    }

    @Override
    public void Draw() {
        RenderTarget.Rectangle(0, 0, Width, Height, _bg);
    }

    public void Click() {
        if (EventListener != null) {
            EventListener.OnButtonClicked(new ButtonClickedEventArgs(_name));
        }
    }
}
