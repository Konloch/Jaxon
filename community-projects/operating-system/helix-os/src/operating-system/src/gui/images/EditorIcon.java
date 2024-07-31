package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class EditorIcon extends BinImage {

    protected EditorIcon(byte[] data) {
        super(data);
    }

    @SuppressWarnings("static-access")
    public static final byte[] DATA = binimp.ByteData.editor_binimg;

    public static Bitmap Load() {
        BinImage img = new EditorIcon(DATA);
        return new Bitmap(img.Width, img.Height, img.PixelData);
    }
}
