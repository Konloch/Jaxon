package kernel;

import app.AppEntry;
import kernel.linux.LinSystem;

public class Kernel
{
	public static void main()
	{
		System._system = new LinSystem();
		AppEntry.start();
	}
}