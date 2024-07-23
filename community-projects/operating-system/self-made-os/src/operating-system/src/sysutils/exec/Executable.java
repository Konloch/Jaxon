package sysutils.exec;

import hardware.keyboard.KeyboardEventRingBuffer;

public abstract class Executable
{
	protected String[] args;
	public boolean acceptsKeyboardInputs = false;
	public KeyboardEventRingBuffer buffer = new KeyboardEventRingBuffer();
	
	public void setArgs(String[] args)
	{
		if (this.args == null)
			this.args = args;
		else
		{
			//tried to add args even tho they are already initialized
			MAGIC.inline(0xCC);
		}
	}
	
	public abstract int execute();
}
