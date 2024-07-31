package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class BackgroundBarn extends BinImage
{
	
	protected BackgroundBarn(byte[] data)
	{
		super(data);
	}
	
	@SuppressWarnings("static-access")
	public static final byte[] DATA = Binimp.ByteData.background_barn_binimg;
	
	public static Bitmap Load()
	{
		BinImage img = new BackgroundBarn(DATA);
		return new Bitmap(img.Width, img.Height, img.PixelData);
	}
}
