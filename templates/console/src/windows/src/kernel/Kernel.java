package kernel;

import app.AppEntry;
import kernel.windows.WinSystem;

public class Kernel
{
	public static void main()
	{
		System._system = new WinSystem();
		AppEntry.start();
	}
}