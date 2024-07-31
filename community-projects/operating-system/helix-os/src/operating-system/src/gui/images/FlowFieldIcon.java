package gui.images;

import formats.images.BinImage.BinImage;
import kernel.display.Bitmap;

public class FlowFieldIcon extends BinImage
{
	
	protected FlowFieldIcon(byte[] data)
	{
		super(data);
	}
	
	@SuppressWarnings("static-access")
	public static final byte[] DATA = Binimp.ByteData.flowfield_binimg;
	
	public static Bitmap Load()
	{
		BinImage img = new FlowFieldIcon(DATA);
		return new Bitmap(img.Width, img.Height, img.PixelData);
	}
}
