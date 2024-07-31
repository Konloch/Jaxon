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
	public int Capacity()
	{
		return _capacity;
	}
	
	public void Put(KeyEvent c)
	{
		_buffer[_headIdx] = c;
		IncHead();
	}
	
	public KeyEvent Peek()
	{
		return _buffer[_tailIdx];
	}
	
	public KeyEvent Get()
	{
		KeyEvent c = _buffer[_tailIdx];
		IncTail();
		return c;
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
	public void IncHead()
	{
		_headIdx = (_headIdx + 1) % _capacity;
		_count++;
	}
	
	@SJC.Inline
	private void IncTail()
	{
		_tailIdx = (_tailIdx + 1) % _capacity;
		_count--;
	}
}
