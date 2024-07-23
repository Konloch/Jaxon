package sysutils;

import hardware.keyboard.Keyboard;
import hardware.keyboard.KeyboardEvent;
import rte.DynamicRuntime;
import sysutils.exec.Executable;

public class Scheduler
{
	private static final int MAX_TASKS = 100;
	private static final int MAX_TERMINALS = 10;
	private static SchedulerTask[] runningTasks;
	private static SystemTerminal currentTerminal;
	private static SystemTerminal[] terminalList;
	
	public static void init()
	{
		runningTasks = new SchedulerTask[MAX_TASKS];
		terminalList = new SystemTerminal[MAX_TERMINALS];
		//populate terminalList
		//0-9 are for CTRL + ALT + 1-9, 0
		for (int i = 0; i < MAX_TERMINALS; i++)
		{
			terminalList[i] = new SystemTerminal();
		}
		currentTerminal = terminalList[0];
	}
	
	public static void startScheduling()
	{
		currentTerminal.init();
		while (true)
		{
			//first, forward all inputs to current terminal
			forwardInputs();
			//call the current terminal
			currentTerminal.focus();
			//execute any tasks that are not marked as finished
			executeActiveTasks();
			
			//run garbage collection
			DynamicRuntime.collectGarbage();
		}
	}
	
	public static SchedulerTask addTask(Executable exec, SystemTerminal callee)
	{
		SchedulerTask newTask = new SchedulerTask(exec, callee);
		for (int i = 0; i < MAX_TASKS; i++)
		{
			if (runningTasks[i] == null || runningTasks[i].isFinished())
			{
				runningTasks[i] = newTask;
				return newTask;
			}
		}
		//we have reached our maximum for tasks, abort
		//TODO: this shouldn't really happen, but maybe there should be proper handling for this
		MAGIC.inline(0xCC);
		return null;
	}
	
	public static void markTaskAsFinished(Executable exec)
	{
		for (int i = 0; i < MAX_TASKS; i++)
		{
			if (runningTasks[i] != null && runningTasks[i].exec == exec)
			{
				runningTasks[i].markAsFinished();
				return;
			}
		}
		//tried to mark a task as done that doesn't exist
		MAGIC.inline(0xCC);
	}
	
	public static void executeActiveTasks()
	{
		for (int i = 0; i < MAX_TASKS; i++)
		{
			if (runningTasks[i] != null && !runningTasks[i].isFinished())
			{
				runningTasks[i].exec.execute();
			}
		}
	}
	
	//forwards new input from the Keyboard buffer to the current terminal
	public static void forwardInputs()
	{
		Keyboard.processInputBuffer();
		while (Keyboard.eventAvailable())
		{
			KeyboardEvent kev = Keyboard.getNextKeyboardEvent();
			currentTerminal.buffer.writeEvent(kev);
		}
	}
	
	public static void setCurrentTerminal(int terminalID)
	{
		currentTerminal.storeMyMem();
		currentTerminal = terminalList[terminalID];
		currentTerminal.restoreMyMem();
		currentTerminal.init();
	}
	
}
