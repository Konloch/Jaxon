package java.util.vector;

import kernel.Kernel;
import java.util.Array;

public class VecByte
{
	private static final int DEFAULT_CAPACITY = 10;
	private byte[] elements;
	private int size;
	
	public VecByte()
	{
		this.elements = new byte[DEFAULT_CAPACITY];
		this.size = 0;
	}
	
	public VecByte(int initialCapacity)
	{
		if (initialCapacity < 0)
			Kernel.panic("Illegal Capacity");
		
		this.elements = new byte[initialCapacity];
		this.size = 0;
	}
	
	public void clearKeepCapacity()
	{
		for (int i = 0; i < size; i++)
			elements[i] = 0;
		
		size = 0;
	}
	
	@SJC.Inline
	public void add(byte element)
	{
		ensureCapacity(size + 1);
		elements[size++] = element;
	}
	
	public void addAll(byte[] toAdd)
	{
		ensureCapacity(size + toAdd.length);
		
		for (int i = 0; i < toAdd.length; i++)
			this.elements[size + i] = toAdd[i];
		
		size += toAdd.length;
	}
	
	@SJC.Inline
	public byte get(int index)
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
	
	public byte[] toArray()
	{
		byte[] array = new byte[size];
		for (int i = 0; i < size; i++)
			array[i] = elements[i];
		
		return array;
	}
	
	private void ensureCapacity(int minCapacity)
	{
		if (minCapacity > capacity())
		{
			int newCapacity = Math.Max(capacity() * 2, minCapacity);
			if (newCapacity < minCapacity)
				Kernel.panic("Vector capacity overflow");
			
			elements = Array.copyOf(elements, newCapacity);
		}
	}
}
