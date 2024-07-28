package kernel;

import app.AppEntry;
import kernel.windows.Win32;
import kernel.windows.WinSystem;

/**
 * @author S. Frenz
 */
public class Kernel
{
	public static void main()
	{
		System._system = new WinSystem();
		AppEntry.start(System._system.getCommandLineArgs());
	}
}