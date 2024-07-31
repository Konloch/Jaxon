package formats.images.QOI;

import kernel.Kernel;

public class QOIDecoder
{
	static final int QOI_SRGB = 0;
	static final int QOI_LINEAR = 1;
	static final int QOI_OP_INDEX = 0x00;
	static final int QOI_OP_DIFF = 0x40;
	static final int QOI_OP_LUMA = 0x80;
	static final int QOI_OP_RUN = 0xC0;
	static final int QOI_OP_RGB = 0xFE;
	static final int QOI_OP_RGBA = 0xFF;
	static final int QOI_MASK_2 = 0xC0;
	static final int QOI_MAGIC = 'q' << 24 | 'o' << 16 | 'i' << 8 | 'f';
	static final byte[] QOI_PADDING = {0, 0, 0, 0, 0, 0, 0, 1};
	private static final int HASH_TABLE_SIZE = 64;
	
	static int GetHashTableIndexRGBA(byte r, byte g, byte b, byte a)
	{
		int hash = (r & 0xFF) * 3 + (g & 0xFF) * 5 + (b & 0xFF) * 7 + (a & 0xFF) * 11;
		return (hash & 0x3F) << 2;
	}
	
	static int GetHashTableIndexRGB(byte r, byte g, byte b)
	{
		int hash = (r & 0xFF) * 3 + (g & 0xFF) * 5 + (b & 0xFF) * 7 + 0xFF * 11;
		return (hash & 0x3F) << 2;
	}
	
	public static QOIImage Decode(byte[] inputStream, int channels)
	{
		if (channels != 0 && channels != 3 && channels != 4)
			Kernel.panic("Invalid channel count, must be 0, 3 or 4");
		
		Input in = new Input(inputStream);
		
		int headerMagic = in.readInt();
		if (headerMagic != QOI_MAGIC)
			Kernel.panic("Invalid magic value, probably not a QOI image");
		
		int width = in.readInt();
		if (width < 1)
			Kernel.panic("Invalid image width");
		
		int height = in.readInt();
		if (height < 1)
			Kernel.panic("Invalid image height");
		
		int storedChannels = Integer.ubyte(in.read());
		if (storedChannels != 3 && storedChannels != 4)
			Kernel.panic("Invalid stored channel count");
		
		if (channels == 0)
			channels = storedChannels;
		
		int colorSpace = in.readColorSpace();
		
		byte[] pixelDataRaw = channels == 3 ? read3(in, width, height) : read4(in, width, height);
		
		for (int i = 0; i < 8; i++)
		{
			if (QOI_PADDING[i] != in.readSkipBuffer())
				Kernel.panic("Invalid padding");
		}
		
		// Convert pixel data to 2D array
		int[] pixelData = new int[width * height];
		for (int i = 0; i < width * height; i++)
		{
			int r = pixelDataRaw[i * channels] & 0xFF;
			int g = pixelDataRaw[i * channels + 1] & 0xFF;
			int b = pixelDataRaw[i * channels + 2] & 0xFF;
			
			if (channels == 4)
			{
				int a = pixelDataRaw[i * channels + 3] & 0xFF;
				pixelData[i] = (r << 16) | (g << 8) | b | (a << 24);
			}
			else
			{
				pixelData[i] = (r << 16) | (g << 8) | b | 0xFF000000;
			}
		}
		
		return new QOIImage(width, height, channels, colorSpace, pixelData);
	}
	
	private static byte[] read3(Input in, int width, int height)
	{
		int pixelDataLength = Math.MultiplyExact(Math.MultiplyExact(width, height), 3);
		byte[] pixelData = new byte[pixelDataLength];
		byte[] index = new byte[HASH_TABLE_SIZE * 4];
		
		byte pixelR = 0;
		byte pixelG = 0;
		byte pixelB = 0;
		byte pixelA = (byte) 0xFF;
		
		for (int pixelPos = 0; pixelPos < pixelDataLength; pixelPos += 3)
		{
			int b1 = in.read() & 0xFF;
			
			if (b1 == QOI_OP_RGB)
			{
				pixelR = in.read();
				pixelG = in.read();
				pixelB = in.read();
			}
			else if (b1 == QOI_OP_RGBA)
			{
				pixelR = in.read();
				pixelG = in.read();
				pixelB = in.read();
				pixelA = in.read();
			}
			else
			{
				switch (b1 & QOI_MASK_2)
				{
					case QOI_OP_INDEX:
						int indexPos = (b1 & ~QOI_MASK_2) << 2;
						pixelR = index[indexPos];
						pixelG = index[indexPos + 1];
						pixelB = index[indexPos + 2];
						pixelA = index[indexPos + 3];
						break;
					case QOI_OP_DIFF:
						pixelR += ((b1 >> 4) & 0x03) - 2;
						pixelG += ((b1 >> 2) & 0x03) - 2;
						pixelB += (b1 & 0x03) - 2;
						break;
					case QOI_OP_LUMA:
						// Safe widening conversion
						int b2 = in.read();
						int vg = (b1 & 0x3F) - 32;
						pixelR += vg - 8 + ((b2 >> 4) & 0x0F);
						pixelG += vg;
						pixelB += vg - 8 + (b2 & 0x0F);
						break;
					case QOI_OP_RUN:
						int run = b1 & 0x3F;
						for (int i = 0; i < run; i++)
						{
							pixelData[pixelPos] = pixelR;
							pixelData[pixelPos + 1] = pixelG;
							pixelData[pixelPos + 2] = pixelB;
							pixelPos += 3;
						}
						break;
				}
			}
			
			int indexPos = GetHashTableIndexRGBA(pixelR, pixelG, pixelB, pixelA);
			index[indexPos] = pixelR;
			index[indexPos + 1] = pixelG;
			index[indexPos + 2] = pixelB;
			index[indexPos + 3] = pixelA;
			
			pixelData[pixelPos] = pixelR;
			pixelData[pixelPos + 1] = pixelG;
			pixelData[pixelPos + 2] = pixelB;
		}
		
		return pixelData;
	}
	
	static byte[] createHashTableRGBA()
	{
		return new byte[HASH_TABLE_SIZE * 4];
	}
	
	static int getHashTableIndexRGBA(byte r, byte g, byte b, byte a)
	{
		int hash = (r & 0xFF) * 3 + (g & 0xFF) * 5 + (b & 0xFF) * 7 + (a & 0xFF) * 11;
		return (hash & 0x3F) << 2;
	}
	
	// Read into 4-channel RGBA buffer
	private static byte[] read4(Input in, int width, int height)
	{
		// Check for overflow on big images
		int pixelDataLength = Math.MultiplyExact(Math.MultiplyExact(width, height), 4);
		
		byte[] pixelData = new byte[pixelDataLength];
		
		byte[] index = createHashTableRGBA();
		
		byte pixelR = 0;
		byte pixelG = 0;
		byte pixelB = 0;
		byte pixelA = (byte) 0xFF;
		
		for (int pixelPos = 0; pixelPos < pixelDataLength; pixelPos += 4)
		{
			int b1 = in.read() & 0xFF;
			
			if (b1 == QOI_OP_RGB)
			{
				pixelR = in.read();
				pixelG = in.read();
				pixelB = in.read();
			}
			else if (b1 == QOI_OP_RGBA)
			{
				pixelR = in.read();
				pixelG = in.read();
				pixelB = in.read();
				pixelA = in.read();
			}
			else
			{
				switch (b1 & QOI_MASK_2)
				{
					case QOI_OP_INDEX:
						int indexPos = (b1 & ~QOI_MASK_2) << 2;
						
						pixelR = index[indexPos];
						pixelG = index[indexPos + 1];
						pixelB = index[indexPos + 2];
						pixelA = index[indexPos + 3];
						
						break;
					case QOI_OP_DIFF:
						pixelR += ((b1 >> 4) & 0x03) - 2;
						pixelG += ((b1 >> 2) & 0x03) - 2;
						pixelB += (b1 & 0x03) - 2;
						
						break;
					case QOI_OP_LUMA:
						// Safe widening conversion
						int b2 = in.read();
						int vg = (b1 & 0x3F) - 32;
						pixelR += vg - 8 + ((b2 >> 4) & 0x0F);
						pixelG += vg;
						pixelB += vg - 8 + (b2 & 0x0F);
						
						break;
					case QOI_OP_RUN:
						int run = b1 & 0x3F;
						
						for (int i = 0; i < run; i++)
						{
							pixelData[pixelPos] = pixelR;
							pixelData[pixelPos + 1] = pixelG;
							pixelData[pixelPos + 2] = pixelB;
							pixelData[pixelPos + 3] = pixelA;
							
							pixelPos += 4;
						}
						
						break;
				}
			}
			
			int indexPos = getHashTableIndexRGBA(pixelR, pixelG, pixelB, pixelA);
			index[indexPos] = pixelR;
			index[indexPos + 1] = pixelG;
			index[indexPos + 2] = pixelB;
			index[indexPos + 3] = pixelA;
			
			pixelData[pixelPos] = pixelR;
			pixelData[pixelPos + 1] = pixelG;
			pixelData[pixelPos + 2] = pixelB;
			pixelData[pixelPos + 3] = pixelA;
		}
		
		return pixelData;
	}
	
	private static final class Input
	{
		private final byte[] buffer;
		private int position;
		private int read;
		
		private Input(byte[] data)
		{
			this.buffer = data;
			this.position = 0;
		}
		
		public byte read()
		{
			if (this.position >= this.buffer.length)
				Kernel.panic("Unexpected end of stream");
			
			return this.buffer[this.position++];
		}
		
		public byte readSkipBuffer()
		{
			if (this.buffer == null)
				return read();
			
			if (this.position == this.read)
				return read();
			
			return this.buffer[this.position++];
		}
		
		public int readInt()
		{
			int a = read() & 0xFF;
			int b = read() & 0xFF;
			int c = read() & 0xFF;
			int d = read() & 0xFF;
			return (a << 24) | (b << 16) | (c << 8) | d;
		}
		
		public int readColorSpace()
		{
			int value = read() & 0xFF;
			
			switch (value)
			{
				case QOI_SRGB:
					return QOI_SRGB;
				case QOI_LINEAR:
					return QOI_LINEAR;
			}
			
			Kernel.panic("Invalid Color Space");
			return -1;
		}
	}
}
