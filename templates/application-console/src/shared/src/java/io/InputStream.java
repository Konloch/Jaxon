package java.io;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public abstract class InputStream
{
	public abstract int read() throws IOException;
	
	public abstract int read(byte[] dest, int destPos, int length) throws IOException;
	
	public int read(byte[] dest) throws IOException
	{
		return read(dest, 0, dest.length);
	}
	
	public abstract long skip(long n) throws IOException;
	
	public abstract int available() throws IOException;
	
	public abstract void mark(int readlimit);
	
	public abstract void reset() throws IOException;
	
	public abstract boolean markSupported();
	
	public abstract void close() throws IOException;
}
