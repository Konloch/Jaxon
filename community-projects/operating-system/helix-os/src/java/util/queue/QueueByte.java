package java.util.queue;

public class QueueByte
{
	private final int _size;
	private final byte[] _buffer;
	private int _headIdx;
	private int _tailIdx;
	private int _count;
	
	public QueueByte(int size)
	{
		this._size = size;
		_buffer = new byte[size];
		_headIdx = 0;
		_tailIdx = 0;
		_count = 0;
	}
	
	public void put(byte c)
	{
		_buffer[_headIdx] = c;
		incHead();
	}
	
	public byte get()
	{
		byte c = _buffer[_tailIdx];
		incTail();
		return c;
	}
	
	@SJC.Inline
	public int count()
	{
		return _count;
	}
	
	@SJC.Inline
	public boolean isEmpty()
	{
		return _count == 0;
	}
	
	@SJC.Inline
	public boolean containsNewElements()
	{
		return _headIdx != _tailIdx;
	}
	
	@SJC.Inline
	private void incHead()
	{
		_headIdx = (_headIdx + 1) % _size;
		_count++;
	}
	
	@SJC.Inline
	private void incTail()
	{
		_tailIdx = (_tailIdx + 1) % _size;
		_count--;
	}
}
