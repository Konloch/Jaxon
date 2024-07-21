package app;

import java.io.ByteArrayOutputStream;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class TestSystemAPI
{
	public static void testFileSystem()
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream("Hello World\n".toByteArray());
		System._system.write("test.txt", 0, output, true);
	}
}
