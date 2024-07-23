package roguelike.tiles;

import roguelike.items.Item;

public class WallTile extends Tile
{
	
	@Override
	public char getSymbol()
	{
		return '#';
	}
	
	@Override
	public boolean isPassable()
	{
		return false;
	}
	
	@Override
	public boolean putItem(Item item)
	{
		return false;
	}
}
