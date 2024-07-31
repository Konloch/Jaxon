package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class ArrowLeftIcon extends BinImage {

    protected ArrowLeftIcon(byte[] data) {
        super(data);
    }

    @SuppressWarnings("static-access")
    public static final byte[] DATA = binimp.ByteData.arrow_left_binimg;

    public static Bitmap Load() {
        BinImage img = new ArrowLeftIcon(DATA);
        return new Bitmap(img.Width, img.Height, img.PixelData);
    }
}
