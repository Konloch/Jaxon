package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class CursorModern extends BinImage
{
	
	protected CursorModern(byte[] data)
	{
		super(data);
	}
	
	@SuppressWarnings("static-access")
	public static final byte[] DATA = Binimp.ByteData.cursor_modern_binimg;
	
	public static Bitmap Load()
	{
		BinImage img = new CursorModern(DATA);
		return new Bitmap(img.Width, img.Height, img.PixelData);
	}
}
