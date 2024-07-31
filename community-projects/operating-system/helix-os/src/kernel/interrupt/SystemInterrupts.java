package kernel.interrupt;

import arch.x86;
import kernel.memory.VirtualMemory;
import kernel.trace.Bluescreen;

public class SystemInterrupts
{
	@SJC.Interrupt
	public static void ignoreHandler()
	{
	
	}
	
	@SJC.Interrupt
	public static void divByZeroHandler()
	{
		x86.breakpoint();
	}
	
	@SJC.Interrupt
	public static void debugHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt debugHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void nmiHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt nmiHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void breakpointHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		
		// Read registers from stack (pushed by x86.interrupt())
		int edi = MAGIC.rMem32(ebp + 4);
		int esi = MAGIC.rMem32(ebp + 8);
		int ebp2 = MAGIC.rMem32(ebp + 12);
		int esp = MAGIC.rMem32(ebp + 16);
		int ebx = MAGIC.rMem32(ebp + 20);
		int edx = MAGIC.rMem32(ebp + 24);
		int ecx = MAGIC.rMem32(ebp + 28);
		int eax = MAGIC.rMem32(ebp + 32);
		
		// Read old EIP from stack
		// 4 bytes before first pushed register + parameters
		int oldEip = x86.eipForInterrupt(ebp, 0);
		Bluescreen.show("Breakpoint", "Breakpoint hit", ebp, oldEip, edi, esi, ebp2, esp, ebx, edx, ecx, eax);
		while (true);
	}
	
	@SJC.Interrupt
	public static void overflowHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt overflowHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void boundRangeExceededHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt boundRangeExceededHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void invalidOpcodeHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt invalidOpcodeHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void reservedHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt reservedHandler", ebp, x86.eipForInterrupt(ebp, 0));
		while (true);
	}
	
	@SJC.Interrupt
	public static void doubleFaultHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt doubleFaultHandler", ebp, x86.eipForInterrupt(ebp, 1));
		while (true);
	}
	
	@SJC.Interrupt
	public static void generalProtectionFaultHandler()
	{
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		Bluescreen.show("PANIC", "Interrupt generalProtectionFaultHandler", ebp, x86.eipForInterrupt(ebp, 1));
		while (true);
	}
	
	@SJC.Interrupt
	public static void pageFaultHandler()
	{
		
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp);
		int cr2 = VirtualMemory.getCR2();
		int eip = x86.eipForInterrupt(ebp, 1);
		Bluescreen.show("PANIC", "Page fault at address: ".append(Integer.toString(cr2)), ebp, eip);
		while (true);
	}
}
