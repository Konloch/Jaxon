package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class CloseIcon extends BinImage
{
	protected CloseIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.close_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new CloseIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
