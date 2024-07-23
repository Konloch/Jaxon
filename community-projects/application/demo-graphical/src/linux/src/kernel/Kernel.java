package kernel;

import kernel.linux.LinSystem;
import kernel.linux.Window;

/**
 * @author S. Frenz
 */
public class Kernel
{
	public static void main()
	{
		System._system = new LinSystem();
		Window.run();
	}
}