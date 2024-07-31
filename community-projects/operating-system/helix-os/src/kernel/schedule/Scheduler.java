package kernel.schedule;

import arch.x86;
import kernel.Kernel;
import kernel.memory.MemoryManager;
import kernel.trace.logging.Logger;

public class Scheduler
{
	public static final int MAX_TASKS = 4096;
	private static Task[] _tasks;
	private static int _taskCount;
	private static Task _currentTask;
	private static int _storeEbp, _storeEsp;
	
	public static void initialize()
	{
		_tasks = new Task[MAX_TASKS];
		_taskCount = 0;
		_currentTask = null;
	}
	
	public static void addTask(Task task)
	{
		Logger.info("SCHED", "Added Task ".append(task.name));
		
		if (_taskCount >= MAX_TASKS)
			Kernel.panic("Too many tasks");
		
		_tasks[_taskCount++] = task;
	}
	
	public static void removeTask(Task task)
	{
		Logger.info("SCHED", "Removing Task ".append(task.name));
		for (int i = 0; i < _taskCount; i++)
		{
			if (_tasks[i] == task)
			{
				_tasks[i] = null;
				
				for (int j = i; j < _taskCount - 1; j++)
					_tasks[j] = _tasks[j + 1];
				
				_taskCount--;
				return;
			}
		}
	}
	
	public static int getTaskId(String name)
	{
		for (int i = 0; i < _taskCount; i++)
		{
			if (_tasks[i].name == name)
				return _tasks[i].id;
		}
		return -1;
	}
	
	public static void run()
	{
		Logger.info("SCHED", "Starting Schedeuler");
		
		MemoryManager.EnableGarbageCollection();
		Logger.info("SCHED", "Enabled Garbage Collection");
		
		MAGIC.inline(0x89, 0x2D);
		MAGIC.inlineOffset(4, _storeEbp); // mov [addr(v1)],ebp
		MAGIC.inline(0x89, 0x25);
		MAGIC.inlineOffset(4, _storeEsp); // mov [addr(v1)],esp
		
		loop();
	}
	
	private static void loop()
	{
		while (true)
		{
			for (int i = 0; i < _taskCount; i++)
			{
				_currentTask = _tasks[i];
				_currentTask.run();
				
			}
			
			if (MemoryManager.ShouldCollectGarbage())
				MemoryManager.TriggerGarbageCollection();
			
			// x86.hlt();
		}
	}
	
	public static void taskBreak()
	{
		if (_currentTask == null)
			return;
		
		removeTask(_currentTask);
		_currentTask = null;
		
		MAGIC.inline(0x8B, 0x2D);
		MAGIC.inlineOffset(4, _storeEbp); // mov ebp,[addr(v1)]
		MAGIC.inline(0x8B, 0x25);
		MAGIC.inlineOffset(4, _storeEsp); // mov esp,[addr(v1)]
		
		x86.sti();
		
		loop();
	}
	
	public static int getTaskCount()
	{
		return _taskCount;
	}
}
