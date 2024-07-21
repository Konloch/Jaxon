package java.io;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class ByteArrayOutputStream extends OutputStream
{
	private byte[] buffer;
	private int position;
	
	public ByteArrayOutputStream(int initialCapacity)
	{
		buffer = new byte[initialCapacity];
	}
	
	public ByteArrayOutputStream(byte[] initialBuffer)
	{
		buffer = initialBuffer;
	}
	
	@Override
	public void write(int b) throws IOException
	{
		ensureCapacity(position + 1);
		buffer[position++] = (byte) b;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		ensureCapacity(position + len);
		Arrays.copy(b, off, buffer, position, len);
		position += len;
	}
	
	@Override
	public void flush() throws IOException
	{
	}
	
	@Override
	public void close() throws IOException
	{
	}
	
	private void ensureCapacity(int capacity)
	{
		if (buffer.length < capacity)
		{
			byte[] newBuffer = new byte[Math.max(capacity, buffer.length * 2)];
			Arrays.copy(buffer, 0, newBuffer, 0, buffer.length);
			buffer = newBuffer;
		}
	}
	
	public byte[] toByteArray()
	{
		byte[] result = new byte[position];
		Arrays.copy(buffer, 0, result, 0, position);
		return result;
	}
	
	public int size()
	{
		return position;
	}
}
