package roguelike.items;

public abstract class Item
{
	public final String name;
	
	public abstract char getSymbol();
	
	public Item(String name)
	{
		this.name = name;
	}
}
