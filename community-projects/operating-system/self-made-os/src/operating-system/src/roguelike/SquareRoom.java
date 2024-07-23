package roguelike;

import roguelike.tiles.FloorTile;
import roguelike.tiles.Tile;
import roguelike.tiles.WallTile;

public class SquareRoom extends Room
{
	SquareRoom(int x, int y, int width, int height)
	{
		super(x, y, width, height);
		this.roomTiles = new Tile[height][width];
		for (int xi = 0; xi < width; xi++)
		{
			for (int yi = 0; yi < height; yi++)
			{
				roomTiles[yi][xi] = new FloorTile();
			}
		}
		
		//override walls
		for (int xi = 0; xi < width; xi++)
		{
			roomTiles[0][xi] = new WallTile();
			roomTiles[height - 1][xi] = new WallTile();
		}
		for (int yi = 0; yi < height; yi++)
		{
			roomTiles[yi][0] = new WallTile();
			roomTiles[yi][width - 1] = new WallTile();
		}
	}
}
