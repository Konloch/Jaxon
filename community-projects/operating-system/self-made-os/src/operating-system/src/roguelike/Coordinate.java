package roguelike;

public class Coordinate
{
	private final int posx, posy;
	
	Coordinate(int posx, int posy)
	{
		this.posx = posx;
		this.posy = posy;
	}
	
	public int getPosx()
	{
		return posx;
	}
	
	public int getPosy()
	{
		return posy;
	}
	
	public boolean equals(Coordinate c)
	{
		if (this.getPosx() != c.getPosx())
			return false;
		return this.getPosy() == c.getPosy();
	}
	
	public int distanceTo(Coordinate coordinate)
	{
		int xDis = this.posx - coordinate.posx;
		int yDis = this.posy - coordinate.posy;
		if (xDis < 0)
			xDis *= -1;
		if (yDis < 0)
			yDis *= -1;
		return xDis + yDis;
	}
}
