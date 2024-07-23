package container;

public class List
{
	private Element first;
	
	private int size = 0;
	
	public void append(Object value)
	{
		Element element = new Element();
		element.value = value;
		if (this.first == null)
		{
			this.first = element;
		}
		else
		{
			Element last = this.first;
			while (last.next != null)
			{
				last = last.next;
			}
			last.next = element;
		}
		this.size++;
	}
	
	public void insert(int index, Object value)
	{
		Element element = new Element();
		element.value = value;
		if (this.first == null)
		{
			this.first = element;
		}
		else
		{
			Element current = this.first;
			while (current.next != null && --index > 0)
				current = current.next;
			element.next = current.next;
			current.next = element;
		}
		this.size++;
	}
	
	public Object get(int index)
	{
		if (index >= 0 && index < this.size)
		{
			Element current = this.first;
			while (index-- > 0)
				current = current.next;
			return current.value;
		}
		return null;
	}
	
	public void remove(int index)
	{
		if (index >= 0 && index < this.size)
		{
			if (index == 0)
			{
				this.first = this.first.next;
			}
			else
			{
				Element current = this.first;
				while (--index > 0)
					current = current.next;
				current.next = current.next.next;
			}
			this.size--;
		}
	}
	
	private static class Element
	{
		
		public Object value;
		
		public Element next;
	}
}
