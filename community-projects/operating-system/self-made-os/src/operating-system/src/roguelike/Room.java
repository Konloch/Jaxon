package roguelike;

import roguelike.tiles.Tile;

public abstract class Room
{
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	protected Tile[][] roomTiles;
	private boolean seen;
	
	Room(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.seen = false;
	}
	
	Coordinate getTopLeft()
	{
		return new Coordinate(x, y);
	}
	
	Coordinate getTopRight()
	{
		return new Coordinate(x + width, y);
	}
	
	Coordinate getBottomLeft()
	{
		return new Coordinate(x, y + height);
	}
	
	Coordinate getBottomRight()
	{
		return new Coordinate(x + width, y + height);
	}
	
	Coordinate getCenter()
	{
		return new Coordinate(x + width / 2, y + height / 2);
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	Tile[][] getRoomTiles()
	{
		return roomTiles;
	}
	
	boolean containsCoordinate(Coordinate c)
	{
		return c.getPosx() >= x && c.getPosx() <= x + width && c.getPosy() >= y && c.getPosy() <= y + height;
	}
	
	boolean isSeen()
	{
		return seen;
	}
	
	void setSeen(boolean seen)
	{
		this.seen = seen;
	}
}
