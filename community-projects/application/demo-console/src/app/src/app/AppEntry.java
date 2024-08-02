package app;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class AppEntry
{
	public static void start(String[] args)
	{
		System.out.print(new StringBuilder("Hello world: ").append(args.length));
		
		for(String s : args)
			System.out.println(new StringBuilder("Arg: ").append(s));
	}
}