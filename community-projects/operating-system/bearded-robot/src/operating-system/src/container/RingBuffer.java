package container;

/**
 * Provides a ring buffer.
 */
public class RingBuffer
{
	/**
	 * Single buffer element.
	 */
	private static class Element
	{
		public Element next;
		public Object value;
	}
	
	/**
	 * The capacity of the buffer.
	 */
	private int capacity = Integer.MAX_VALUE;
	
	/**
	 * The number of elements in the buffer.
	 */
	private int size = 0;
	
	private Element in = null;
	
	private Element out = null;
	
	public RingBuffer()
	{
	}
	
	public RingBuffer(int capacity)
	{
		this.capacity = capacity;
	}
	
	public int capacity()
	{
		return this.capacity;
	}
	
	public int size()
	{
		return this.size;
	}
	
	/**
	 * Adds an element to the buffer.
	 *
	 * @param value An object to be stored in the buffer.
	 */
	public void push(Object value)
	{
		if (this.size < this.capacity)
		{
			// Falls die Kapazität nicht ausgeschöpft ist, wird ein neues
			// Element erzeugt.
			Element newElement = new Element();
			newElement.value = value;
			
			if (this.size <= 0)
			{
				// Falls der Puffer leer ist ( => in == out == null), werden
				// sowohl in als auch out auf das neuen Element gesetzt und das
				// neue Element mit sich selbst verkettet.
				newElement.next = newElement;
				this.in = this.out = newElement;
			}
			else
			{
				newElement.next = this.in.next;
				this.in.next = newElement;
				this.in = newElement;
			}
			
			size++;
		}
		else
		{
			// Falls die Kapazität ausgeschöpft ist, wird der Wert des ältesten
			// Elements ersetzt.
			this.in = this.out;
			this.out = this.out.next;
			this.in.value = value;
		}
	}
	
	/**
	 * Deletes the first element.
	 */
	public void pop()
	{
		if (this.size > 0)
		{
			this.in.next = this.out.next;
			this.out = this.out.next;
			this.size--;
		}
	}
	
	/**
	 * Returns the first element.
	 *
	 * @return
	 */
	public Object front()
	{
		if (this.size > 0)
		{
			return this.out.value;
		}
		return null;
	}
	
	/**
	 * Skip element.
	 */
	public void next()
	{
		if (this.size > 0)
		{
			this.in = this.out;
			this.out = this.out.next;
		}
	}
}
