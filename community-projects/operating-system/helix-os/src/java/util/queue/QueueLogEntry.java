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
	
	public void put(LogEntry c)
	{
		_buffer[_headIdx] = c;
		incHead();
	}
	
	public LogEntry get()
	{
		LogEntry c = _buffer[_tailIdx];
		incTail();
		return c;
	}
	
	public LogEntry peek()
	{
		return _buffer[_tailIdx];
	}
	
	public LogEntry peek(int offset)
	{
		return _buffer[(_tailIdx + offset) % _size];
	}
	
	public LogEntry peekBack(int i)
	{
		return _buffer[(_headIdx - i + _size - 1) % _size];
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
