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
public class QOIImage extends Image {
    public int Channels;
    /*
     * 0: SRGB
     * 1: Linear
     */
    public int ColorSpace;

    public QOIImage(int width, int height, int channels, int colorSpace, int[] pixelData) {
        this.Width = width;
        this.Height = height;
        this.PixelData = pixelData;
        this.Channels = channels;
        this.ColorSpace = colorSpace;
    }
}
