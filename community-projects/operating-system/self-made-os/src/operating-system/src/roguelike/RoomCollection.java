package roguelike;


//double linked list for rooms
public class RoomCollection
{
	
	
	private class RoomWrapper
	{
		Room room;
		RoomWrapper prev, next;
		
		RoomWrapper(Room room)
		{
			this.room = room;
		}
		
		RoomWrapper(Room room, RoomWrapper prev, RoomWrapper next)
		{
			this.room = room;
			this.prev = prev;
			this.next = next;
		}
	}
	
	private RoomWrapper first, last;
	private int count = 0;
	
	//adds room to the collection, returns true if successful, false if collection is full (max enemies)
	public boolean append(Room room)
	{
		if (first == null)
		{
			//sanity check
			if (last == null)
			{
				first = last = new RoomWrapper(room);
				count++;
				return true;
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
		last.next = new RoomWrapper(room, last, null);
		last = last.next;
		count++;
		return true;
	}
	
	public void delete(Room room)
	{
		RoomWrapper i = first;
		while (i != null)
		{
			if (i.room == room)
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
	
	public Room getEnemyAtIndex(int index)
	{
		if (index > count)
			return null;
		RoomWrapper retval = first;
		for (int i = 1; i < index; i++)
		{
			retval = retval.next;
		}
		return retval.room;
	}
	
	public Room[] getRooms()
	{
		Room[] enemies = new Room[count];
		RoomWrapper roomWrapper = first;
		int i = 0;
		while (roomWrapper != null)
		{
			enemies[i] = roomWrapper.room;
			i++;
			roomWrapper = roomWrapper.next;
		}
		return enemies;
	}
	
	public int getCount()
	{
		return count;
	}
}
