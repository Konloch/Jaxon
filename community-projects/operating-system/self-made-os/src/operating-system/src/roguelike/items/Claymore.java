package roguelike.items;

import roguelike.MessageStatPrinter;
import roguelike.Resources;
import roguelike.entities.Player;

public class Claymore extends Weapon
{
	public Claymore()
	{
		super(Resources.CLAYMORE_NAME, Resources.CLAYMORE_HITCHANCE, Resources.CLAYMORE_BLOCKCHANCE, Resources.CLAYMORE_PARRYCHANCE, Resources.CLAYMORE_MINDMG, Resources.CLAYMORE_MAXDMG);
	}
	
	@Override
	public void onEquip(Player p, MessageStatPrinter msp)
	{
	
	}
	
	@Override
	public void onUnequip(Player p, MessageStatPrinter msp)
	{
	
	}
}
