package kernel;

import app.AppEntry;
import kernel.linux.LinSystem;

/**
 * @author S. Frenz
 */
public class Kernel
{
	public static void main()
	{
		System._system = new LinSystem();
		AppEntry.start();
	}
}