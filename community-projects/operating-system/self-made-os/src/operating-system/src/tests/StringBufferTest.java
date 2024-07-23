package tests;

import hardware.Serial;

public class StringBufferTest
{
	public static boolean test()
	{
		//test single append
		StringBuilder sb = new StringBuilder();
		sb.append('f');
		if (!sb.getString().equals("f"))
		{
			fail("append char");
			return false;
		}
		sb = new StringBuilder();
		sb.append(123);
		if (!sb.getString().equals("123"))
		{
			fail("append int, expected 123 got: ".concat(sb.getString()));
			return false;
		}
		sb = new StringBuilder();
		sb.append((byte) 45);
		if (!sb.getString().equals("45"))
		{
			fail("append byte");
			return false;
		}
		sb = new StringBuilder();
		sb.append((long) 1234567890);
		if (!sb.getString().equals("1234567890"))
		{
			fail("append long");
			return false;
		}
		sb = new StringBuilder();
		sb.append((short) 789);
		if (!sb.getString().equals("789"))
		{
			fail("append short");
			return false;
		}
		sb = new StringBuilder();
		char[] c = new char[3];
		c[0] = 'a';
		c[1] = 'b';
		c[2] = 'c';
		sb.append(c);
		if (!sb.getString().equals("abc"))
		{
			fail("append char array");
			return false;
		}
		sb = new StringBuilder();
		sb.append("test");
		if (!sb.getString().equals("test"))
		{
			fail("append string");
			return false;
		}
		//TODO: capacity tests, and appending multiple times
		Serial.println("StringBuilder test successful.");
		return true;
	}
	
	private static void fail(String reason)
	{
		Serial.print("failed StringBuilder test: ".concat(reason));
	}
}
