package roguelike;

//container class for coordinates
class CoordinateList
{
	Coordinate[] coords = new Coordinate[Resources.MAX_PLAYFIELD_WIDTH * Resources.MAX_PLAYFIELD_HEIGHT];
	private int count = 0;
	
	void add(Coordinate c)
	{
		coords[count] = c;
		count++;
	}
	
	Coordinate coordinateAt(int index)
	{
		return coords[index];
	}
	
	boolean contains(Coordinate coord)
	{
		for (int i = 0; i < count; i++)
		{
			if (coords[i].getPosx() == coord.getPosx() && coords[i].getPosy() == coord.getPosy())
				return true;
		}
		return false;
	}
	
	public int getCount()
	{
		return count;
	}
}
