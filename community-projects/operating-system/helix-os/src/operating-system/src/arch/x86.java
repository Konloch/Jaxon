package arch;

public class x86
{
	/*
	 * Enable Interrupt Flag
	 */
	@SJC.Inline
	public static void sti()
	{
		MAGIC.inline(0xFB);
	}
	
	/*
	 * Clear Interrupt Flag
	 */
	@SJC.Inline
	public static void cli()
	{
		MAGIC.inline(0xFA);
	}
	
	/*
	 * Load Interrupt Descriptor Table
	 */
	@SJC.Inline
	public static void ldit(int baseAddress, int tableLimit)
	{
		long tmp = (((long) baseAddress) << 16) | (long) tableLimit;
		MAGIC.inline(0x0F, 0x01, 0x5D);
		MAGIC.inlineOffset(1, tmp); // lidt [ebp-0x08/tmp]
	}
	
	/*
	 * Breakpoint
	 */
	@SJC.Inline
	public static void breakpoint()
	{
		MAGIC.inline(0xCC);
	}
	
	/*
	 * Halt
	 */
	@SJC.Inline
	public static void hlt()
	{
		MAGIC.inline(0xF4);
	}
	
	@SJC.Inline
	public static int eipForFunction(int ebp)
	{
		return MAGIC.rMem32(ebp + 4);
	}
	
	@SJC.Inline
	public static int eipForInterrupt(int ebp, int numOfParams)
	{
		return MAGIC.rMem32(ebp + 4 * 9 + numOfParams);
	}
}
