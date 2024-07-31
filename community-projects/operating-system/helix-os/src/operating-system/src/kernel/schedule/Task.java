package kernel.schedule;

public abstract class Task
{
	public final int id;
	public final String name;
	protected boolean _active;
	protected boolean _running;
	
	public Task(String name)
	{
		id = nextId();
		this.name = name;
		_active = false;
		_running = false;
	}
	
	public void register()
	{
		Scheduler.AddTask(this);
	}
	
	public void removeFromExec()
	{
		Scheduler.RemoveTask(this);
	}
	
	public final void runTask()
	{
		_active = true;
		run();
		_active = false;
	}
	
	public abstract void run();
	
	private static int _idC = 0;
	
	protected static int nextId()
	{
		return _idC++;
	}
}
