package sysutils.exec;

import graphics.Console;
import sysutils.Scheduler;

public class ClearConsole extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new ClearConsole();
			}
			
			@Override
			public String getName()
			{
				return "clear";
			}
		});
	}
	
	@Override
	public int execute()
	{
		Console.clearConsole();
		Scheduler.markTaskAsFinished(this);
		return 0;
	}
	
}
