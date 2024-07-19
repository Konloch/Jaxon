package kernel.windows;

/**
 * @author S. Frenz
 */
public class BITMAPINFO extends STRUCT
{
	BITMAPINFOHEADER info;
	@SJC(count = 0)
	RGBQUAD[] rgbQuad;
}