package roguelike;

import graphics.Console;
import roguelike.entities.Player;
import roguelike.items.Consumable;
import roguelike.items.Equipable;
import roguelike.items.Item;
import roguelike.items.ItemCollection;
import rte.DynamicRuntime;
import rte.SClassDesc;

public class MessageStatPrinter
{
	static final int MESSAGEBAR_LINE = 21;
	static final int CHARACTER_STATS_LINE = 23;
	final MessageRingBuffer msgBuf;
	boolean msgPrinted;
	
	MessageStatPrinter()
	{
		msgBuf = new MessageRingBuffer();
	}
	
	//ring buffer
	static private class MessageRingBuffer
	{
		private static final int DEFAULT_SIZE = 16;
		private final String[] buffer;
		private final int size;
		private int writePointer = 0;
		private int readPointer = 0;
		
		public MessageRingBuffer()
		{
			buffer = new String[DEFAULT_SIZE];
			size = DEFAULT_SIZE;
		}
		
		public MessageRingBuffer(int size)
		{
			buffer = new String[size];
			this.size = size;
		}
		
		private void increaseWritePointer()
		{
			if (writePointer == size - 1)
			{
				writePointer = 0;
				return;
			}
			writePointer++;
		}
		
		private void increaseReadPointer()
		{
			if (readPointer == writePointer)
				return;
			if (readPointer == size - 1)
			{
				readPointer = 0;
				return;
			}
			readPointer++;
		}
		
		//returns whether or not there is new data to be read
		//if canRead() returns false, then readMessage() and peekMessage() will read old data
		public boolean canRead()
		{
			return readPointer != writePointer;
		}
		
		//returns whether or not buffer is full
		public boolean full()
		{
			return (writePointer == readPointer - 1);
		}
		
		//returns the next byte in the buffer without advancing the read pointer
		//if canRead() returns false, then this will return old data
		public String peekMessage()
		{
			return buffer[readPointer];
		}
		
		//returns the next message in the buffer
		//if canRead() returns false, then this will return old data
		public String readMessage()
		{
			String s = peekMessage();
			increaseReadPointer();
			return s;
		}
		
		//writes the message s into the buffer
		public void writeMessage(String s)
		{
			buffer[writePointer] = s;
			increaseWritePointer();
		}
		
		//clears the buffer
		public void clearBuffer()
		{
			readPointer = writePointer = 0;
		}
		
	}
	
	
	//true if there are messages in queue, false otherwise
	boolean hasMessages()
	{
		return msgBuf.canRead();
	}
	
	//returns true if message was queued, false if buffer is full
	public boolean queueMessage(String msg)
	{
		if (msgBuf.full())
			return false;
		msgBuf.writeMessage(msg);
		return true;
	}
	
	void printNextMessage()
	{
		if (msgPrinted)
			clearMessage();
		String s = msgBuf.readMessage();
		Console.setCursor(0, MESSAGEBAR_LINE);
		Console.print(s);
		if (hasMessages())
			Console.print(" [...]");
		msgPrinted = true;
	}
	
	void clearMessage()
	{
		Console.setCursor(0, MESSAGEBAR_LINE);
		for (int i = 0; i < 80; i++)
		{
			Console.print(' ');
		}
		msgPrinted = false;
	}
	
	public void printStats(Player p)
	{
		Console.setCursor(0, CHARACTER_STATS_LINE);
		//craft string first for performance reasons
		StringBuilder sb = new StringBuilder("  Health: ");
		sb.append(p.getHealth());
		sb.append("/");
		sb.append(p.getMaxHealth());
		sb.append(" Stats: ");
		sb.append(p.getStrength());
		sb.append("Str/");
		sb.append(p.getIntelligence());
		sb.append("Int/");
		sb.append(p.getDefense());
		sb.append("Def");
		Console.print(sb.getString());
	}
	
	public Consumable[] printConsumables(ItemCollection items)
	{
		int index = 0;
		Consumable[] consumables = new Consumable[50];
		StringBuilder sb = new StringBuilder();
		for (Item i : items.getItems())
		{
			if (DynamicRuntime.isInstance(i, (SClassDesc) MAGIC.clssDesc("Consumable"), false))
			{
				sb.append(index);
				sb.append(": ");
				sb.append(i.name);
				sb.append(" ");
				consumables[index] = (Consumable) i;
				index++;
			}
		}
		if (index > 0)
		{
			queueMessage(sb.getString());
			return consumables;
		}
		else
		{
			queueMessage("No items");
			return null;
		}
	}
	
	public Equipable[] printEquipables(ItemCollection items)
	{
		int index = 0;
		Equipable[] consumables = new Equipable[50];
		StringBuilder sb = new StringBuilder();
		for (Item i : items.getItems())
		{
			if (DynamicRuntime.isInstance(i, (SClassDesc) MAGIC.clssDesc("Equipable"), false))
			{
				sb.append(index);
				sb.append(": ");
				sb.append(i.name);
				sb.append(" ");
				consumables[index] = (Equipable) i;
				index++;
			}
		}
		if (index > 0)
		{
			queueMessage(sb.getString());
			return consumables;
		}
		else
		{
			queueMessage("No items");
			return null;
		}
	}
	
}
