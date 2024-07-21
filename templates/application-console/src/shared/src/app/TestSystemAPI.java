package app;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class TestSystemAPI
{
	public static void testFileSystem()
	{
		try
		{
			ByteArrayInputStream output = new ByteArrayInputStream("Hello World\n".toByteArray());
			
			File newNameFolder = new File("new_name");
			newNameFolder.delete();
			
			File testFolder = new File("test");
			testFolder.mkdir();
			testFolder.rename("new_name");
			
			System._system.write("test.txt", 0, output, true);
		}
		catch (Exception e)
		{
			System.out.println("Error Caught");
		}
	}
}
