package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class TransparencyTest extends BinImage {

    protected TransparencyTest(byte[] data) {
        super(data);
    }

    @SuppressWarnings("static-access")
    public static final byte[] DATA = binimp.ByteData.transparency_binimg;

    public static Bitmap Load() {
        BinImage img = new TransparencyTest(DATA);
        return new Bitmap(img.Width, img.Height, img.PixelData);
    }
}
