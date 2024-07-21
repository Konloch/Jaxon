package app;

import java.io.ByteArrayInputStream;

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
			System._system.createDirectory("test");
			System._system.rename("test", "new_name");
			System._system.write("test.txt", 0, output, true);
		}
		catch (Exception e)
		{
			System.out.println("Error Caught");
		}
	}
}
