package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class ArrowRightIcon extends BinImage
{
	protected ArrowRightIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.arrow_right_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new ArrowRightIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
