package rte;

public class DynamicRuntime
{
	public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type)
	{
		while (true);
	}
	
	public static SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		while (true);
	}
	
	public static void newMultArray(SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		while (true);
	}
	
	public static boolean isInstance(Object o, SClassDesc dest, boolean asCast)
	{
		while (true);
	}
	
	public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast)
	{
		while (true);
	}
	
	public static boolean isArray(SArray o, int stdType, Object unitType, int arrDim, boolean asCast)
	{
		while (true);
	}
	
	public static void checkArrayStore(Object dest, SArray newEntry)
	{
		while (true);
	}
}