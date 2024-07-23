package hardware.keyboard;

public class KeyboardEventRingBuffer
{
	private static final int DEFAULT_SIZE = 64;
	private final KeyboardEvent[] buffer;
	private final int size;
	private int writePointer = 0;
	private int readPointer = 0;
	
	public KeyboardEventRingBuffer()
	{
		buffer = new KeyboardEvent[DEFAULT_SIZE];
		size = DEFAULT_SIZE;
	}
	
	public KeyboardEventRingBuffer(int size)
	{
		buffer = new KeyboardEvent[size];
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
	//if canRead() returns false, then readEvent() and peekEvent() will read old data
	public boolean canRead()
	{
		return readPointer != writePointer;
	}
	
	//returns the next event in the buffer without advancing the read pointer
	//if canRead() returns false, then this will return old data
	public KeyboardEvent peekEvent()
	{
		return buffer[readPointer];
	}
	
	//returns the next event in the buffer
	//if canRead() returns false, then this will return old data
	public KeyboardEvent readEvent()
	{
		KeyboardEvent b = peekEvent();
		increaseReadPointer();
		return b;
	}
	
	//writes the event e into the buffer
	public void writeEvent(KeyboardEvent e)
	{
		buffer[writePointer] = e;
		increaseWritePointer();
	}
	
	//clears the buffer
	public void clearBuffer()
	{
		readPointer = writePointer = 0;
	}
}
