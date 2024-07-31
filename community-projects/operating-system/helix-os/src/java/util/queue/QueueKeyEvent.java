package java.util.queue;

import kernel.hardware.keyboard.KeyEvent;

public class QueueKeyEvent
{
	private final int _capacity;
	private final KeyEvent[] _buffer;
	private int _headIdx;
	private int _tailIdx;
	private int _count;
	
	public QueueKeyEvent(int size)
	{
		this._capacity = size;
		_buffer = new KeyEvent[size];
		_headIdx = 0;
		_tailIdx = 0;
		_count = 0;
	}
	
	@SJC.Inline
	public int capacity()
	{
		return _capacity;
	}
	
	public void put(KeyEvent c)
	{
		_buffer[_headIdx] = c;
		incHead();
	}
	
	public KeyEvent peek()
	{
		return _buffer[_tailIdx];
	}
	
	public KeyEvent get()
	{
		KeyEvent c = _buffer[_tailIdx];
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
	public void incHead()
	{
		_headIdx = (_headIdx + 1) % _capacity;
		_count++;
	}
	
	@SJC.Inline
	private void incTail()
	{
		_tailIdx = (_tailIdx + 1) % _capacity;
		_count--;
	}
}
