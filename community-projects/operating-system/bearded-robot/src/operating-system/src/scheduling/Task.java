package scheduling;

public abstract class Task
{
	boolean stopped = true;
	
	boolean sticky = false;
	
	void schedule()
	{
		stopped = false;
		onSchedule();
	}
	
	final void start()
	{
		stopped = false;
		onStart();
	}
	
	public final void stop()
	{
		if (!this.sticky)
		{
			onStop();
			stopped = true;
		}
	}
	
	public final boolean isStopped()
	{
		return this.stopped;
	}
	
	public final boolean isSticky()
	{
		return this.sticky;
	}
	
	protected abstract void onSchedule();
	
	protected void onStart()
	{
	}
	
	protected void onStop()
	{
	}
}
