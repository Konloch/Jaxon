package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class TransparencyTest extends BinImage
{
	protected TransparencyTest(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.transparency_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new TransparencyTest(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
