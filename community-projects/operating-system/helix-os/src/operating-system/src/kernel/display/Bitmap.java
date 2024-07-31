package kernel.display;

import kernel.Kernel;
import kernel.memory.Memory;

public class Bitmap {
    public int Width;
    public int Height;
    public int[] PixelData;
    public boolean IsTransparent;

    public Bitmap(int width, int height, int[] pixelData) {
        Width = width;
        Height = height;
        PixelData = pixelData;
        IsTransparent = AnyTransparency();
    }

    public Bitmap(int width, int height, boolean isTransparent) {
        Width = width;
        Height = height;
        IsTransparent = isTransparent;
        PixelData = new int[Width * Height];
    }

    public void Clear() {
        int from = MAGIC.addr(PixelData[0]);
        int len = PixelData.length;
        Memory.Memset(from, len * 4, (byte) 0);
    }

    public int GetPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= Width || y >= Height) {
            Kernel.panic("Bitmap.GetPixel: out of bounds (".append(x).append(", ").append(y).append(")"));
        }

        int index = Index(x, y);

        if (index < 0 || index >= PixelData.length) {
            Kernel.panic("Bitmap.GetPixel: index out of bounds");
            return 0;
        }

        return PixelData[index];
    }

    @SJC.Inline
    public void SetPixel(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= Width || y >= Height) {
            return;
        }

        PixelData[x + y * Width] = color;
    }

    @SJC.Inline
    public void Darken(int amount) {
        for (int i = 0; i < PixelData.length; i++) {
            int color = PixelData[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            r = (r - amount) & ~((r - amount) >> 31);
            g = (g - amount) & ~((g - amount) >> 31);
            b = (b - amount) & ~((b - amount) >> 31);
            PixelData[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    public void Rectangle(int x, int y, int width, int height, int color) {
        if (PixelData == null) {
            Kernel.panic("Bitmap.Rectangle: PixelData is null");
            return;
        }

        if (PixelData.length == 0) {
            Kernel.panic("Bitmap.Rectangle: PixelData is len 0");
            return;
        }

        // only draw visible part
        if (x < 0) {
            width += x;
            x = 0;
        }

        if (x + width > Width) {
            width = Width - x;
        }

        if (y < 0) {
            height += y;
            y = 0;
        }

        if (y + height > Height) {
            height = Height - y;
        }

        // this should not happen but it does and im confused
        // somehow returning fixes it but it makes no sense
        if (width <= 0 || height <= 0) {
            return;
        }

        for (int i = 0; i < height; i++) {
            int index = Index(x, y + i);
            int addr = MAGIC.addr(PixelData[index]);
            Memory.Memset32(addr, width, color);
        }
    }

    public void Blit(int x, int y, Bitmap bitmap, boolean transparent) {
        if (bitmap == null) {
            Kernel.panic("VESAGraphics.setBitmap: mode or bitmap is null");
            return;
        }

        int off_bitmap_x = 0;
        int off_bitmap_y = 0;
        int up_to_x = bitmap.Width;
        int up_to_y = bitmap.Height;

        if (x < 0) {
            off_bitmap_x = -x;
            up_to_x += x;
            x = 0;
        }

        if (y < 0) {
            off_bitmap_y = -y;
            up_to_y += y;
            y = 0;
        }

        if (x + up_to_x > Width) {
            up_to_x = Width - x;
        }

        if (y + up_to_y > Height) {
            up_to_y = Height - y;
        }

        if (up_to_x <= 0 || up_to_y <= 0) {
            Kernel.panic("Bitmap.blit: up_to_x <= 0 || up_to_y <= 0");
        }

        if (transparent) {
            for (int cur_y = 0; cur_y < up_to_y; cur_y++) {
                for (int cur_x = 0; cur_x < up_to_x; cur_x++) {
                    int self_index = Index(x + cur_x, y + cur_y);
                    int bitmap_index = bitmap.Index(off_bitmap_x + cur_x, off_bitmap_y + cur_y);
                    PixelData[self_index] = Blend(bitmap.PixelData[bitmap_index], PixelData[self_index]);
                }
            }
        } else {
            for (int cur_y = 0; cur_y < up_to_y; cur_y++) {
                int index = Index(x, y + cur_y);
                int addr = MAGIC.addr(PixelData[index]);
                int bitmap_index = bitmap.Index(off_bitmap_x, off_bitmap_y + cur_y);
                int bitmap_addr = MAGIC.addr(bitmap.PixelData[bitmap_index]);
                Memory.Memcopy32(bitmap_addr, addr, up_to_x);
            }
        }

    }

    public Bitmap Scale(int newWidth, int newHeight) {
        int[] newPixelData = new int[newWidth * newHeight];
        int x_ratio = (Width << 16) / newWidth + 1;
        int y_ratio = (Height << 16) / newHeight + 1;

        for (int i = 0; i < newHeight; i++) {
            for (int j = 0; j < newWidth; j++) {
                int x = (j * x_ratio) >> 16;
                int y = (i * y_ratio) >> 16;
                newPixelData[j + i * newWidth] = PixelData[x + y * Width];
            }
        }

        return new Bitmap(newWidth, newHeight, newPixelData);
    }

    @SJC.Inline
    private int Blend(int withAlpha, int noAlpha) {
        int alpha = (withAlpha >> 24) & 0xFF;

        if (alpha == 0) {
            return noAlpha;
        }

        if (alpha == 255) {
            return withAlpha;
        }

        int alpha_rem = 255 - alpha;
        int r = (alpha * ((withAlpha >> 16) & 0xFF) + alpha_rem * ((noAlpha >> 16) & 0xFF)) >> 8;
        int g = (alpha * ((withAlpha >> 8) & 0xFF) + alpha_rem * ((noAlpha >> 8) & 0xFF)) >> 8;
        int b = (alpha * (withAlpha & 0xFF) + alpha_rem * (noAlpha & 0xFF)) >> 8;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @SJC.Inline
    public int Row(int y) {
        return y * Width;
    }

    @SJC.Inline
    public int Index(int x, int y) {
        return x + y * Width;
    }

    public boolean AnyTransparency() {
        for (int i = 0; i < PixelData.length; i++) {
            if ((PixelData[i] & 0xFF000000) != 0) {
                return true;
            }
        }
        return false;
    }
}
