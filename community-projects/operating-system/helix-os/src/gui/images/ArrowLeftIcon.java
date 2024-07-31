package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class ArrowLeftIcon extends BinImage
{
	protected ArrowLeftIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.arrow_left_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new ArrowLeftIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
