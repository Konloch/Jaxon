package sysutils.exec;

import sysutils.Scheduler;

class Debug extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new Debug();
			}
			
			@Override
			public String getName()
			{
				return "debughalt";
			}
		});
	}
	
	@Override
	public int execute()
	{
		MAGIC.inline(0xCC);
		Scheduler.markTaskAsFinished(this);
		return -1;
	}
	
}
