package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class CursorModern extends BinImage
{
	protected CursorModern(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.cursor_modern_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new CursorModern(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
