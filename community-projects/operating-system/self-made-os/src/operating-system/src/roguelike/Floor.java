package roguelike;

import hardware.Random;
import hardware.Serial;
import roguelike.entities.Enemy;
import roguelike.entities.EnemyCollection;
import roguelike.items.Item;
import roguelike.tiles.*;
import rte.DynamicRuntime;
import rte.SClassDesc;

class Floor {
	private EnemyCollection enemies;
	private RoomCollection rooms;
	private CoordinateList pathTileCoords;
	private ConnectionList connections;
	
	Floor(RoomCollection rooms) {
		this.rooms = rooms;
		enemies = new EnemyCollection();
		rooms = new RoomCollection();
		pathTileCoords = new CoordinateList();
		connections = new ConnectionList();
	}
	
	void setPathTileCoords(CoordinateList cl) {
		pathTileCoords = cl;
	}
	
	public CoordinateList getPathTileCoords() {
		return pathTileCoords;
	}
	
	public ConnectionList getConnections() {
		return connections;
	}
	
	//returns a valid spawn position for player
	public Coordinate getValidSpawn() {
		//get list of valid coordinates, pick one at random
		CoordinateList cl = getValidSpawnPoints();
		return cl.coordinateAt(Random.unsignedRand()%cl.getCount());
	}
	
	//returns a valid spawn position for an enemy, except for the position the player is currently in
	public Coordinate getValidEnemySpawn(Coordinate playerPos) {
		CoordinateList cl = getValidSpawnPoints();
		Coordinate c = null;
		while(c==null||c==playerPos){
			c = cl.coordinateAt(Random.unsignedRand()%cl.getCount());
		}
		return c;
	}
	
	//returns whether or not tile at coord is passable
	public boolean isCoordinatePassable(Coordinate coord) {
		//we didn't find a room that contains the coordinate, therefore it is unpassable
		Tile t = getTileAtCoordinate(coord);
		if(t!=null)
			return t.isPassable();
		return pathTileCoords.contains(coord);
	}
	
	//returns enemy if there is one at coord, otherwise null
	public Enemy getEnemyAtCoordinate(Coordinate coord) {
		for(Enemy e : enemies.getEnemies()) {
			if (e==null) continue;
			if (e.getCoord().equals(coord)) return e;
		}
		return null;
	}
	
	//adds enemy to floor and returns true if successful, returns false if there is already an enemy at that coordinate
	//TODO: rethink if this is the correct approach
	public boolean insertEnemy(Enemy enemy) {
		if(getEnemyAtCoordinate(enemy.getCoord())!=null) return false;
		return enemies.append(enemy);
	}
	
	public EnemyCollection getEnemies() {
		return enemies;
	}
	
	public void killEnemy(Enemy e) {
		enemies.delete(e);
	}
	
	public Tile getTileAtCoordinate(Coordinate coord) {
		for (Room r : rooms.getRooms()) {
			if(r.containsCoordinate(coord)) {
				return r.getRoomTiles()[coord.getPosy()-r.getY()][coord.getPosx()-r.getX()];
			}
		}
		//there is no tile at that coordinate
		return null;
	}
	
	public RoomCollection getRooms() {
		return rooms;
	}
	
	//returns a list of all valid spawn points
	private CoordinateList getValidSpawnPoints() {
		CoordinateList list = new CoordinateList();
		for (Room r : rooms.getRooms()) {
			Tile[][] roomTiles = r.getRoomTiles();
			for (int x = 0; x < r.getWidth(); x++) {
				for (int y = 0; y < r.getHeight(); y++) {
					Tile tile = roomTiles[y][x];
					if(tile.isPassable() && !DynamicRuntime.isInstance(tile, (SClassDesc) MAGIC.clssDesc("PathTile"), false)) {
						list.add(new Coordinate(x+r.getX(), y+r.getY()));
					}
				}
			}
		}
		return list;
	}
	
	public Item getItemAtCoordinate(Coordinate coordinate) {
		FloorTile ft = ((FloorTile)getTileAtCoordinate(coordinate));
		if(ft!=null) {
			Item item = ft.getItem();
			ft.removeItem();
			return item;
		}
		return null;
	}
	
	public boolean hasItemAtCoordinate(Coordinate coordinate) {
		FloorTile ft = ((FloorTile)getTileAtCoordinate(coordinate));
		if(ft!=null) {
			Item item = ft.getItem();
			return item!=null;
		}
		return false;
	}
}
