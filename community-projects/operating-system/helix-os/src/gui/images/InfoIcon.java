package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class InfoIcon extends BinImage
{
	protected InfoIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.info_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new InfoIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
