package roguelike.items;

import roguelike.MessageStatPrinter;
import roguelike.entities.Player;

public abstract class Consumable extends Item
{
	public Consumable(String name)
	{
		super(name);
	}
	
	public abstract void onUse(Player p, MessageStatPrinter msp);
}
