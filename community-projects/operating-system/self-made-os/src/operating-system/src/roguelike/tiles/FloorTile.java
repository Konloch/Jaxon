package roguelike.tiles;

import roguelike.items.Item;

public class FloorTile extends Tile
{
	private Item item;
	
	@Override
	public char getSymbol()
	{
		if (item != null)
		{
			return item.getSymbol();
		}
		return '.';
	}
	
	@Override
	public boolean isPassable()
	{
		return true;
	}
	
	@Override
	public boolean putItem(Item item)
	{
		if (this.item == null)
		{
			this.item = item;
			return true;
		}
		else
			return false;
	}
	
	public Item getItem()
	{
		return this.item;
	}
	
	public void removeItem()
	{
		this.item = null;
	}
}
