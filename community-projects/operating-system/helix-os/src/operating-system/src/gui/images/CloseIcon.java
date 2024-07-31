package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class CloseIcon extends BinImage {

    protected CloseIcon(byte[] data) {
        super(data);
    }

    @SuppressWarnings("static-access")
    public static final byte[] DATA = binimp.ByteData.close_binimg;

    public static Bitmap Load() {
        BinImage img = new CloseIcon(DATA);
        return new Bitmap(img.Width, img.Height, img.PixelData);
    }
}
