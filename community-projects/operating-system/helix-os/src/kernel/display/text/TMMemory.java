package kernel.display.text;

public class TMMemory extends STRUCT
{
	@SJC(count = 2000)
	public TM3DisplayCell[] cells;
}