package jaxon.sjc;

import jaxon.JaxonIO;
import sjc.compbase.Context;
import sjc.osio.sun.ReflectionSymbols;
import sjc.symbols.SymbolFactory;

import java.io.File;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class SJCUtil
{
	public static void sjcCLI(String[] args)
	{
		int res;
		Context ctx = new Context(new JaxonIO(System.out));
		SymbolFactory.preparedReflectionSymbols = new ReflectionSymbols();
		if ((res = ctx.compile(args, "vJRE")) == 0)
			ctx.writeSymInfo();
		
		if (res != 0)
			System.out.println("Results: " + res);
	}
	
	public static String[] convertToAbsoluteFilePath(String[] args)
	{
		for(int i = 0; i < args.length; i++)
		{
			File tempFile = new File(args[i]);
			if(tempFile.exists())
				args[i] = tempFile.getAbsolutePath();
		}
		
		return args;
	}
}
