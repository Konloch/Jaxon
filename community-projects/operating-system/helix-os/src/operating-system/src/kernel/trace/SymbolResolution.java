package kernel.trace;

import kernel.MemoryLayout;
import rte.SClassDesc;
import rte.SMthdBlock;
import rte.SPackage;

public class SymbolResolution
{
	private static SClassDesc bootloaderClassDesc;
	private static SMthdBlock bootloaderMethod;
	
	public static void initialize()
	{
		bootloaderClassDesc = new SClassDesc();
		bootloaderClassDesc.name = "Bootloader";
		bootloaderMethod = new SMthdBlock();
		bootloaderMethod.namePar = "bootloader()";
		bootloaderMethod.owner = bootloaderClassDesc;
		bootloaderMethod.nextMthd = null;
	}
	
	public static SMthdBlock resolve(int addr)
	{
		if (MemoryLayout.BOOTLOADER_START <= addr && addr <= MemoryLayout.BOOTLOADER_END)
			return bootloaderMethod;
		
		return resolveInPackage(addr, SPackage.root);
	}
	
	private static SMthdBlock resolveInPackage(int addr, SPackage pkg)
	{
		while (pkg != null)
		{
			SMthdBlock found = resolveInPackage(addr, pkg.subPacks);
			if (found != null)
				return found;
			
			found = resolveInClass(addr, pkg.units);
			
			if (found != null)
				return found;
			
			pkg = pkg.nextPack;
		}
		
		return null;
	}
	
	private static SMthdBlock resolveInClass(int addr, SClassDesc cls)
	{
		while (cls != null)
		{
			SMthdBlock found = resolveInMethodBlock(addr, cls.mthds);
			
			if (found != null)
				return found;
			
			cls = cls.nextUnit;
		}
		
		return null;
	}
	
	private static SMthdBlock resolveInMethodBlock(int addr, SMthdBlock mths)
	{
		while (mths != null)
		{
			int start = MAGIC.cast2Ref(mths);
			int end = start + mths._r_scalarSize;
			
			if (start <= addr && addr <= end)
				return mths;
			
			mths = mths.nextMthd;
		}
		
		return null;
	}
}
