package java.util.vector;

import kernel.Kernel;

public class VecLines
{
	private static final int DEFAULT_CAPACITY = 10;
	private VecChar[] elements;
	private int size;
	
	public VecLines()
	{
		this.elements = new VecChar[DEFAULT_CAPACITY];
		this.size = 0;
	}
	
	public VecLines(int initialCapacity)
	{
		if (initialCapacity < 0)
			Kernel.panic("Illegal Capacity");
		this.elements = new VecChar[initialCapacity];
		this.size = 0;
	}
	
	public void clearKeepCapacity()
	{
		for (int i = 0; i < size; i++)
		{
			elements[i] = null;
		}
		size = 0;
	}
	
	@SJC.Inline
	public void add(VecChar element)
	{
		ensureCapacity(size + 1);
		elements[size++] = element;
	}
	
	@SJC.Inline
	public VecChar get(int index)
	{
		if (index < 0 || index >= size)
			Kernel.panic("Index out of bounds for vector access");
		return elements[index];
	}
	
	@SJC.Inline
	public int size()
	{
		return size;
	}
	
	@SJC.Inline
	public int capacity()
	{
		return elements.length;
	}
	
	private void ensureCapacity(int minCapacity)
	{
		if (minCapacity > elements.length)
		{
			int newCapacity = elements.length * 2;
			if (newCapacity < minCapacity)
			{
				newCapacity = minCapacity;
			}
			
			VecChar[] newElements = new VecChar[newCapacity];
			for (int i = 0; i < size; i++)
			{
				newElements[i] = elements[i];
			}
			elements = newElements;
		}
	}
}
