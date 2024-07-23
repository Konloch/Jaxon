package roguelike;

public class Resources
{
	static final int MAX_PLAYFIELD_HEIGHT = 20;
	static final int MAX_PLAYFIELD_WIDTH = 80;
	
	static final int MAX_FLOORFACTORY_ROOMTRIES = 200;
	static final int MAX_FLOORFACTORY_ROOMS = 10;
	
	public static final int MAX_ENEMY_COUNT_PER_FLOOR = 10;
	
	//default player stats
	public static final int defaultPlayerHealth = 20;
	public static final int defaultStr = 10;
	public static final int defaultDef = 10;
	public static final int defaultInt = 10;
	static final int defaultPlayerItemCapacity = 20;
	
	//zombie
	public static final int ZOMBIE_HEALTH = 10;
	public static final int ZOMBIE_MINDMG = 4;
	public static final int ZOMBIE_MAXDMG = 5;
	public static final int ZOMBIE_HITCHANCE = 65;
	public static final int ZOMBIE_DODGECHANCE = 30;
	public static final String ZOMBIE_ENEMYNAME = "Zombie";
	
	//final endboss
	public static final int ENDBOSS_HEALTH = 150;
	public static final int ENDBOSS_MINDMG = 10;
	public static final int ENDBOSS_MAXDMG = 20;
	public static final int ENDBOSS_HITCHANCE = 95;
	public static final int ENDBOSS_DODGECHANCE = 40;
	public static final String ENDBOSS_ENEMYNAME = "Mephistopheles Of The Hell";
	
	//claymore
	public static final int CLAYMORE_HITCHANCE = 75;
	public static final int CLAYMORE_BLOCKCHANCE = 0;
	public static final int CLAYMORE_PARRYCHANCE = 15;
	public static final int CLAYMORE_MINDMG = 3;
	public static final int CLAYMORE_MAXDMG = 5;
	public static final String CLAYMORE_NAME = "Iron Claymore";
	
	//excalibur
	public static final int EXCALIBUR_HITCHANCE = 100;
	public static final int EXCALIBUR_BLOCKCHANCE = 75;
	public static final int EXCALIBUR_PARRYCHANCE = 25;
	public static final int EXCALIBUR_MINDMG = 25;
	public static final int EXCALIBUR_MAXDMG = 50;
	public static final int EXCALIBUR_ADDEDMAXHEALTH = 25;
	public static final String EXCALIBUR_NAME = "Excalibur, Legendary Sword Of King Arthur";
	
	//health potion
	public static final String HEALTHPOTION_NAME = "Health Potion";
	public static final int HEALTHPOTION_DHEALTH = 20;
	//directions
	static final int DIR_UP = 1;
	static final int DIR_RIGHT = 2;
	static final int DIR_DOWN = 3;
	static final int DIR_LEFT = 4;
	
	//basic floor for testing
	static String returnBasicFloor()
	{
		char[] val = new char[1600];
		int index = 0;
		for (; index < 80; index++)
		{
			val[index] = 32;
		}
		for (; index < 160; index++)
		{
			val[index] = 35;
		}
		for (; index < 1520; index++)
		{
			if (index % 80 == 0 || index % 80 == 79)
			{
				val[index] = 35;
			}
			else if (index == 1422)
			{
				val[index] = 178;
			}
			else
			{
				val[index] = 46;
			}
		}
		for (; index < 1600; index++)
		{
			val[index] = 35;
		}
		return new String(val);
	}
}
