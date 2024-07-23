package roguelike.items;

import roguelike.MessageStatPrinter;
import roguelike.entities.Player;

public abstract class Equipable extends Item
{
	public Equipable(String name)
	{
		super(name);
	}
	
	//execute special modifications, such as in(/de)creasing stats or healing the player
	abstract public void onEquip(Player p, MessageStatPrinter msp);
	
	abstract public void onUnequip(Player p, MessageStatPrinter msp);
}
