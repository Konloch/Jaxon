package roguelike.items;

import roguelike.MessageStatPrinter;
import roguelike.Resources;
import roguelike.entities.Player;

public class HealingPotion extends Consumable
{
	
	public HealingPotion()
	{
		super(Resources.HEALTHPOTION_NAME);
	}
	
	@Override
	public void onUse(Player p, MessageStatPrinter msp)
	{
		p.addHealth(Resources.HEALTHPOTION_DHEALTH);
		StringBuilder sb = new StringBuilder("Healed for ");
		sb.append(Resources.HEALTHPOTION_DHEALTH);
		msp.queueMessage(sb.getString());
	}
	
	@Override
	public char getSymbol()
	{
		return '*';
	}
}
