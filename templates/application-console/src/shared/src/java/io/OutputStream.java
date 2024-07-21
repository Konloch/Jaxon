package java.io;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public abstract class OutputStream
{
	public abstract void write(int b) throws IOException;
	
	public abstract void write(byte[] b, int off, int len) throws IOException;
	
	public abstract void flush() throws IOException;
	
	public abstract void close() throws IOException;
}
