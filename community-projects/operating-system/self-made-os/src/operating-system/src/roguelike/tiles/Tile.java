package roguelike.tiles;

import roguelike.items.Item;

public abstract class Tile
{
	public abstract char getSymbol();
	
	public abstract boolean isPassable();
	
	public abstract boolean putItem(Item item);
}
