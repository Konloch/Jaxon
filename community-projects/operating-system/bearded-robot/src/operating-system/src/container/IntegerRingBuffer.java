package container;

public class IntegerRingBuffer
{
	private final int[] elements;
	
	private int size = 0;
	
	private int in = 0;
	
	private int out = 0;
	
	public IntegerRingBuffer(int maximumSize)
	{
		this.elements = new int[maximumSize];
	}
	
	public int size()
	{
		return this.size;
	}
	
	public int maximumSize()
	{
		return this.elements.length;
	}
	
	public void push(int element)
	{
		if (in >= this.elements.length)
		{
			in %= this.elements.length;
		}
		
		if (in == out && size != 0)
		{
			this.pop();
		}
		
		this.elements[in] = element;
		this.in = (this.in + 1) % this.elements.length;
		this.size++;
	}
	
	public void pop()
	{
		if (this.size-- > 0)
		{
			this.out = (this.out + 1) % this.elements.length;
		}
	}
	
	public int front()
	{
		if (this.size > 0)
		{
			return this.elements[this.out];
		}
		else
		{
			return 0;
		}
	}
}
