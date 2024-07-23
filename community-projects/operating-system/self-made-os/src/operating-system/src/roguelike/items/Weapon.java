package roguelike.items;

public abstract class Weapon extends Equipable
{
	int hitChance, blockChance, parryChance;
	int minDamage, maxDamage;
	
	public Weapon(String name, int hitChance, int blockChance, int parryChance, int minDamage, int maxDamage)
	{
		super(name);
		this.hitChance = hitChance;
		this.blockChance = blockChance;
		this.parryChance = parryChance;
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
	}
	
	@Override
	public char getSymbol()
	{
		return 234;
	}
	
	public int getHitChance()
	{
		return hitChance;
	}
	
	public int getBlockChance()
	{
		return blockChance;
	}
	
	public int getParryChance()
	{
		return parryChance;
	}
	
	public int getMinDamage()
	{
		return minDamage;
	}
	
	public int getMaxDamage()
	{
		return maxDamage;
	}
}
