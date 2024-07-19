/* Copyright (C) 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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
 * TextPrinter: interface to formatted output
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 091102 added printSequence
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080712 added getNewFilePrinter
 * version 060613 initial version
 */

public abstract class TextPrinter
{
	public abstract void close();
	
	public abstract void print(int i);
	
	public abstract void print(long l);
	
	public abstract void print(char c);
	
	public abstract void print(String s);
	
	public abstract void println();
	
	public void println(int i)
	{
		print(i);
		println();
	}
	
	public void println(long l)
	{
		print(l);
		println();
	}
	
	public void println(char c)
	{
		print(c);
		println();
	}
	
	public void println(String s)
	{
		print(s);
		println();
	}
	
	public void printHexFix(int i, int l)
	{
		int c, d;
		
		for (c = 0; c < l; c++)
		{
			d = (i >>> ((l - c - 1) << 2)) & 0xF;
			if (d < 10)
				print((char) (d + 48));
			else
				print((char) (d + 55));
		}
	}
	
	public void printSequence(byte[] data, int srcStart, int srcLength, boolean suppressSpecialChars, boolean stopAtLineFeed)
	{
		byte t;
		while (srcLength-- > 0)
		{
			if (data.length > srcStart)
			{
				t = data[srcStart++];
				if (stopAtLineFeed && t == (byte) '\n')
				{
					print("\\...");
					return;
				}
				else if (suppressSpecialChars && t < (byte) 32)
					t = (byte) ' ';
				print((char) t);
			}
			else
				return;
		}
	}
}
