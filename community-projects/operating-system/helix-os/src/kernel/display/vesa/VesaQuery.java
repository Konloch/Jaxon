package kernel.display.vesa;

import kernel.MemoryLayout;
import kernel.bios.BIOS;
import kernel.display.vesa.layout.VESAControllerInfoStruct;
import kernel.display.vesa.layout.VESAModeInfoStruct;
import java.lang.StringBuilder;
import java.util.vector.VecInt;
import java.util.vector.VecVesaMode;

public class VesaQuery
{
	private final static VESAControllerInfoStruct contrInfo = (VESAControllerInfoStruct) MAGIC.cast2Struct(MemoryLayout.BIOS_BUFFER_MEMMAP_START);
	private final static VESAModeInfoStruct modeInfo = (VESAModeInfoStruct) MAGIC.cast2Struct(MemoryLayout.BIOS_BUFFER_MEMMAP_START);
	
	public static VecVesaMode AvailableModes()
	{
		// get information through real mode interrupt
		contrInfo.id = 0x32454256; // VBE2
		BIOS.Registers.EAX = 0x4F00; // get controller information
		// BIOS.regs.DS=(short)(KernelConst.KM_SCRATCH>>>4);
		BIOS.Registers.ES = (short) (MemoryLayout.BIOS_BUFFER_MEMMAP_START >>> 4);
		BIOS.Registers.EDI = MemoryLayout.BIOS_BUFFER_MEMMAP_START & 0xF;
		BIOS.rint(0x10);
		
		// check signatures
		if ((short) BIOS.Registers.EAX != (short) 0x004F)
			return null;
		if (contrInfo.id != 0x41534556)
			return null; // VESA
		
		// VESA detected, get information of controller info struct
		if (contrInfo.version < (byte) 2)
			return null; // at least version 1.2 required
		int modePtr = (((int) contrInfo.videoModePtrSeg & 0xFFFF) << 4) + ((int) contrInfo.videoModePtrOff & 0xFFFF);
		
		// get all available modi
		VecInt modeNumbers = new VecInt();
		int modeNr;
		while ((modeNr = (int) MAGIC.rMem16(modePtr) & 0xFFFF) != 0xFFFF)
		{
			modeNumbers.add(modeNr);
			modePtr += 2;
		}
		
		// get information for available modi (cannot be done above because info-struct
		// is overwritten)
		VecVesaMode modes = new VecVesaMode(modeNumbers.size());
		for (int i = 0; i < modeNumbers.size(); i++)
		{
			int vesaMode = modeNumbers.get(i);
			BIOS.Registers.EAX = 0x4F01; // get mode information
			BIOS.Registers.ECX = vesaMode;
			BIOS.Registers.ES = (short) (MemoryLayout.BIOS_BUFFER_MEMMAP_START >>> 4);
			BIOS.Registers.EDI = MemoryLayout.BIOS_BUFFER_MEMMAP_START & 0xF;
			BIOS.rint(0x10);
			if ((modeInfo.attributes & VESAModeInfoStruct.ATTR_LINFRMBUF) == (short) 0)
			{ // no linear frame buffer
				// TO-DO remove mode from list
			}
			else
			{ // linear frame buffer supported
				boolean isGraphical = (modeInfo.attributes & VESAModeInfoStruct.ATTR_GRAPHICAL) != (short) 0;
				int XRes = Integer.ushort(modeInfo.xRes);
				int YRes = Integer.ushort(modeInfo.yRes);
				int ColorDepth = Integer.ubyte(modeInfo.colDepth);
				int LfbAddress = modeInfo.lfbAddress;
				VESAMode mode = new VESAMode(vesaMode, XRes, YRes, ColorDepth, LfbAddress, isGraphical);
				modes.add(mode);
			}
		}
		
		// return driver object
		return modes;
	}
	
	public static VESAMode GetMode(VecVesaMode modes, int xRes, int yRes, int colDepth, boolean graphical)
	{
		for (int i = 0; i < modes.size(); i++)
		{
			VESAMode mode = modes.get(i);
			if (mode.XRes == xRes && mode.YRes == yRes && mode.ColorDepth == colDepth && mode.Graphical == graphical)
			{
				return mode;
			}
		}
		return null;
	}
	
	public static String ModesToStr(VecVesaMode modes)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < modes.size(); i++)
		{
			VESAMode mode = modes.get(i);
			sb.appendLine(mode);
		}
		return sb.toString();
	}
}
