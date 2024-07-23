package os.commands;

import os.screen.Terminal;
import os.tasks.CommandTask;

public class Echo extends CommandTask
{
	private String[] args;
	
	private int i;
	
	public Echo(Terminal out)
	{
		super("echo", "Echo arguments", out);
	}
	
	@Override
	public void run()
	{
		if (out == null || args == null || this.isDone())
			return;
		
		out.print(this.args[i]);
		if (++i < this.args.length)
		{
			out.print(' ');
		}
		else
		{
			out.println();
			this.setDone(true);
		}
	}
	
	@Override
	public void setup(String[] args)
	{
		this.args = args;
		this.i = 1;
		this.setDone(args.length == 0);
	}
}
