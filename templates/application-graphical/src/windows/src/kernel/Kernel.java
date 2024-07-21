package kernel;

import kernel.windows.WinSystem;
import kernel.windows.Window;

/**
 * @author S. Frenz
 */
public class Kernel
{
	public static void main()
	{
		System._system = new WinSystem();
		Window.run();
	}
}