package kernel.memory;

import java.lang.StringBuilder;

public class Memory
{
	public static void Memset(int start, int len, byte value)
	{
		int end = start + len;
		if (len % 4 == 0)
		{
			Memset32(start, len / 4, value);
		}
		else
		{
			for (int i = start; i < end; i++)
			{
				MAGIC.wMem8(i, value);
			}
		}
	}
	
	/*
	 * Set 32-bit words in memory to a specific value.
	 * When changing variable order or layout, make sure to update the
	 * corresponding inline assembly as well.
	 *
	 * Cannot be inlined since it has fixed offsets to function argument pointers.
	 */
	@SJC.NoInline
	public static void Memset32(int start, int numberOf32BitBlocks, int value)
	{
		MAGIC.inlineBlock("memset32");
	}
	
	/*
	 * Copy bytes from one memory location to another.
	 */
	public static void Memcopy(int from, int to, int len)
	{
		if (len % 4 == 0)
		{
			Memcopy32(from, to, len / 4);
		}
		else
		{
			while (len > 0)
			{
				MAGIC.wMem8(to, MAGIC.rMem8(from));
				from++;
				to++;
				len--;
			}
		}
	}
	
	/*
	 * Copy 32-bit words from one memory location to another.
	 * When changing variable order or layout, make sure to update the
	 * corresponding inline assembly as well.
	 *
	 * Cannot be inlined since it has fixed offsets to function argument pointers.
	 */
	@SJC.NoInline
	public static void Memcopy32(int from, int to, int cnt)
	{
		MAGIC.inlineBlock("memcopy32");
	}
	
	public static String FormatBytes(int bytes)
	{
		StringBuilder sb = new StringBuilder();
		if (bytes < 1024)
		{
			sb.append(bytes).append(" B");
		}
		else if (bytes < 1024 * 1024)
		{
			sb.append(bytes / 1024).append(" KB");
		}
		else if (bytes < 1024 * 1024 * 1024)
		{
			sb.append(bytes / 1024 / 1024).append(" MB");
		}
		else if (bytes < 1024 * 1024 * 1024 * 1024)
		{
			sb.append(bytes / 1024 / 1024 / 1024).append(" GB");
		}
		else
		{
			sb.append(bytes / 1024 / 1024 / 1024 / 1024).append(" TB");
		}
		return sb.toString();
	}
	
	public static String FormatBytesToKb(int bytes)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(bytes / 1024).append(" KB");
		return sb.toString();
	}
}
