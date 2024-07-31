package gui.components.button;

import formats.fonts.Font3x6;
import kernel.display.Bitmap;

public class BitmapButton extends Button {

    private Bitmap _icon;

    public BitmapButton(
            String name,
            int x,
            int y,
            int size,
            Bitmap icon,
            IButtonListener listener) {
        super(name,
                x,
                y,
                size,
                size,
                0,
                0,
                "",
                Font3x6.Instance,
                listener);
        _icon = icon.Scale(size, size);
        Draw();
    }

    @Override
    public void Draw() {
        RenderTarget.Blit(0, 0, _icon, false);
    }
}
