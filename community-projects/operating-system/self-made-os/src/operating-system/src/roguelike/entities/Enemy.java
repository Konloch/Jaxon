package roguelike.entities;

import roguelike.Coordinate;
import roguelike.tiles.Tile;

public abstract class Enemy extends Entity
{
	//0-100
	private final int hitChance;
	private final int dodgeChance;
	private final int minDamage;
	private final int maxDamage;
	private final String name;
	
	public Enemy(Coordinate coord, int health, int maxHealth, int hitChance, int dodgeChance, int minDamage, int maxDamage, String name)
	{
		super(coord, health, maxHealth);
		this.hitChance = hitChance;
		this.dodgeChance = dodgeChance;
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
		this.name = name;
	}
	
	public int getHitChance()
	{
		return hitChance;
	}
	
	public int getDodgeChance()
	{
		return dodgeChance;
	}
	
	public int getMinDamage()
	{
		return minDamage;
	}
	
	public int getMaxDamage()
	{
		return maxDamage;
	}
	
	//special on death mechanics like dropping items or winning the game
	public abstract void onDeath(Player p, Tile t);
	
	public String getName()
	{
		return name;
	}
}
