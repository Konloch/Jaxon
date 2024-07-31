package java.util.vector;

import kernel.Kernel;

public class VecChar
{
	private static final int DEFAULT_CAPACITY = 10;
	private char[] elements;
	private int size;
	
	public VecChar()
	{
		this.elements = new char[DEFAULT_CAPACITY];
		this.size = 0;
	}
	
	public VecChar(int initialCapacity)
	{
		if (initialCapacity < 0)
			Kernel.panic("Illegal Capacity");
		this.elements = new char[initialCapacity];
		this.size = 0;
	}
	
	public void clearKeepCapacity()
	{
		for (int i = 0; i < size; i++)
		{
			elements[i] = 0;
		}
		size = 0;
	}
	
	@SJC.Inline
	public void add(char element)
	{
		ensureCapacity(size + 1);
		elements[size++] = element;
	}
	
	public void addAll(char[] toAdd)
	{
		ensureCapacity(size + toAdd.length);
		for (int i = 0; i < toAdd.length; i++)
		{
			this.elements[size + i] = toAdd[i];
		}
		size += toAdd.length;
	}
	
	@SJC.Inline
	public char get(int index)
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
	
	public char[] toArray()
	{
		char[] array = new char[size];
		for (int i = 0; i < size; i++)
		{
			array[i] = elements[i];
		}
		return array;
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
			
			char[] newElements = new char[newCapacity];
			for (int i = 0; i < size; i++)
			{
				newElements[i] = elements[i];
			}
			elements = newElements;
		}
	}
}
