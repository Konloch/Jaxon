package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class BackgroundBarn extends BinImage
{
	protected BackgroundBarn(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.background_barn_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new BackgroundBarn(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
