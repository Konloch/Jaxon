package java.lang;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Acts as the System implementation API to support cross-platform actions.
 *
 * @author Konloch
 * @author S. Frenz
 * @since 7/17/2024
 */
public abstract class System
{
	public static System _system;
	
	public static Out out = new Out();
	
	public static String platform;
	
	public abstract boolean isDirectory(String path);
	
	public abstract boolean doesExist(String path);
	
	public abstract boolean delete(String path);
	
	public abstract boolean createDirectory(String path);
	
	public abstract boolean rename(String oldPath, String newPath);
	
	public abstract long getSize(String path);
	
	public abstract String[] listDirectory(String path);
	
	public abstract InputStream read(String path);
	
	public abstract void write(String path, int offset, OutputStream stream, boolean append);
	
	public abstract void print(int c);
	
	public static class Out
	{
		public void printChar(int c)
		{
			_system.print(c);
		}
		
		public void print(int i)
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
		
		public void printHex(int i)
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
		
		public void printHexByte(int b)
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
		
		public void printHexLong(long l)
		{
			printHex((int) (l >>> 32));
			printHex((int) l);
		}
		
		public void printBZ(byte[] bzStr)
		{
			int i;
			
			for (i = 0; i < bzStr.length && bzStr[i] != 0; i++)
				printChar(bzStr[i]);
		}
		
		public void print(String s)
		{
			int i;
			
			for (i = 0; i < s.count; i++)
				printChar(s.value[i]);
		}
		
		public void print(StringBuilder s)
		{
			print(s.toString());
		}
		
		public void println()
		{
			printChar(10);
		}
		
		public void println(String s)
		{
			print(s);
			printChar(10);
		}
	}
}
