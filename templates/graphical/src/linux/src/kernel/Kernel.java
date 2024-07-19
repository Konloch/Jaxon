package kernel;

import kernel.linux.LinSystem;
import kernel.linux.Window;

public class Kernel
{
	public static void main()
	{
		System._system = new LinSystem();
		Window.run();
	}
}