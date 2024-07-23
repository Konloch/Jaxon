package interrupts;

class Pics
{
	public final static int MASTER = 0x20;
	
	public final static int SLAVE = 0xA0;
	
	private Pics()
	{
	}
	
	public static void init()
	{
		programmChip(MASTER, 0x20, 0x04); // init offset and slave config of master
		programmChip(SLAVE, 0x28, 0x02); // init offset and slave config of slave
	}
	
	private static void programmChip(int port, int offset, int icw3)
	{
		MAGIC.wIOs8(port++, (byte) 0x11); // ICW1
		MAGIC.wIOs8(port, (byte) offset); // ICW2
		MAGIC.wIOs8(port, (byte) icw3); // ICW3
		MAGIC.wIOs8(port, (byte) 0x01); // ICW4
	}
}
