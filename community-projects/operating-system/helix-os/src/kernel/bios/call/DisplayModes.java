package kernel.bios.call;

import kernel.bios.BIOS;

public class DisplayModes extends BIOS
{
	public static void activateGraphicsMode()
	{
		BIOS.Registers.EAX = 0x0013;
		BIOS.rint(0x10);
	}
	
	public static void activateTextMode()
	{
		BIOS.Registers.EAX = 0x0003;
		BIOS.rint(0x10);
	}
	
	public static void setVesaMode(int modeNr)
	{
		Registers.EAX = 0x4F02;
		Registers.EBX = modeNr;
		BIOS.rint(0x10);
	}
}
