package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class InfoIcon extends BinImage
{
	
	protected InfoIcon(byte[] data)
	{
		super(data);
	}
	
	@SuppressWarnings("static-access")
	public static final byte[] DATA = Binimp.ByteData.info_binimg;
	
	public static Bitmap Load()
	{
		BinImage img = new InfoIcon(DATA);
		return new Bitmap(img.Width, img.Height, img.PixelData);
	}
}
