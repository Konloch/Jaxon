/* Copyright (C) 2007, 2008, 2009, 2015 Stefan Frenz
 *
 * This file is part of SJC, the Small Java Compiler written by Stefan Frenz.
 *
 * SJC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SJC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SJC. If not, see <http://www.gnu.org/licenses/>.
 */

package sjc.osio;

/**
 * TextBuffer: print text to a buffer that is back-readable
 *
 * @author S. Frenz
 * @version 151031 added newline support and implemented decimal int/long output
 * version 090207 added copyright notice
 * version 080712 added reset, replace and toString methods
 * version 080701 moved to osio-package
 * version 070714 changed data to be char instead of byte
 * version 070713 initial version
 */

public class TextBuffer extends TextPrinter
{
	private final static int INITSIZE = 2000;
	
	public char[] data;
	public int used;
	
	public TextBuffer()
	{
		data = new char[INITSIZE];
	}
	
	public void reset()
	{
		used = 0;
	}
	
	public void replace(char old, char now)
	{
		int i = 0;
		
		while (i < used)
		{
			if (data[i] == old)
				data[i] = now;
			i++;
		}
	}
	
	public String toString()
	{
		return new String(data, 0, used);
	}
	
	public void close()
	{
	}
	
	public void print(char c)
	{
		writeByte((int) c & 0xFFFF);
	}
	
	public void print(int i)
	{
		if (i < 0)
		{
			writeByte(45);
			i = -i;
		}
		if (i == 0)
			writeByte(48);
		else
		{
			if (i >= 10)
				print(i / 10);
			writeByte(48 + i % 10);
		}
	}
	
	public void print(long l)
	{
		if (l < 0L)
		{
			writeByte(45);
			l = -l;
		}
		if (l == 0L)
			writeByte(48);
		else
		{
			if (l >= 10L)
				print(l / 10L);
			writeByte(48 + (int) (l % 10L));
		}
	}
	
	public void printHex(int i)
	{
		printHexFix(i, 8);
	}
	
	public void printHexLong(long l)
	{
		printHexFix((int) (l >>> 32), 8);
		printHexFix((int) l, 8);
	}
	
	public void print(String what)
	{
		int i, m;
		
		m = what.length();
		for (i = 0; i < m; i++)
			writeByte(what.charAt(i));
	}
	
	public void println()
	{
		writeByte('\n');
	}
	
	private void writeByte(int what)
	{
		char[] tmp;
		int i;
		
		if (used + 1 == data.length)
		{
			tmp = data;
			data = new char[data.length << 1];
			for (i = 0; i < used; i++)
				data[i] = tmp[i];
		}
		data[used++] = (char) what;
	}
}
