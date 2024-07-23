package roguelike.entities;

import roguelike.Coordinate;
import roguelike.Resources;
import roguelike.tiles.Tile;

public class FinalBoss extends Enemy
{
	public FinalBoss(Coordinate coord)
	{
		super(coord, Resources.ENDBOSS_HEALTH, Resources.ENDBOSS_HEALTH, Resources.ENDBOSS_HITCHANCE, Resources.ENDBOSS_DODGECHANCE, Resources.ENDBOSS_MINDMG, Resources.ENDBOSS_MAXDMG, Resources.ENDBOSS_ENEMYNAME);
	}
	
	@Override
	public void onDeath(Player p, Tile t)
	{
		p.setWon();
	}
	
	@Override
	public char getSymbol()
	{
		return 233;
	}
}
