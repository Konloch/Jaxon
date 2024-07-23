package sysutils.exec;

public abstract class ExecutableFactory
{
	public abstract Executable createExecutable();
	
	public abstract String getName();
}
