package sysutils;

import sysutils.exec.Executable;

class SchedulerTask
{
	public final Executable exec;
	public final SystemTerminal callee;
	private boolean finished;
	
	SchedulerTask(Executable exec, SystemTerminal callee)
	{
		this.exec = exec;
		this.callee = callee;
		finished = false;
	}
	
	void markAsFinished()
	{
		finished = true;
	}
	
	boolean isFinished()
	{
		return finished;
	}
}
