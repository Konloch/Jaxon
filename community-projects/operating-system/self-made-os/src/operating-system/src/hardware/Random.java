package hardware;

public class Random
{
	private static int staticSeed;
	private static int dynamicSeed;
	private static final int MAX_RAND_BEFORE_RESEED = 50;
	private static int randUntilReseed = MAX_RAND_BEFORE_RESEED;
	
	static
	{
		staticSeed = (int) (Time.readRTC() + Time.readTSC());
		dynamicSeed = 1;
	}
	
	public static void srand(int seed)
	{
		dynamicSeed = seed;
	}
	
	public static int rand()
	{
		int retval = dynamicSeed = dynamicSeed + staticSeed * 1103515245 + 12345;
		randUntilReseed--;
		if (randUntilReseed == 0)
		{
			//reseed the dynamic seed, static seed stays the same
			dynamicSeed = (int) ((dynamicSeed + staticSeed * 1103515245) * Time.getSystemTime() * ((int) Time.readRTC() + Time.readTSC()) + 12345);
			randUntilReseed = MAX_RAND_BEFORE_RESEED;
		}
		return retval;
	}
	
	public static int rand(int max)
	{
		return rand() % max;
	}
	
	//returns number between min and max inclusive. always returns 0 if min is greater than max
	public static int rand(int min, int max)
	{
		if (min > max)
			return 0;
		return unsignedRand() % (max - min + 1) + min;
	}
	
	//returns a random positive number
	public static int unsignedRand()
	{
		int retval = rand();
		if (retval < 0)
			retval *= -1;
		else if (retval == -2147483648)
			retval++;
		return retval;
	}
	
	public static int unsignedRand(int max)
	{
		return rand(0, max);
	}
	
}
