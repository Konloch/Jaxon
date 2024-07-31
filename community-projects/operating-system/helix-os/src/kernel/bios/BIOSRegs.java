package kernel.bios;

public class BIOSRegs extends STRUCT
{
	public short DS, ES, FS, FLAGS;
	public int EDI, ESI, EBP, ESP, EBX, EDX, ECX, EAX;
}