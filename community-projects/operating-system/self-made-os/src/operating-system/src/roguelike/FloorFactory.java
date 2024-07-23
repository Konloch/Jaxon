package roguelike;

import hardware.Random;

public class FloorFactory
{
	private static int DIR_UP = 0;
	private static int DIR_RIGHT = 1;
	private static int DIR_DOWN = 2;
	private static int DIR_LEFT = 3;
	
	static Floor getRandomFloor()
	{
		//try making a room often, and only take ones that don't overlap. this is a value that should be played with.
		int roomCreationTries = Resources.MAX_FLOORFACTORY_ROOMTRIES;
		RoomCollection rooms = new RoomCollection();
		for (int i = 0; i < roomCreationTries; i++)
		{
			if (rooms.getCount() >= Resources.MAX_FLOORFACTORY_ROOMS)
				break;
			int posx = Random.rand(0, Resources.MAX_PLAYFIELD_WIDTH - 1);
			int posy = Random.rand(0, Resources.MAX_PLAYFIELD_HEIGHT - 1);
			int width = Random.rand(5, 26);
			int height = Random.rand(3, 10);
			//ensure values are in bounds
			if (posx + width >= Resources.MAX_PLAYFIELD_WIDTH)
			{
				posx -= (posx + width) - Resources.MAX_PLAYFIELD_WIDTH + 1;
			}
			if (posy + height >= Resources.MAX_PLAYFIELD_HEIGHT)
			{
				posy -= (posy + height) - Resources.MAX_PLAYFIELD_HEIGHT + 1;
			}
			Room newR = new SquareRoom(posx, posy, width, height);
			boolean overlap = false;
			for (Room r : rooms.getRooms())
			{
				if (r == null)
					continue;
				if (roomsOverlap(newR, r))
				{
					//rooms overlap
					overlap = true;
					break;
				}
			}
			//remove this debug
			newR.setSeen(true);
			if (!overlap)
				rooms.append(newR);
		}
		//generate basic floor object, at this point we only have rooms.
		Floor f = new Floor(rooms);
		
		/*
		//work on connections between rooms
		fillMaze(f);
		https://www.saschawillems.de/blog/2010/02/07/random-dungeon-generation/  */
		
		//TODO: backup plan, linien-basiert wege (langweilig)
		//ensureRoomConnections(f);
		return f;
	}
	
	private static void ensureRoomConnections(Floor f)
	{
		//select one random room as spawn room
		int cap = f.getRooms().getCount();
		int rIndex = Random.rand(0, cap - 1);
		Room spawnRoom = f.getRooms().getRooms()[rIndex];
		//make bool array of which rooms we know are connected
		boolean[] connected = new boolean[cap];
		//set spawn room as true (connected to itself)
		connected[rIndex] = true;
		while (anyFalse(connected))
		{
			//select room that is not yet connected
			int index = -1;
			/*for (int i = 0; i < cap; i++)
			{
				if (!connected[i])
					index = i;
				break;
			}*/
			//find closest room that is already connected by using top-left coordinates
			Room roomToBeConnected = f.getRooms().getRooms()[index];
			int minDistance = 999999999;
			Room nearestRoom = null;
			for (Room r : f.getRooms().getRooms())
			{
				int distance = roomToBeConnected.getTopLeft().distanceTo(r.getTopLeft());
				if (distance < minDistance)
					nearestRoom = r;
			}
			//now we have the nearest room, do the connection
			//connectRooms(roomToBeConnected, nearestRoom, f);
		}
	}
	
	private static void connectRooms(Room leftRoom, Room rightRoom, Floor f)
	{
		//we take a point on either left or right side of one room (preferrably whichever is closer to other room)
		//and a point on either up or down side of other room (again preferrably whichever is closer)
		//we then draw lines orthogonal to the axis of the side the point is on through the point
		// (so parallel to y axis if on top or bottom, or parallel to x axis if on left or right side)
		//then take another point which is one tile away from the side the original point was on, for both points
		//and find both intersections of axis-parallel lines going through the points.
		//hopefully, at least one of them should NOT go through a room
		
		
		//is leftRoom above or below rightRoom
		boolean lAboveR = leftRoom.getTopLeft().getPosy() < rightRoom.getTopLeft().getPosy();
		boolean lLeftOfR = leftRoom.getTopLeft().getPosx() < rightRoom.getTopLeft().getPosx();
		
		//todo: finish this part and draw connections
		if (!lAboveR && !lLeftOfR)
		{ //right side on r, top side on l
			Coordinate lCoord = new Coordinate(leftRoom.getTopLeft().getPosx() + Random.rand(1, leftRoom.getWidth() - 2), leftRoom.getTopLeft().getPosy() + 1);
			Coordinate rCoord = new Coordinate(rightRoom.getTopRight().getPosx() + 1, rightRoom.getTopRight().getPosy() + Random.rand(1, rightRoom.getHeight() - 2));
			
		}
		else if (lAboveR && lLeftOfR)
		{ //left side on r, bottom side on l
		
		}
		else if (lAboveR)
		{ //right side on r, bottom side on l
		
		}
		else
		{ //left side on r, top side on l
		
		}
	}
	
	private static boolean anyFalse(boolean[] bools)
	{
		for (boolean b : bools)
			if (!b)
				return false;
		return true;
	}
	
	
	private static final int OVERLAP_BORDER = 2;
	
	//https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection
	private static boolean roomsOverlap(Room r1, Room r2)
	{
		return r1.getX() < r2.getX() + r2.getWidth() + OVERLAP_BORDER && r1.getX() + r1.getWidth() + OVERLAP_BORDER > r2.getX() && r1.getY() < r2.getY() + r2.getHeight() + OVERLAP_BORDER && r1.getY() + r1.getHeight() + OVERLAP_BORDER > r2.getY();
	}
}
