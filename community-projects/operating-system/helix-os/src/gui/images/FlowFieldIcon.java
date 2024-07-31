package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class FlowFieldIcon extends BinImage
{
	protected FlowFieldIcon(byte[] data)
	{
		super(data);
	}
	
	public static final byte[] DATA = binimp.ByteData.flowfield_binimg;
	
	public static Bitmap load()
	{
		BinImage img = new FlowFieldIcon(DATA);
		return new Bitmap(img.width, img.height, img.pixelData);
	}
}
