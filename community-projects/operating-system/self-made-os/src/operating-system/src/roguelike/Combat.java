package roguelike;

import hardware.Random;
import roguelike.entities.Enemy;
import roguelike.entities.Player;
import roguelike.items.Weapon;

public class Combat
{
	public static final int ENEMY_DIED = 1;
	public static final int PLAYER_DIED = -1;
	public static final int NOBODY_DIED = 0;
	
	//returns 1 if enemy died, -1 if player died, 0 if neither died
	static int doMeleeCombat(Player p, Enemy e, MessageStatPrinter messages)
	{
		//player always hits first, so roll if player will hit
		//get player weapon
		Weapon w = p.getEquippedWeapon();
		int pHit = Random.rand(1, 100);
		//rolling a one is a critical hit
		boolean pCriticalHit = pHit == 1;
		if (pHit < w.getHitChance())
		{
			//player hits, roll if enemy dodges
			int eEvade = Random.rand(1, 100);
			if (eEvade >= e.getDodgeChance())
			{
				//player hit, enemy doesn't dodge, handle damage
				handlePlayerMeleeHit(p, e, w, pCriticalHit, messages);
				//check if enemy is dead, otherwise continue with enemy attack
				if (e.getHealth() <= 0)
					return ENEMY_DIED;
			}
			else
			{
				StringBuilder sb = new StringBuilder();
				sb.append(e.getName());
				sb.append(" dodged your attack.");
				messages.queueMessage(sb.getString());
			}
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("You miss ");
			sb.append(e.getName());
			messages.queueMessage(sb.getString());
		}
		
		//enemy rolls for hit
		int eHit = Random.rand(1, 100);
		boolean eCriticalHit = eHit == 1;
		if (eHit < e.getHitChance())
		{
			//enemy hits, roll if player parries (no player damage, reflect half damage)
			int pParry = Random.rand(1, 100);
			if (pParry < w.getParryChance())
			{
				handlePlayerParry(e, messages);
				//check if enemy dead, otherwise end combat
				if (e.getHealth() <= 0)
					return ENEMY_DIED;
				return NOBODY_DIED;
			}
			//player didn't parry, roll if player blocks (player takes half damage)
			int pBlock = Random.rand(1, 100);
			handleEnemyHit(p, e, pBlock < w.getBlockChance(), messages);
			if (p.getHealth() <= 0)
				return PLAYER_DIED;
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append(e.getName());
			sb.append(" misses you.");
			messages.queueMessage(sb.getString());
		}
		return NOBODY_DIED;
	}
	
	//player hits enemy
	static void handlePlayerMeleeHit(Player p, Enemy e, Weapon w, boolean critical, MessageStatPrinter messages)
	{
		//roll damage
		int damage = Random.rand(w.getMinDamage(), w.getMaxDamage());
		//add player attack power
		damage += p.getAttackPower();
		if (critical)
			damage *= 2;
		//update enemy health
		e.setHealth(e.getHealth() - damage);
		StringBuilder sb = new StringBuilder();
		sb.append("You hit ");
		sb.append(e.getName());
		sb.append(" for ");
		sb.append(damage);
		sb.append(" damage.");
		messages.queueMessage(sb.getString());
	}
	
	//player parries enemy hit
	static void handlePlayerParry(Enemy e, MessageStatPrinter messages)
	{
		//roll enemy damage
		int damage = Random.rand(e.getMinDamage(), e.getMaxDamage()) / 2;
		e.setHealth(e.getHealth() - damage);
		StringBuilder sb = new StringBuilder();
		sb.append("You parry ");
		sb.append(e.getName());
		sb.append(" for ");
		sb.append(damage);
		sb.append(" damage.");
		messages.queueMessage(sb.getString());
	}
	
	//enemy hits player
	static void handleEnemyHit(Player p, Enemy e, boolean blocked, MessageStatPrinter messages)
	{
		//roll enemy damage
		int damage = Random.rand(e.getMinDamage(), e.getMaxDamage());
		//subtract player block power
		damage -= p.getDefensePower();
		if (blocked)
			damage /= 2;
		//ensure the number is positive, we can't be healing for negative damage
		if (damage < 0)
			damage = 0;
		p.setHealth(p.getHealth() - damage);
		StringBuilder sb = new StringBuilder();
		sb.append(e.getName());
		sb.append(" hits you for ");
		sb.append(damage);
		sb.append(" damage.");
		messages.queueMessage(sb.getString());
	}
}
