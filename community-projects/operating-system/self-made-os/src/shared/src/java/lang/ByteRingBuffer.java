package java.lang;


public class ByteRingBuffer
{
	private static final int DEFAULT_SIZE = 64;
	private final byte[] buffer;
	private final int size;
	private int writePointer = 0;
	private int readPointer = 0;
	
	public ByteRingBuffer()
	{
		buffer = new byte[DEFAULT_SIZE];
		size = DEFAULT_SIZE;
	}
	
	public ByteRingBuffer(int size)
	{
		buffer = new byte[size];
		this.size = size;
	}
	
	private void increaseWritePointer()
	{
		if (writePointer == size - 1)
		{
			writePointer = 0;
			return;
		}
		writePointer++;
	}
	
	private void increaseReadPointer()
	{
		if (readPointer == writePointer)
			return;
		if (readPointer == size - 1)
		{
			readPointer = 0;
			return;
		}
		readPointer++;
	}
	
	//returns whether or not there is new data to be read
	//if canRead() returns false, then readByte() and peekByte() will read old data
	public boolean canRead()
	{
		return readPointer != writePointer;
	}
	
	//returns the next byte in the buffer without advancing the read pointer
	//if canRead() returns false, then this will return old data
	public byte peekByte()
	{
		return buffer[readPointer];
	}
	
	//returns the next byte in the buffer
	//if canRead() returns false, then this will return old data
	public byte readByte()
	{
		byte b = peekByte();
		increaseReadPointer();
		return b;
	}
	
	//writes the byte b into the buffer
	public void writeByte(byte b)
	{
		buffer[writePointer] = b;
		increaseWritePointer();
	}
	
	//clears the buffer
	public void clearBuffer()
	{
		readPointer = writePointer = 0;
	}
	
}
