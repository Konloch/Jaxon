package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class EditorIcon extends BinImage
{
	protected EditorIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.editor_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new EditorIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
