package java.io;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class ByteWriter extends InputStream
{
	public byte[] contents = new byte[0];
	
	public ByteWriter()
	{
	
	}
	
	public ByteWriter(byte[] bytes)
	{
		this.contents = bytes;
	}
	
	public int read() throws IOException
	{
		return 0;
	}
	
	public int read(byte[] b, int off, int len) throws IOException
	{
		return 0;
	}
	
	public long skip(long n) throws IOException
	{
		return 0;
	}
	
	public int available() throws IOException
	{
		return 0;
	}
	
	public void mark(int readlimit)
	{
	
	}
	
	public void reset() throws IOException
	{
	
	}
	
	public boolean markSupported()
	{
		return false;
	}
	
	public void close() throws IOException
	{
	
	}
}
