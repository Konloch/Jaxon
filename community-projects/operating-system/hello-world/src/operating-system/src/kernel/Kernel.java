package kernel;

import video.VideoChar;
import video.VideoMemory;

public class Kernel
{
	public static void main()
	{
		clear();
		print("Hello World!!!");
		while (true)
		{
		
		}
	}
	
	/**
	 * Deletes all characters on the screen.
	 */
	public static void clear()
	{
		for (VideoChar vidChar : VideoMemory.std.chars)
		{
			vidChar.ascii = 32; // Leerzeichen
			vidChar.color = 0;
		}
	}
	
	/**
	 * Outputs a character string on the screen. The output starts at the
	 * beginning of the screen.
	 *
	 * @param str The character string to be output.
	 */
	public static void print(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			VideoMemory.std.chars[i].ascii = (byte) str.charAt(i);
			VideoMemory.std.chars[i].color = (byte) (i % 5 + 10); // Colors 2, 3, 4, 5 and 6 in light
		}
	}
}
