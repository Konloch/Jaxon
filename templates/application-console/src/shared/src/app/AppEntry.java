package app;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class AppEntry
{
	public static void start(String[] args)
	{
		for(String s : args)
			System.out.println(new StringBuilder("Arg: ").append(s));
		
		System.out.print(new StringBuilder("Hello world: ").append(args.length));
		TestSystemAPI.testFileSystem();
	}
}