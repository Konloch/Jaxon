package roguelike;

import graphics.Console;
import graphics.ConsoleColors;
import hardware.Serial;
import roguelike.entities.Enemy;
import roguelike.entities.EnemyCollection;
import roguelike.entities.Entity;
import roguelike.entities.Player;
import roguelike.tiles.PathTile;
import roguelike.tiles.Tile;
import rte.DynamicRuntime;
import rte.SClassDesc;

public class Renderer
{
	private final Floor floor;
	private final Player player;
	
	Renderer(Floor floor, Player player)
	{
		this.floor = floor;
		this.player = player;
	}
	
	void renderFloor()
	{
		Room[] rooms = floor.getRooms().getRooms();
		Console.clearConsole();
		for (Room r : rooms)
		{
			if (!r.isSeen())
				continue;
			Tile[][] tiles = r.getRoomTiles();
			Coordinate c = r.getTopLeft();
			for (int y = 0; y < r.getHeight(); y++)
			{
				//set cursor to correct position
				Console.setCursor(r.getX(), r.getY() + y);
				for (int x = 0; x < r.getWidth(); x++)
				{
					Console.print(tiles[y][x].getSymbol());
				}
			}
		}
		
		CoordinateList paths = floor.getPathTileCoords();
		for (Coordinate c : paths.coords)
		{
			if (c == null)
				break;
			Console.directPrintChar(PathTile.sym, c.getPosx(), c.getPosy(), ConsoleColors.DEFAULT_CONSOLE_COLOR);
		}
	}
	
	void renderEntity(Entity e)
	{
		int consColor = ConsoleColors.DEFAULT_CONSOLE_COLOR;
		if (DynamicRuntime.isInstance(e, (SClassDesc) MAGIC.clssDesc("Enemy"), false))
		{
			consColor = ConsoleColors.BG_BLACK | ConsoleColors.FG_RED;
		}
		else if (DynamicRuntime.isInstance(e, (SClassDesc) MAGIC.clssDesc("Player"), false))
		{
			consColor = ConsoleColors.BG_BLACK | ConsoleColors.FG_GREEN;
		}
		else
		{
			Serial.print("Cannot determine type of Entity in renderEntity");
			MAGIC.inline(0xCC);
		}
		Coordinate lastCoord = e.getLastCoord();
		if (lastCoord != null)
		{
			//TODO: possible bug? what if e1 moves away from tile, e2 moves on it, and e2 is rendered before e1?
			Room[] rooms = floor.getRooms().getRooms();
			for (Room r : rooms)
			{
				if (r.containsCoordinate(lastCoord))
				{
					Tile[][] roomTiles = r.getRoomTiles();
					//translate coordinate to room coordinates
					Tile t = roomTiles[lastCoord.getPosy() - r.getY()][lastCoord.getPosx() - r.getX()];
					Console.directPrintChar(t.getSymbol(), lastCoord.getPosx(), lastCoord.getPosy(), ConsoleColors.DEFAULT_CONSOLE_COLOR);
					//we found the room and printed, so we don't have to check the other rooms
					break;
				}
			}
		}
		Coordinate currentCoord = e.getCoord();
		Console.directPrintChar(e.getSymbol(), currentCoord.getPosx(), currentCoord.getPosy(), consColor);
	}
	
	void renderPlayer()
	{
		renderEntity(player);
	}
	
	public void renderEnemies(EnemyCollection enemies)
	{
		for (Enemy e : enemies.getEnemies())
		{
			renderEntity(e);
		}
	}
	
	public void renderTile(Tile t, Coordinate coord)
	{
		Console.directPrintChar(t.getSymbol(), coord.getPosx(), coord.getPosy(), ConsoleColors.DEFAULT_CONSOLE_COLOR);
	}
	
	public void rerenderTile(Coordinate coordinate)
	{
		renderTile(floor.getTileAtCoordinate(coordinate), coordinate);
	}
}
