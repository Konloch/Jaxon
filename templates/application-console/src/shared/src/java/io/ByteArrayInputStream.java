package java.io;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class ByteArrayInputStream extends InputStream
{
	private final byte[] buffer;
	private int position;
	private int markPosition;
	
	public ByteArrayInputStream(byte[] buffer)
	{
		this.buffer = buffer;
		this.position = 0;
		this.markPosition = 0;
	}
	
	@Override
	public int read() throws IOException
	{
		if (position >= buffer.length)
			return -1;
		
		return buffer[position++] & 0xFF;
	}
	
	@Override
	public int read(byte[] dest, int destPos, int length) throws IOException
	{
		if (position >= buffer.length)
			return -1;
		
		int bytesRead = Math.min(length, buffer.length - position);
		
		if (bytesRead >= 0)
			Arrays.copy(buffer, position, dest, destPos, bytesRead);
		
		position += bytesRead;
		return bytesRead;
	}
	
	@Override
	public long skip(long n) throws IOException
	{
		long skipped = Math.min(n, buffer.length - position);
		position += skipped;
		return skipped;
	}
	
	@Override
	public int available() throws IOException
	{
		return buffer.length - position;
	}
	
	@Override
	public void mark(int readlimit)
	{
		markPosition = position;
	}
	
	@Override
	public void reset() throws IOException
	{
		position = markPosition;
	}
	
	@Override
	public boolean markSupported()
	{
		return true;
	}
	
	@Override
	public void close() throws IOException
	{
	}
}
