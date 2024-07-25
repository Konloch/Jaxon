package kernel.windows;

import java.lang.STRUCT;

/**
 * @author S. Frenz
 */
public class BITMAPINFOHEADER extends STRUCT
{
	int biSize;
	int biWidth;
	int biHeight;
	short biPlanes;
	short biBitCount;
	int biCompression;
	int biSizeImage;
	int biXPelsPerMeter;
	int biYPelsPerMeter;
	int biClrUsed;
	int biClrImportant;
}