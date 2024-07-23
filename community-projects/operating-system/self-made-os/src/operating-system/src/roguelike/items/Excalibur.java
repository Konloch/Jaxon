package roguelike.items;

import roguelike.MessageStatPrinter;
import roguelike.Resources;
import roguelike.entities.Player;

public class Excalibur extends Weapon
{
	public Excalibur()
	{
		super(Resources.EXCALIBUR_NAME, Resources.EXCALIBUR_HITCHANCE, Resources.EXCALIBUR_BLOCKCHANCE, Resources.EXCALIBUR_PARRYCHANCE, Resources.EXCALIBUR_MINDMG, Resources.EXCALIBUR_MAXDMG);
	}
	
	@Override
	public void onEquip(Player p, MessageStatPrinter msp)
	{
		p.changeMaxHealth(Resources.EXCALIBUR_ADDEDMAXHEALTH);
	}
	
	@Override
	public void onUnequip(Player p, MessageStatPrinter msp)
	{
		p.changeMaxHealth(-Resources.EXCALIBUR_ADDEDMAXHEALTH);
	}
}
