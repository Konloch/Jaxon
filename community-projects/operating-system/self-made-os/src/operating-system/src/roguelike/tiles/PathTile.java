package roguelike.tiles;

import roguelike.items.Item;

public class PathTile extends Tile
{
	public static final char sym = 176;
	
	@Override
	public char getSymbol()
	{
		return PathTile.sym;
	}
	
	@Override
	public boolean isPassable()
	{
		return true;
	}
	
	@Override
	public boolean putItem(Item item)
	{
		return false;
	}
}
