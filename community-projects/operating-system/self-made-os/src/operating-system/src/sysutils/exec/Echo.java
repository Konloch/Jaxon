package sysutils.exec;

import graphics.Console;
import sysutils.Scheduler;

class Echo extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new Echo();
			}
			
			@Override
			public String getName()
			{
				return "echo";
			}
		});
	}
	
	@Override
	public int execute()
	{
		for (int i = 0; i < args.length; i++)
		{
			Console.print(args[i]);
			if (i != args.length - 1)
				Console.print(" ");
		}
		Console.println();
		Scheduler.markTaskAsFinished(this);
		return 0;
	}
	
}
