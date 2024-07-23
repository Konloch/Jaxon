package apps.tetris;

import container.List;

public class Field
{
	private final int width;
	private final int height;
	private final List rows = new List();
	
	public Field(int width, int height)
	{
		this.width = width;
		this.height = height;
		for (int i = 0; i < height; i++)
		{
			this.rows.append(new Row(width));
		}
	}
	
	public void set(int x, int y, byte color)
	{
		Row row = (Row) this.rows.get(y);
		row.set(x, color);
	}
	
	public boolean isSet(int x, int y)
	{
		Row row = (Row) this.rows.get(y);
		return row.isSet(x);
	}
	
	public byte colorAt(int x, int y)
	{
		Row row = (Row) this.rows.get(y);
		return row.colorAt(x);
	}
	
	public int cleanUp()
	{
		int count = 0;
		for (int i = 0; i < this.height; i++)
		{
			Row row = (Row) this.rows.get(i);
			if (row.left() <= 0)
			{
				this.rows.remove(i--);
				this.rows.append(new Row(this.width));
				count++;
			}
		}
		return count;
	}
	
	public int width()
	{
		return this.width;
	}
	
	public int height()
	{
		return this.height;
	}
}
