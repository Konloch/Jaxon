package roguelike.entities;

import roguelike.Coordinate;

public abstract class Entity
{
	//position
	protected Coordinate coord;
	//player stats
	protected int health;
	protected int maxHealth;
	protected Coordinate lastCoord;
	
	public Entity(Coordinate coord, int health, int maxHealth)
	{
		this.coord = coord;
		this.health = health;
		this.maxHealth = maxHealth;
	}
	
	public int getHealth()
	{
		return health;
	}
	
	public int getMaxHealth()
	{
		return maxHealth;
	}
	
	public Coordinate getCoord()
	{
		return coord;
	}
	
	public void setCoord(Coordinate coord)
	{
		lastCoord = this.coord;
		this.coord = coord;
	}
	
	public Coordinate getLastCoord()
	{
		return lastCoord;
	}
	
	//see Resources for direction constants
	public void move(Coordinate newCoord)
	{
		lastCoord = coord;
		coord = newCoord;
	}
	
	public abstract char getSymbol();
	
	public void setHealth(int health)
	{
		this.health = health;
	}
}
