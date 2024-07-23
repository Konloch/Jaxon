package roguelike.entities;

import roguelike.Resources;

//double linked list of items to make removal and insertion easy
//TODO: test this class
public class EnemyCollection
{
	private class EnemyWrapper
	{
		Enemy enemy;
		EnemyWrapper prev, next;
		
		EnemyWrapper(Enemy enemy)
		{
			this.enemy = enemy;
		}
		
		EnemyWrapper(Enemy enemy, EnemyWrapper prev, EnemyWrapper next)
		{
			this.enemy = enemy;
			this.prev = prev;
			this.next = next;
		}
	}
	
	private EnemyWrapper first, last;
	private int count = 0;
	
	//adds enemy to the collection, returns true if successful, false if collection is full (max enemies)
	public boolean append(Enemy enemy)
	{
		int enemyMax = Resources.MAX_ENEMY_COUNT_PER_FLOOR;
		if (count == enemyMax)
			return false;
		if (first == null)
		{
			//sanity check
			if (last == null)
			{
				first = last = new EnemyWrapper(enemy);
				count++;
				return true;
			}
			else
			{ //we fucked something up majorly
				MAGIC.inline(0xCC);
			}
		}
		//sanity check
		if (last.next != null)
		{
			MAGIC.inline(0xCC);
		}
		last.next = new EnemyWrapper(enemy, last, null);
		last = last.next;
		count++;
		return true;
	}
	
	public void delete(Enemy enemy)
	{
		EnemyWrapper i = first;
		while (i != null)
		{
			if (i.enemy == enemy)
			{
				if (i.prev != null)
				{
					i.prev.next = i.next;
				}
				if (i.next != null)
				{
					i.next.prev = i.prev;
				}
				if (first == i)
				{
					first = i.next;
				}
				if (last == i)
				{
					last = i.prev;
				}
				count--;
				return;
			}
			i = i.next;
		}
	}
	
	public Enemy getEnemyAtIndex(int index)
	{
		if (index > count)
			return null;
		EnemyWrapper retval = first;
		for (int i = 1; i < index; i++)
		{
			retval = retval.next;
		}
		return retval.enemy;
	}
	
	public Enemy[] getEnemies()
	{
		Enemy[] enemies = new Enemy[count];
		EnemyWrapper enemyWrapper = first;
		int i = 0;
		while (enemyWrapper != null)
		{
			enemies[i] = enemyWrapper.enemy;
			i++;
			enemyWrapper = enemyWrapper.next;
		}
		return enemies;
	}
}
