package sysutils;

import graphics.Console;
import graphics.ConsoleColors;
import graphics.VideoCharCopy;
import graphics.VideoMemory;
import hardware.keyboard.Key;
import hardware.keyboard.KeyboardEvent;
import hardware.keyboard.KeyboardEventRingBuffer;
import sysutils.exec.Executable;
import sysutils.exec.ExecutableStore;
import utils.ASCIIControlSequences;
import utils.ArrayUtils;

public class SystemTerminal
{
	private static final int INPUT_BUFFER_SIZE = 3200;
	private static final int COMMAND_HISTORY_SIZE = 20;
	KeyboardEventRingBuffer buffer = new KeyboardEventRingBuffer();
	private char[] inputBuffer = new char[INPUT_BUFFER_SIZE];
	private final char[][] commandHistory = new char[COMMAND_HISTORY_SIZE][INPUT_BUFFER_SIZE];
	private int inputBufferPointer = 0;
	private int commandHistoryWritePointer = 0;
	//always points to the command history entry that should be read next, or -1 if the history is still empty
	private int commandHistoryReadPointer = -1;
	//whether or not the current command is loaded history
	private boolean lastCommand = false;
	private SchedulerTask activeTask;
	private boolean firstCall = true;
	VideoCharCopy[] myVidMem;
	int myXPos, myYPos;
	
	//display a nice splash and a prompt
	public void init()
	{
		if (firstCall)
		{
			firstCall = false;
			Console.setColor(ConsoleColors.FG_LIGHTCYAN, ConsoleColors.BG_BLACK, false);
			Console.clearConsole();
			//top line
			printBlock(80);
			//2nd line
			printBlock(29);
			Console.print((char) 0xC9);
			for (int i = 0; i < 20; i++)
			{
				Console.print((char) 0xCD);
			}
			Console.print((char) 0xBB);
			printBlock(29);
			//middle line
			printBlock(29);
			Console.print((char) 0xBA);
			Console.print(" Welcome to ClubOS! ");
			Console.print((char) 0xBA);
			printBlock(29);
			//4th line
			printBlock(29);
			Console.print((char) 0xC8);
			for (int i = 0; i < 20; i++)
			{
				Console.print((char) 0xCD);
			}
			Console.print((char) 0xBC);
			printBlock(29);
			//bottom line
			printBlock(80);
			Console.setDefaultColor();
			
			printPrompt();
		}
	}
	
	private void printBlock(int blocks)
	{
		for (int i = 0; i < blocks; i++)
		{
			Console.print((char) 0xDB);
		}
	}
	
	private void printPrompt()
	{
		Console.print(">");
	}
	
	//takes over control and starts handling keyboard inputs
	public void focus()
	{
		if (activeTask != null)
		{
			if (activeTask.isFinished())
			{
				//remove the active task and continue, as it is finished
				activeTask = null;
				printPrompt();
			}
			else
			{
				//we have to wait for the task to finished, pass on inputs if necessary
				while (buffer.canRead() && activeTask.exec.acceptsKeyboardInputs)
				{
					activeTask.exec.buffer.writeEvent(buffer.readEvent());
				}
			}
		}
		while (buffer.canRead())
		{
			KeyboardEvent kev = buffer.readEvent();
			switch (kev.KEYCODE)
			{ //special handling
				case Key.BACKSPACE:
				{
					if (inputBufferPointer > 0)
					{
						inputBuffer[--inputBufferPointer] = '\u0000'; //reset to zerovalue
						Console.print(ASCIIControlSequences.BACKSPACE);
						lastCommand = false;
					}
					continue;
				}
				case Key.ENTER:
				{
					Console.print(ASCIIControlSequences.LINE_FEED);
					if (!isBufferWhitespace())
					{ //buffer may contain a command, we have to check
						//first, turn our buffer contents into a string and split it on spaces
						String buf = String.compactString(inputBuffer);
						String[] split = buf.split(' ', buf);
						if (split.length > 0)
						{
							Executable ex = ExecutableStore.fetchExecutable(split[0]);
							if (ex != null)
							{
								addCommandToHistory();
								String[] args = ArrayUtils.subArray(split, 1, split.length);
								//TODO: add event to the scheduler and pass arguments
								ex.setArgs(args);
								activeTask = Scheduler.addTask(ex, this);
							}
							else
							{
								printExNotFound(split[0]);
							}
						}
					}
					updateHistoryPointer();
					clearInputBuffer();
					continue;
				}
				case Key.UP_ARROW:
				{
					char[] temp = getNextCommandHistoryEntry();
					showCommandHistoryEntry(temp);
					continue;
				}
				case Key.DOWN_ARROW:
				{
					char[] temp = getPreviousCommandHistoryEntry();
					showCommandHistoryEntry(temp);
					continue;
				}
			}
			if ((kev.KEYCODE == Key.D || kev.KEYCODE == Key.d) && kev.CONTROL && kev.SHIFT)
			{ //breakpoint
				MAGIC.inline(0xCC);
			}
			if ((kev.KEYCODE == Key.C || kev.KEYCODE == Key.c) && kev.CONTROL)
			{ //clear line
				clearInputBuffer();
				Console.print("^C\n");
				printPrompt();
				continue;
			}
			if (kev.CONTROL && kev.SHIFT)
			{
				//set correct terminal
				int terminalNum = kev.KEYCODE - 48;
				if (terminalNum >= 0 && terminalNum <= 9)
					Scheduler.setCurrentTerminal(terminalNum);
				return;
			}
			if (kev.KEYCODE >= Key.SPACE && kev.KEYCODE <= Key.TILDE)
			{ //printable ascii, straight up cast and push
				inputBuffer[inputBufferPointer] = (char) (kev.KEYCODE & 0xFF);
				inputBufferPointer++;
				lastCommand = false;
				Console.print((char) (kev.KEYCODE & 0xFF));
				continue;
			}
			
		}
	}
	
	private void updateHistoryPointer()
	{
		commandHistoryReadPointer = commandHistoryWritePointer - 1;
	}
	
	private void addCommandToHistory()
	{
		if (!lastCommand)
		{
			if (commandHistoryWritePointer == COMMAND_HISTORY_SIZE)
			{
				//history is full, move everything one over
				for (int i = 0; i < COMMAND_HISTORY_SIZE - 1; i++)
				{
					commandHistory[i] = commandHistory[i + 1];
				}
				for (int i = 0; i < inputBufferPointer; i++)
				{
					commandHistory[COMMAND_HISTORY_SIZE - 1][i] = inputBuffer[i];
				}
				//
				commandHistoryReadPointer = COMMAND_HISTORY_SIZE - 1;
				return;
			}
			//there is space in the history
			for (int i = 0; i < inputBufferPointer; i++)
			{
				commandHistory[commandHistoryWritePointer][i] = inputBuffer[i];
			}
			commandHistoryReadPointer = commandHistoryWritePointer;
			commandHistoryWritePointer++;
		}
		
	}
	
	private char[] getNextCommandHistoryEntry()
	{
		if (commandHistoryReadPointer == -1)
			return null;
		char[] temp = copyCommandHistory(commandHistoryReadPointer);
		lastCommand = (commandHistoryReadPointer == commandHistoryWritePointer - 1);
		commandHistoryReadPointer--;
		return temp;
	}
	
	private char[] getPreviousCommandHistoryEntry()
	{
		if (commandHistoryReadPointer + 2 > commandHistoryWritePointer)
		{
			//out of bounds
			return null;
		}
		else if (commandHistoryReadPointer + 2 == commandHistoryWritePointer)
		{
			//go back to empty command line
			commandHistoryReadPointer++;
			return new char[INPUT_BUFFER_SIZE];
		}
		char[] temp = copyCommandHistory(commandHistoryReadPointer + 2);
		lastCommand = (commandHistoryReadPointer + 2 == commandHistoryWritePointer - 1);
		commandHistoryReadPointer++;
		return temp;
	}
	
	private char[] copyCommandHistory(int index)
	{
		char[] temp = new char[INPUT_BUFFER_SIZE];
		for (int i = 0; i < INPUT_BUFFER_SIZE; i++)
		{
			temp[i] = commandHistory[index][i];
		}
		return temp;
	}
	
	private void showCommandHistoryEntry(char[] temp)
	{
		if (temp != null)
		{
			inputBuffer = temp;
			clearLine();
			printPrompt();
			//restore inputBufferPointer
			for (inputBufferPointer = 0; inputBufferPointer < INPUT_BUFFER_SIZE; inputBufferPointer++)
			{
				if (inputBuffer[inputBufferPointer] == 0)
					break;
			}
			//print inputBufferPointer
			for (int i = 0; i < inputBufferPointer; i++)
			{
				Console.print(inputBuffer[i]);
			}
		}
	}
	
	//returns whether or not the buffer only consists of whitespace,
	public boolean isBufferWhitespace()
	{
		int i = 0;
		while (i < inputBufferPointer)
		{
			if (inputBuffer[i] != ' ' && inputBuffer[i] != '\u0000')
				return false;
			i++;
		}
		return true;
	}
	
	private void printErrorCode(int error)
	{
		//TODO: implement
	}
	
	private void printExNotFound(String name)
	{
		Console.print("no executable ".concat(name).concat(" found\n"));
		printPrompt();
	}
	
	private void clearInputBuffer()
	{
		for (int i = 0; i < INPUT_BUFFER_SIZE; i++)
		{
			inputBuffer[i] = 0;
		}
		inputBufferPointer = 0;
	}
	
	private void clearLine()
	{
		Console.print(ASCIIControlSequences.CARRIAGE_RETURN);
		for (int i = 0; i < VideoMemory.VIDEO_MEMORY_COLUMNS; i++)
		{
			Console.print(ASCIIControlSequences.SPACE);
		}
		Console.setCursor(Console.getXPos(), Console.getYPos() - 1);
	}
	
	void storeMyMem()
	{
		myVidMem = Console.getCurrentVideoMemory();
		myXPos = Console.getXPos();
		myYPos = Console.getYPos();
	}
	
	void restoreMyMem()
	{
		if (myVidMem != null)
		{
			Console.writeVideoMemory(myVidMem);
			myVidMem = null;
		}
		Console.setPos(myXPos, myYPos);
		Console.setCursor(myXPos, myYPos);
	}
}
