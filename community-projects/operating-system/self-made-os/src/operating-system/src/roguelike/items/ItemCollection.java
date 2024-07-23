package roguelike.items;

//double linked list of items to make removal and insertion easy
//TODO: test this class
public class ItemCollection
{
	private class ItemWrapper
	{
		Item item;
		ItemWrapper prev, next;
		
		ItemWrapper(Item item)
		{
			this.item = item;
		}
		
		ItemWrapper(Item item, ItemWrapper prev, ItemWrapper next)
		{
			this.item = item;
			this.prev = prev;
			this.next = next;
		}
	}
	
	private ItemWrapper first, last;
	private int count;
	
	public void append(Item item)
	{
		if (first == null)
		{
			//sanity check
			if (last == null)
			{
				first = last = new ItemWrapper(item);
				count++;
				return;
			}
			else
			{ //we fucked something up majorly
				MAGIC.inline(0xCC);
			}
		}
		//sanity check
		if (last.next != null)
		{
			MAGIC.inline(0xCC);
		}
		last.next = new ItemWrapper(item, last, null);
		last = last.next;
		count++;
	}
	
	public void delete(Item item)
	{
		ItemWrapper i = first;
		while (i != null)
		{
			if (i.item == item)
			{
				if (i.prev != null)
				{
					i.prev.next = i.next;
				}
				if (i.next != null)
				{
					i.next.prev = i.prev;
				}
				if (first == i)
				{
					first = i.next;
				}
				if (last == i)
				{
					last = i.prev;
				}
				count--;
				return;
			}
			i = i.next;
		}
	}
	
	public Item getItemAtIndex(int index)
	{
		if (index > count)
			return null;
		ItemWrapper retval = first;
		for (int i = 1; i < index; i++)
		{
			retval = retval.next;
		}
		return retval.item;
	}
	
	public Item[] getItems()
	{
		Item[] items = new Item[count];
		ItemWrapper itemWrapper = first;
		int i = 0;
		while (itemWrapper != null)
		{
			items[i] = itemWrapper.item;
			i++;
			itemWrapper = itemWrapper.next;
		}
		return items;
	}
}
