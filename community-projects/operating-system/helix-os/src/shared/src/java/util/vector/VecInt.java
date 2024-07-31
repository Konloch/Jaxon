package java.util.vector;

import kernel.Kernel;

public class VecInt
{
	private static final int DEFAULT_CAPACITY = 10;
	private int[] elements;
	private int size;
	
	public VecInt()
	{
		this.elements = new int[DEFAULT_CAPACITY];
		this.size = 0;
	}
	
	public VecInt(int initialCapacity)
	{
		if (initialCapacity < 0)
			Kernel.panic("Illegal Capacity");
		
		this.elements = new int[initialCapacity];
		this.size = 0;
	}
	
	public void add(int element)
	{
		ensureCapacity(size + 1);
		elements[size++] = element;
	}
	
	public int get(int index)
	{
		if (index < 0 || index >= size)
			Kernel.panic("Index out of bounds for vector access");

		return elements[index];
	}
	
	public int size()
	{
		return size;
	}
	
	private void ensureCapacity(int minCapacity)
	{
		if (minCapacity > elements.length)
		{
			int newCapacity = elements.length * 2;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			
			int[] newElements = new int[newCapacity];
			for (int i = 0; i < size; i++)
				newElements[i] = elements[i];
			elements = newElements;
		}
	}
}
