package jaxon.templates;

import jaxon.zip.ZipUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class TemplateUtil
{
	public static void templateCLI(String[] args) throws IOException
	{
		String template = args[1];
		
		if (template.equalsIgnoreCase("console"))
			createTemplate("console");
		else if (template.equalsIgnoreCase("graphical"))
			createTemplate("graphical");
		else if (template.equalsIgnoreCase("operating-system"))
			createTemplate("operating-system-demo");
		else if (template.equalsIgnoreCase("operating-system-hello-world"))
			createTemplate("operating-system-hello-world");
		else if (template.equalsIgnoreCase("atmega"))
		{
			createTemplate("atmega");
			String warning = "WARNING: ATmega currently fails to build - this will need to be resolved";
			System.err.println(warning);
			System.out.println(warning);
		}
		else if (template.equalsIgnoreCase("barebones"))
		{
			createTemplate("barebones");
			String warning = "WARNING: You should instead use the template 'console' unless you know what you're doing";
			System.err.println(warning);
			System.out.println(warning);
		}
	}
	
	public static void createTemplate(String path)
	{
		if (ZipUtil.unzipFolder("/templates/" + path + ".zip", path))
		{
			File location = new File(path);
			System.out.println("Added the " + path + " template into " + location.getAbsolutePath());
			System.out.println("Open that folder with your favorite Java IDE (Intellij is supported)");
			System.out.println("Build using the build scripts provided by the templates");
		}
	}
}
