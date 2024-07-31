package gui.images;

import formats.images.QOI.QOIDecoder;
import formats.images.QOI.QOIImage;
import kernel.display.Bitmap;

public class Logo25 extends QOIImage
{
	
	@SuppressWarnings("static-access")
	public static final byte[] DATA = Binimp.ByteData.logo_25_qoi;
	
	protected Logo25(int width, int height, int channels, int colorSpace, int[] pixelData)
	{
		super(width, height, channels, colorSpace, pixelData);
	}
	
	public static Bitmap Load()
	{
		QOIImage img = QOIDecoder.Decode(DATA, 3);
		return new Bitmap(img.Width, img.Height, img.PixelData);
	}
}
