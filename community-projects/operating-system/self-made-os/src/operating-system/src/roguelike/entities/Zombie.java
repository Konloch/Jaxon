package roguelike.entities;

import roguelike.Coordinate;
import roguelike.Resources;
import roguelike.tiles.Tile;

public class Zombie extends Enemy
{
	public Zombie(Coordinate coord)
	{
		super(coord, Resources.ZOMBIE_HEALTH, Resources.ZOMBIE_HEALTH, Resources.ZOMBIE_HITCHANCE, Resources.ZOMBIE_DODGECHANCE, Resources.ZOMBIE_MINDMG, Resources.ZOMBIE_MAXDMG, Resources.ZOMBIE_ENEMYNAME);
	}
	
	@Override
	public char getSymbol()
	{
		return 'Z';
	}
	
	@Override
	public void onDeath(Player p, Tile t)
	{
		return;
	}
}
