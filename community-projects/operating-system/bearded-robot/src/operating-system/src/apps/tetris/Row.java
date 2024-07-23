package apps.tetris;

public class Row
{
	private int left;
	private final short[] positions;
	
	public Row(int lenght)
	{
		this.positions = new short[lenght];
		this.left = lenght;
	}
	
	public void set(int index, byte color)
	{
		if ((this.positions[index] & 1) == 0)
			this.left--;
		this.positions[index] = (short) ((color << 8) | 1);
	}
	
	public boolean isSet(int index)
	{
		return (this.positions[index] & 1) == 1;
	}
	
	public byte colorAt(int index)
	{
		return (byte) (this.positions[index] >> 8);
	}
	
	public int left()
	{
		return this.left;
	}
	
	public int lenght()
	{
		return this.positions.length;
	}
}
