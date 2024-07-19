package kernel.windows;

public class WinSystem extends System
{
	@Override
	public void doPrintChar(int c)
	{
		MAGIC.inline(0x6A, 0x00);                            //push byte 0 (no overlap)
		MAGIC.inline(0x8D, 0x45, 0xFC);                      //lea eax,[ebp-4] (address of result)
		MAGIC.inline(0x50);                                  //push eax
		MAGIC.inline(0x6A, 0x01);                            //push byte 1 (single character)
		MAGIC.inline(0x8D, 0x45, 0x08);                      //lea eax,[ebp+8] (address of string)
		MAGIC.inline(0x50);                                  //push eax
		MAGIC.inline(0xFF, 0x35); MAGIC.inline32(rte.DynamicRuntime._hndStdOut); //push handle
		MAGIC.inline(0xFF, 0x15); MAGIC.inline32(rte.DynamicRuntime._Kernel_WriteFile); //call
	}
}
