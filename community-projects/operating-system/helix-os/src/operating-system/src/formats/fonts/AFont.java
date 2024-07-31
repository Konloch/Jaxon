package formats.fonts;

import kernel.display.Bitmap;
import kernel.display.GraphicsContext;
import util.BitHelper;

public abstract class AFont {

    public abstract int Width();

    public abstract int Height();

    public abstract int SpacingW();

    public abstract int SpacingH();

    public abstract int CharacterBitmapLine(int ch, int line);

    public abstract boolean Vertical();

    public void RenderToDisplay(GraphicsContext display, int x, int y, int ch, int color) {
        int fontWidth = Width();
        int fontHeight = Height();
        boolean fontVertical = Vertical();

        for (int charLine = 0; charLine < fontWidth; charLine++) {
            int b = CharacterBitmapLine(ch, charLine);
            for (int lineBit = 0; lineBit < fontHeight; lineBit++) {
                int bit = BitHelper.GetBit(b, lineBit);
                int posX = x;
                int posY = y;
                if (fontVertical) {
                    posX += charLine;
                    posY += lineBit;
                } else {
                    posX += lineBit;
                    posY += charLine;
                }
                if (bit == 1) {
                    display.Pixel(posX, posY, color);
                }
            }
        }
    }

    public void RenderToBitmap(Bitmap display, int x, int y, int ch, int color) {
        int fontWidth = Width();
        int fontHeight = Height();
        boolean fontVertical = Vertical();

        for (int charLine = 0; charLine < fontWidth; charLine++) {
            int b = CharacterBitmapLine(ch, charLine);
            for (int lineBit = 0; lineBit < fontHeight; lineBit++) {
                int bit = BitHelper.GetBit(b, lineBit);
                int posX = x;
                int posY = y;
                if (fontVertical) {
                    posX += charLine;
                    posY += lineBit;
                } else {
                    posX += lineBit;
                    posY += charLine;
                }
                if (bit == 1) {
                    display.SetPixel(posX, posY, color);
                }
            }
        }
    }
}
