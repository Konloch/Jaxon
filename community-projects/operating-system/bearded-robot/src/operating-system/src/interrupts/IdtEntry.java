package interrupts;

class IdtEntry extends STRUCT
{
	public short offsetLo;
	public short selector;
	public byte zero;
	public byte flags;
	public short offsetHi;
}
