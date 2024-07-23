package roguelike;

import graphics.Console;
import hardware.Serial;
import hardware.keyboard.Key;
import hardware.keyboard.KeyboardEvent;
import roguelike.entities.Enemy;
import roguelike.entities.FinalBoss;
import roguelike.entities.Player;
import roguelike.entities.Zombie;
import roguelike.items.*;
import roguelike.tiles.Tile;
import rte.DynamicRuntime;
import rte.SClassDesc;
import sysutils.Scheduler;
import sysutils.exec.Executable;
import sysutils.exec.ExecutableFactory;
import sysutils.exec.ExecutableStore;

public class RogueExec extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			
			@Override
			public Executable createExecutable()
			{
				return new RogueExec();
			}
			
			@Override
			public String getName()
			{
				return "rogue";
			}
		});
	}
	
	RogueExec()
	{
		acceptsKeyboardInputs = true;
		messages = new MessageStatPrinter();
	}
	
	public boolean firstRun = true;
	//game state
	private Floor currFloor;
	private Player player;
	
	//game dependencies
	private Renderer renderer;
	private final MessageStatPrinter messages;
	
	//confirm quit
	private boolean confirmQuit = false;
	//save time not rerendering stats every time
	private boolean rerenderStats = true;
	//consumable selection
	private boolean consumableSelection = false;
	private Consumable[] consumables;
	
	//eqipable selection
	private boolean equipableSelection = false;
	private Equipable[] equipables;
	
	void init()
	{
		Console.disableCursor();
		firstRun = false;
		//TODO: remove this debug
		
	}
	
	void cleanup()
	{
		Console.enableCursor();
		Console.clearConsole();
	}
	
	@Override
	public int execute()
	{
		//get a new floor if there is no floor currently
		//TODO: generate the floor randomly
		if (currFloor == null)
		{
			currFloor = FloorFactory.getRandomFloor();
		}
		//if player isn't initialized, create new object with random coords on a valid Tile
		if (player == null)
		{
			Coordinate spawn = currFloor.getValidSpawn();
			player = new Player(spawn);
			
			//TODO: REMOVE THIS DEBUG
			//player.setGodMode(true);
		}
		//initialize renderer
		if (renderer == null)
		{
			renderer = new Renderer(currFloor, player);
			renderer.renderFloor();
			renderer.renderPlayer();
		}
		//initialize everything we need to initialize if its the first execution
		if (firstRun)
		{
			//display stats
			//initialize empty message?
			init();
		}
		
		if (consumableSelection)
		{
			while (buffer.canRead())
			{
				KeyboardEvent kev = buffer.readEvent();
				int selectedIndex = kev.KEYCODE - Key.ZERO;
				if (selectedIndex > consumables.length - 1 || selectedIndex < 0 || selectedIndex > 9)
				{
					messages.queueMessage("invalid selection");
				}
				else
				{
					StringBuilder sb = new StringBuilder("Used ");
					Consumable c = consumables[selectedIndex];
					sb.append(c.name);
					c.onUse(player, messages);
					player.getItems().delete(c);
					messages.queueMessage(sb.getString());
					messages.printStats(player);
				}
				consumableSelection = false;
				messages.printNextMessage();
			}
		}
		
		if (equipableSelection)
		{
			while (buffer.canRead())
			{
				KeyboardEvent kev = buffer.readEvent();
				int selectedIndex = kev.KEYCODE - Key.ZERO;
				if (selectedIndex > equipables.length - 1 || selectedIndex < 0 || selectedIndex > 9)
				{
					messages.queueMessage("invalid selection");
				}
				else
				{
					StringBuilder sb = new StringBuilder("Equipped");
					Equipable c = equipables[selectedIndex];
					sb.append(c.name);
					c.onEquip(player, messages);
					if (DynamicRuntime.isInstance(c, (SClassDesc) MAGIC.clssDesc("Weapon"), false))
					{
						//call unequip on old weapon
						player.setEquippedWeapon((Weapon) c, messages);
					}
					else
					{
						//TODO: other types of equippables? armor?
					}
					player.getItems().delete(c);
					messages.queueMessage(sb.getString());
					messages.printStats(player);
				}
				equipableSelection = false;
				messages.printNextMessage();
			}
		}
		while (buffer.canRead())
		{
			KeyboardEvent kev = buffer.readEvent();
			if (confirmQuit)
			{
				if (kev.KEYCODE == Key.q || kev.KEYCODE == Key.Q)
				{
					cleanup();
					Scheduler.markTaskAsFinished(this);
					return 0;
				}
				else
				{
					confirmQuit = false;
					messages.clearMessage();
				}
			}
			//if we have messages to display, ignore the pressed key entirely
			if (messages.hasMessages())
			{
				messages.printNextMessage();
			}
			else
			{
				messages.clearMessage();
				switch (kev.KEYCODE)
				{
					case Key.h:
						movement(Resources.DIR_LEFT);
						break;
					case Key.j:
						movement(Resources.DIR_DOWN);
						break;
					case Key.k:
						movement(Resources.DIR_UP);
						break;
					case Key.l:
						movement(Resources.DIR_RIGHT);
						break;
					case Key.Q:
						confirmQuit = true;
						messages.queueMessage("Press Q/q again to quit.");
						break;
					case Key.d:
						player.toggleGodmode();
						break;
					case Key.H:
						spawnHealthpotion();
						break;
					case Key.Z:
						spawnEndboss();
						break;
					case Key.R:
						moveToNewFloor();
						break;
					case Key.c:
						printConsumables();
						break;
					case Key.e:
						printEquipables();
						break;
					case Key.E:
						spawnExcalibur();
						break;
				}
				//check this again to make sure we print everything
				if (messages.hasMessages() && !player.isDead() && !player.hasWon())
				{
					messages.printNextMessage();
				}
			}
			if (player.isDead())
			{
				return 0;
			}
			if (player.hasWon())
			{
				winScreen();
				return 0;
			}
			renderer.renderPlayer();
			renderer.renderEnemies(currFloor.getEnemies());
		}
		if (rerenderStats)
		{
			messages.printStats(player);
			rerenderStats = false;
		}
		return 0;
	}
	
	private void printEquipables()
	{
		equipables = messages.printEquipables(player.getItems());
		if (equipables != null)
			equipableSelection = true;
	}
	
	private void printConsumables()
	{
		consumables = messages.printConsumables(player.getItems());
		if (consumables != null)
			consumableSelection = true;
	}
	
	private void moveToNewFloor()
	{
	}
	
	//check if movement is obstructed by tile or enemy, action accordingly
	private void movement(int direction)
	{
		Coordinate coord, newCoord;
		coord = player.getCoord();
		switch (direction)
		{
			case Resources.DIR_UP:
				newCoord = new Coordinate(coord.getPosx(), coord.getPosy() - 1);
				break;
			case Resources.DIR_DOWN:
				newCoord = new Coordinate(coord.getPosx(), coord.getPosy() + 1);
				break;
			case Resources.DIR_LEFT:
				newCoord = new Coordinate(coord.getPosx() - 1, coord.getPosy());
				break;
			case Resources.DIR_RIGHT:
				newCoord = new Coordinate(coord.getPosx() + 1, coord.getPosy());
				break;
			default:
				return;
		}
		//check if passable
		if (!currFloor.isCoordinatePassable(newCoord))
			return;
		//check if enemy on tile
		Enemy enemy = currFloor.getEnemyAtCoordinate(newCoord);
		if (enemy != null)
		{
			rerenderStats = true;
			//TODO: handle fight
			int fightOutcome = Combat.doMeleeCombat(player, enemy, messages);
			if (fightOutcome == Combat.PLAYER_DIED)
			{
				player.setDead();
				deathScreen();
				return;
			}
			if (fightOutcome == Combat.ENEMY_DIED)
			{
				//remove the enemy from the enemy list, and rerender tile it was on
				currFloor.killEnemy(enemy);
				Tile t = currFloor.getTileAtCoordinate(newCoord);
				renderer.renderTile(t, newCoord);
				StringBuilder sb = new StringBuilder();
				sb.append(enemy.getName());
				sb.append(" died!");
				messages.queueMessage(sb.getString());
				//call onDeath event for enemy for additional effects
				enemy.onDeath(player, currFloor.getTileAtCoordinate(newCoord));
			}
			return;
		}
		if (currFloor.hasItemAtCoordinate(newCoord))
		{
			Item it = currFloor.getItemAtCoordinate(newCoord);
			player.getItems().append(it);
			StringBuilder sb = new StringBuilder("Found item: ");
			sb.append(it.name);
			messages.queueMessage(sb.getString());
		}
		player.move(newCoord);
	}
	
	private void spawnZombie()
	{
		//generate new zombie
		Zombie z = new Zombie(currFloor.getValidEnemySpawn(player.getCoord()));
		//insert it into the floor
		if (currFloor.insertEnemy(z))
			Serial.print("success spawning zombie!\n");
	}
	
	private void spawnEndboss()
	{
		Enemy e = new FinalBoss(currFloor.getValidEnemySpawn(player.getCoord()));
		if (currFloor.insertEnemy(e))
			Serial.print("success spawning final boss\n ");
	}
	
	private void spawnHealthpotion()
	{
		//add a healpotion to player inventory
		player.getItems().append(new HealingPotion());
	}
	
	//drops excalibur next to player
	private void spawnExcalibur()
	{
		spawnItem(new Excalibur(), findSpawnCoordNextToPlayer());
	}
	
	private void spawnItem(Item item, Coordinate coordinate)
	{
		currFloor.getTileAtCoordinate(coordinate).putItem(item);
		renderer.rerenderTile(coordinate);
	}
	
	private Coordinate findSpawnCoordNextToPlayer()
	{
		Coordinate spawnCoord = new Coordinate(player.getCoord().getPosx() + 1, player.getCoord().getPosy());
		if (currFloor.isCoordinatePassable(spawnCoord))
		{
			return spawnCoord;
		}
		spawnCoord = new Coordinate(player.getCoord().getPosx() - 1, player.getCoord().getPosy());
		if (currFloor.isCoordinatePassable(spawnCoord))
		{
			return spawnCoord;
		}
		spawnCoord = new Coordinate(player.getCoord().getPosx(), player.getCoord().getPosy() + 1);
		if (currFloor.isCoordinatePassable(spawnCoord))
		{
			return spawnCoord;
		}
		spawnCoord = new Coordinate(player.getCoord().getPosx(), player.getCoord().getPosy() - 1);
		if (currFloor.isCoordinatePassable(spawnCoord))
		{
			return spawnCoord;
		}
		return null;
	}
	
	
	private void deathScreen()
	{
		//TODO: make this nice like the original rogue death screen
		Console.clearConsole();
		Console.print("YOU DIED. RIP.\n");
		Scheduler.markTaskAsFinished(this);
	}
	
	private void winScreen()
	{
		Console.clearConsole();
		StringBuilder sb = new StringBuilder();
		sb.append("You have managed to best ");
		sb.append(Resources.ENDBOSS_ENEMYNAME);
		sb.append(". The world is saved, you are a hero, and the world will forever tell the tales of your adventure.\n");
		Console.print(sb.getString());
		Scheduler.markTaskAsFinished(this);
	}
}
