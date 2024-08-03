package jaxon;

import jaxon.installer.Installer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author Konloch
 * @since 8/1/2024
 */
public class JaxonConstants
{
	public static boolean IS_WINDOWS = Installer.isWindows();
	public static boolean IS_NIX = Installer.isNix();
	public static final File JAXON_ROOT_DIR = Installer.resolveJaxonRoot();
	public static final File JAXON_BIN_DIR = new File(JAXON_ROOT_DIR, "bin");
	public static final File JAXON_JDK_DIR = new File(JAXON_ROOT_DIR, "JDK");
	public static final File JAXON_BINARY_FILE = new File(JAXON_BIN_DIR, IS_WINDOWS ? "jaxon.exe" : "jaxon");
	public static final File API_TOKEN_FILE = new File(JAXON_ROOT_DIR, "API.token");
	public static final boolean API_TOKEN_EXISTS = API_TOKEN_FILE.exists();
	public static String API_TOKEN;
	
	static
	{
		try
		{
			API_TOKEN = API_TOKEN_EXISTS ? new String(Files.readAllBytes(API_TOKEN_FILE.toPath()), StandardCharsets.UTF_8) : null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static final String INCORRECT_USAGE = "Incorrect Usage: Read the documentation at https://konloch.com/Jaxon/tree/056a39d31e89b9d1bc78b14ed9e33dd17a78a040?tab=readme-ov-file#jaxon-command-line";
}
