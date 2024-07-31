package formats.images;

public abstract class Image {
    public int Width;
    public int Height;
    public int[] PixelData;

    protected Image() {
        this.Width = 0;
        this.Height = 0;
        this.PixelData = null;
    }
}
