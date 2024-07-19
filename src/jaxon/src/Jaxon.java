import sjc.compbase.Context;
import sjc.osio.sun.ReflectionSymbols;
import sjc.osio.sun.SunOS;
import sjc.symbols.SymbolFactory;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Jaxon
{
	public static void main(String[] args)
	{
		if(args.length <= 2)
		{
			System.out.println("Incorrect Usage: jaxon template console | jaxon sc -s 1m -a 4198912 -l -o boot -O #win [input-files]");
			return;
		}
		
		String command = args[0];
		
		if(command.equalsIgnoreCase("sc"))
			sfc(args);
		else if(command.equalsIgnoreCase("build"))
			sfc(args);
		else if(command.equalsIgnoreCase("template"))
			template(args);
	}
	
	private static void sfc(String[] args)
	{
		int newArgumentsLength = args.length-1;
		String[] newArguments = new String[newArgumentsLength];
		int i;
		for(i = 2; i < args.length; i++)
		{
			int normalizedIndex = i - 1;
			newArguments[normalizedIndex] = args[i];
		}
		
		int res;
		Context ctx = new Context(new JaxonIO(System.out));
		SymbolFactory.preparedReflectionSymbols = new ReflectionSymbols();
		if ((res = ctx.compile(newArguments, "vJRE")) == 0)
			ctx.writeSymInfo();
		System.exit(res);
	}
	
	private static void template(String[] args)
	{
		String command = args[0];
	}
}
