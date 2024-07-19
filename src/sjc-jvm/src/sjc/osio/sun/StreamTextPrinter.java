/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.osio.sun;

import sjc.osio.TextPrinter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * StreamTextPrinter: formatted output to screen or file
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100422 added parameter to enable different stdOut OutputStreams
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080820 added FileNotFound handling
 * version 080712 renamed toFile to TextPrinter.getNewFilePrinter
 * version 070817 made creation of PrintStream compatible with jdk1.4.2
 * version 070713 removed unused methods to print float and double
 * version 070312 fixed print(long)
 * version 060613 adapted to osio-interfaces
 * version 060607 initial version
 */

public class StreamTextPrinter extends TextPrinter
{
	private PrintStream ps;
	private final boolean canClose;
	
	public StreamTextPrinter(String filename, OutputStream stdOut)
	{ //default: to standard out
		if (filename == null)
		{
			ps = new PrintStream(stdOut);
			canClose = false;
		}
		else
		{
			try
			{
				ps = new PrintStream(new FileOutputStream(filename));
			}
			catch (FileNotFoundException e)
			{
				System.out.println("could not create output file " + filename + ": " + e.getMessage());
				System.exit(-1);
			}
			canClose = true;
		}
	}
	
	public void close()
	{
		if (canClose)
			ps.close();
	}
	
	public void print(int i)
	{
		ps.print(i);
	}
	
	public void print(long l)
	{
		ps.print("0x");
		printHexFix((int) (l >>> 32), 8);
		printHexFix((int) l, 8);
	}
	
	public void print(char c)
	{
		ps.print(c);
	}
	
	public void print(String s)
	{
		ps.print(s);
	}
	
	public void println()
	{
		ps.println();
	}
}
