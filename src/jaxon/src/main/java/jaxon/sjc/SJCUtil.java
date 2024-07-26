package jaxon.sjc;

import jaxon.JaxonIO;
import jaxon.build.BuildUtil;
import sjc.compbase.Context;
import sjc.osio.sun.ReflectionSymbols;
import sjc.symbols.SymbolFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class SJCUtil
{
	public static void sjcJaxon(String[] args)
	{
		String[] trimmedArgs = new String[args.length-1];
		System.arraycopy(args, 1, trimmedArgs, 0, trimmedArgs.length);
		sjcCLI(SJCUtil.convertToAbsoluteFilePath(trimmedArgs));
	}
	
	public static void sjcJaxonEnvironment(String[] args) throws IOException
	{
		String env = args[1];
		String input = args[2];
		String output = args[3];
		
		String[] trimmedArgs = new String[args.length-4];
		System.arraycopy(args, 4, trimmedArgs, 0, trimmedArgs.length);
		trimmedArgs = SJCUtil.convertToAbsoluteFilePath(trimmedArgs);
		
		//setup env
		BuildUtil.setupEnv(env);
		
		//run SJC
		sjcCLI(trimmedArgs);
		
		//export build files
		BuildUtil.exportBuild(input, "build/operating-system/" + output);
		BuildUtil.exportBuild("syminfo.txt", "build/build_sym_info.txt");
	}
	
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
