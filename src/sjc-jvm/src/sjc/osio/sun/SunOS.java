/* Copyright (C) 2009, 2010 Stefan Frenz
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

import sjc.compbase.StringList;
import sjc.osio.BinWriter;
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;

import java.io.*;

/**
 * SunOS: operating system interface implementation for Sun Java runtime environment
 *
 * @author S. Frenz
 * @version 100513 adopted changed OsIO
 * version 100422 adopted changed StreamTextPrinter and added constructor to set stdOut stream
 * version 090916 Exclude files starting with a "."
 * version 090311 added getTimeInfo
 * version 090303 initial version
 */

public class SunOS extends OsIO
{
	private StringList result;
	private final OutputStream stdOut;
	
	public SunOS(OutputStream streamAsStdOut)
	{
		stdOut = streamAsStdOut;
	}
	
	public BinWriter getNewBinWriter()
	{
		return new FileBinWriter();
	}
	
	public TextPrinter getNewFilePrinter(String filename)
	{
		return new StreamTextPrinter(filename, stdOut);
	}
	
	public boolean isDir(String name)
	{
		return (new File(name)).isDirectory();
	}
	
	public StringList listDir(String name, boolean recurse)
	{
		result = null;
		appendDir(new File(name), recurse);
		return result;
	}
	
	public byte[] readFile(String fname)
	{
		InputStream is;
		int cnt;
		byte[] data = null;
		
		is = null;
		try
		{
			is = new FileInputStream(fname);
			cnt = is.available();
			data = new byte[cnt];
			if (is.read(data, 0, cnt) != cnt)
			{
				is.close();
				return null;
			}
			is.close();
		}
		catch (IOException e)
		{
			return null;
		}
		return data;
	}
	
	public long getTimeInfo()
	{
		return System.nanoTime();
	}
	
	private void appendDir(File dir, boolean recurse)
	{
		File[] entries;
		StringList newFile;
		int i;
		
		entries = dir.listFiles();
		for (i = 0; i < entries.length; i++)
		{
			if (entries[i].getName().startsWith("."))
				continue;
			if (entries[i].isDirectory())
			{
				if (recurse)
					appendDir(entries[i], true);
			}
			else
			{
				try
				{
					newFile = new StringList(entries[i].getCanonicalPath());
					newFile.next = result;
					result = newFile;
				}
				catch (IOException e)
				{
					System.out.print("IO-error: " + e.getMessage());
				}
			}
		}
	}
}
