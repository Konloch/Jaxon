package java.util.vector;

import kernel.Kernel;
import kernel.display.vesa.VESAMode;

public class VecVesaMode
{
	private static final int DEFAULT_CAPACITY = 10;
	private VESAMode[] elements;
	private int size;
	
	public VecVesaMode()
	{
		this.elements = new VESAMode[DEFAULT_CAPACITY];
		this.size = 0;
	}
	
	public VecVesaMode(int initialCapacity)
	{
		if (initialCapacity < 0)
			Kernel.panic("Illegal Capacity");
		
		this.elements = new VESAMode[initialCapacity];
		this.size = 0;
	}
	
	public void add(VESAMode element)
	{
		ensureCapacity(size + 1);
		elements[size++] = element;
	}
	
	public VESAMode get(int index)
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
			
			VESAMode[] newElements = new VESAMode[newCapacity];
			
			for (int i = 0; i < size; i++)
				newElements[i] = elements[i];
			
			elements = newElements;
		}
	}
}
