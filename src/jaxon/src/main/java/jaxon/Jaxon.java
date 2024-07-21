package jaxon;

import jaxon.build.Build;
import jaxon.jdk.JDKUtil;
import jaxon.sjc.SJCUtil;
import jaxon.templates.TemplateUtil;
import jaxon.zip.ZipUtil;

import java.io.*;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Jaxon
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.out.println("Incorrect Usage: Read the documentation at https://konloch.com/Jaxon");
			return;
		}
		
		String command = args[0];
		
		if (command.equalsIgnoreCase("sjc"))
		{
			int newArgumentsLength = args.length - 1;
			String[] trimmedArgs = new String[newArgumentsLength];
			int i;
			for (i = 2; i < args.length; i++)
			{
				int normalizedIndex = i - 1;
				trimmedArgs[normalizedIndex] = args[i];
			}
			
			SJCUtil.sjcCLI(trimmedArgs);
		}
		else if (command.equalsIgnoreCase("build"))
			Build.buildCLI(args);
		else if (command.equalsIgnoreCase("template"))
			TemplateUtil.templateCLI(args);
		else if (command.equalsIgnoreCase("zip"))
			ZipUtil.zipCLI(args);
		else if (command.equalsIgnoreCase("jdk"))
			JDKUtil.jdkCLI(args);
		else
			System.out.println("Read the documentation at https://konloch.com/Jaxon");
	}
}
