package kernel;

import rte.SClassDesc;
import rte.SMthdBlock;
import rte.SPackage;

public class SymbolResolution
{
	private static final int BOOTLOADER_START = 0x7C00;
	private static final int BOOTLOADER_END = 0x7DFF;
	private static SClassDesc cd;
	private static SMthdBlock bootloader;
	
	static
	{
		cd = new SClassDesc();
		cd.name = "Bootloader";
		bootloader = new SMthdBlock();
		bootloader.namePar = "bootloader()";
		bootloader.nextMthd = null;
		bootloader.owner = cd;
	}
	
	@SJC.Inline
	public static SMthdBlock findMethodBlock(int addr)
	{
		if (addr >= BOOTLOADER_START && addr <= BOOTLOADER_END)
			return bootloader;
		return searchPackageRecursive(addr, SPackage.root);
	}
	
	@SJC.Inline
	private static SMthdBlock searchPackageRecursive(int addr, SPackage pack)
	{
		SMthdBlock retval = null;
		if (pack.subPacks != null)
		{
			retval = searchPackageRecursive(addr, pack.subPacks);
		}
		if (retval != null)
			return retval;
		//go through our own classes
		retval = searchClass(addr, pack.units);
		if (retval != null)
			return retval;
		if (pack.nextPack != null)
			return searchPackageRecursive(addr, pack.nextPack);
		return null;
	}
	
	@SJC.Inline
	private static SMthdBlock searchClass(int addr, SClassDesc cl)
	{
		SMthdBlock retval = null;
		while (cl != null)
		{
			//check methods of this class
			if (cl.mthds != null)
				retval = checkMethods(addr, cl.mthds);
			if (retval != null)
				return retval;
			cl = cl.nextUnit;
		}
		return null;
	}
	
	@SJC.Inline
	private static SMthdBlock checkMethods(int addr, SMthdBlock mthd)
	{
		while (mthd != null)
		{
			int startAddr = MAGIC.cast2Ref(mthd);
			int endAddr = startAddr + mthd._r_scalarSize;
			if (startAddr <= addr && addr <= endAddr)
				return mthd;
			mthd = mthd.nextMthd;
		}
		return null;
	}
}
