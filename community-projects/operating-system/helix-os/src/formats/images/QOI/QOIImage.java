package formats.images.QOI;

import formats.images.Image;

/*
 * The Quite OK Image format is a simple image format that is easy to decode and encode.
 * https://github.com/phoboslab/qoi
 *
 * Java implementation *borrowed* from https://github.com/saharNooby/qoi-java
 *
 * Online encoder: https://www.aconvert.com/image/png-to-qoi/
 */
public class QOIImage extends Image
{
	public int channels;
	/*
	 * 0: SRGB
	 * 1: Linear
	 */
	public int colorSpace;
	
	public QOIImage(int width, int height, int channels, int colorSpace, int[] pixelData)
	{
		this.width = width;
		this.height = height;
		this.pixelData = pixelData;
		this.channels = channels;
		this.colorSpace = colorSpace;
	}
}
