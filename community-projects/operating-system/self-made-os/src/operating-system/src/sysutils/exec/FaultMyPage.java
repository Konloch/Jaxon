package sysutils.exec;


//forces a page fault by accessing first or last page
class FaultMyPage extends Executable
{
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new FaultMyPage();
			}
			
			@Override
			public String getName()
			{
				return "faultme";
			}
		});
	}
	
	@Override
	public int execute()
	{
		int addr = 0x0;
		//for last page
		if (args.length > 0 && args[0].equals("-l"))
		{
			addr = 0xFFFFFFFF;
		}
		if (args.length > 0 && args[0].equals("-w"))
		{
			MAGIC.wMem32(addr, 0);
		}
		int i = MAGIC.rMem32(addr);
		return 0;
	}
}