package formats.images;

public abstract class Image
{
	public int width;
	public int height;
	public int[] pixelData;
	
	protected Image()
	{
		this.width = 0;
		this.height = 0;
		this.pixelData = null;
	}
}
