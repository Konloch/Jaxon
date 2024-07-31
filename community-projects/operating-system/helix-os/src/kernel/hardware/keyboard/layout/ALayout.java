package kernel.hardware.keyboard.layout;

public abstract class ALayout
{
	public abstract char logicalKey(int physicalKey, boolean shift, boolean alt);
}
