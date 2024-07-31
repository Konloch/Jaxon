package java.util.queue;

import kernel.trace.logging.LogEntry;

public class QueueLogEntry
{
	private final int _size;
	private final LogEntry[] _buffer;
	private int _headIdx;
	private int _tailIdx;
	private int _count;
	
	public QueueLogEntry(int size)
	{
		this._size = size;
		_buffer = new LogEntry[size];
		_headIdx = 0;
		_tailIdx = 0;
		_count = 0;
	}
	
	public void Put(LogEntry c)
	{
		_buffer[_headIdx] = c;
		IncHead();
	}
	
	public LogEntry Get()
	{
		LogEntry c = _buffer[_tailIdx];
		IncTail();
		return c;
	}
	
	public LogEntry Peek()
	{
		return _buffer[_tailIdx];
	}
	
	public LogEntry Peek(int offset)
	{
		return _buffer[(_tailIdx + offset) % _size];
	}
	
	public LogEntry PeekBack(int i)
	{
		return _buffer[(_headIdx - i + _size - 1) % _size];
	}
	
	@SJC.Inline
	public int Count()
	{
		return _count;
	}
	
	@SJC.Inline
	public boolean IsEmpty()
	{
		return _count == 0;
	}
	
	@SJC.Inline
	public boolean ContainsNewElements()
	{
		return _headIdx != _tailIdx;
	}
	
	@SJC.Inline
	private void IncHead()
	{
		_headIdx = (_headIdx + 1) % _size;
		_count++;
	}
	
	@SJC.Inline
	private void IncTail()
	{
		_tailIdx = (_tailIdx + 1) % _size;
		_count--;
	}
}
