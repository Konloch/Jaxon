package java.lang;

public class System
{
	public static long rdtsc()
	{
		long res = 0l;
		MAGIC.inline(0x0F, 0x31); //rdtsc
		MAGIC.inline(0x89, 0x55);
		MAGIC.inlineOffset(1, res, 4);
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, res, 0);
		return res;
	}
}
