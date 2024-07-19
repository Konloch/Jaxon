package java.lang;

/**
 * @author Konloch
 * @author S. Frenz
 * @since 7/17/2024
 */
public abstract class System
{
	public static System _system;
	
	public abstract boolean isDirectory(String path);
	
	public abstract boolean doesExist(String path);
	
	public abstract boolean delete(String path);
	
	public abstract boolean createDirectory(String path);
	
	public abstract boolean rename(String oldPath, String newPath);
	
	public abstract long getSize(String path);
	
	public abstract String[] listDirectory(String path);
	
	public abstract byte[] read(String path);
	
	public abstract void write(String path, int offset, byte[] bytes, int length, boolean append);
	
	public abstract void printCharacter(int c);
	
	public static void printChar(int c)
	{
		_system.printCharacter(c);
	}
	
	public static void print(int i)
	{
		if (i < 0)
		{
			printChar(45);
			i = -i;
		}
		
		if (i == 0)
			printChar(48);
		
		else
		{
			if (i >= 10)
				print(i / 10);
			printChar(48 + i % 10);
		}
	}
	
	public static void printHex(int i)
	{
		int v, p;
		
		for (p = 0; p < 8; p++)
		{
			v = (i >>> ((7 - p) << 2)) & 0xF;
			if (v < 10)
				printChar(48 + v); //'0'..'9'
			else
				printChar(55 + v); //'A'..'Z'
		}
	}
	
	public static void printHexByte(int b)
	{
		int v, p;
		
		for (p = 0; p < 2; p++)
		{
			v = (b >>> ((1 - p) << 2)) & 0xF;
			if (v < 10)
				printChar(48 + v); //'0'..'9'
			else
				printChar(55 + v); //'A'..'Z'
		}
	}
	
	public static void printHexLong(long l)
	{
		printHex((int) (l >>> 32));
		printHex((int) l);
	}
	
	public static void printBZ(byte[] bzStr)
	{
		int i;
		
		for (i = 0; i < bzStr.length && bzStr[i] != 0; i++)
			printChar((int) bzStr[i]);
	}
	
	public static void print(String s)
	{
		int i;
		
		for (i = 0; i < s.count; i++)
			printChar((int) s.value[i]);
	}
	
	public static void println()
	{
		printChar(10);
	}
	
	public static void println(String s)
	{
		print(s);
		printChar(10);
	}
}
