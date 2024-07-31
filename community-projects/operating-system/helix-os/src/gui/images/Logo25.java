package gui.images;

import formats.images.QOI.QOIDecoder;
import formats.images.QOI.QOIImage;
import kernel.display.Bitmap;

public class Logo25 extends QOIImage
{
	public static final byte[] DATA = binimp.ByteData.logo_25_qoi;
	
	protected Logo25(int width, int height, int channels, int colorSpace, int[] pixelData)
	{
		super(width, height, channels, colorSpace, pixelData);
	}
	
	public static Bitmap load()
	{
		QOIImage img = QOIDecoder.decode(DATA, 3);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
