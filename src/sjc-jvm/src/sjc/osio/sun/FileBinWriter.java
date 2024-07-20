/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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

import sjc.osio.BinWriter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * FileBinWriter: write data to binary file
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 060628 adapted changed BinWriter
 * version 060613 adapted to osio-interfaces
 * version 060607 initial version
 */

public class FileBinWriter extends BinWriter
{
	private RandomAccessFile file;
	private boolean error;
	
	public FileBinWriter()
	{
		file = null;
		error = false;
	}
	
	public boolean open(String fname)
	{
		try
		{
			file = new RandomAccessFile(new File(fname).getAbsolutePath(), "rw");
			file.setLength(0);
		}
		catch (IOException e)
		{
			return false;
		}
		return true;
	}
	
	public void close()
	{
		if (file != null)
		{
			try
			{
				file.close();
			}
			catch (IOException e)
			{
			}
			file = null;
		}
	}
	
	public boolean write(byte[] what, int offset, int len)
	{
		if (error)
			return false;
		try
		{
			file.write(what, offset, len);
		}
		catch (IOException e)
		{
			error = true;
			return false;
		}
		return true;
	}
	
	public boolean setSize(int addBytes)
	{
		try
		{
			file.setLength(file.getFilePointer() + addBytes);
		}
		catch (IOException e)
		{
			error = true;
			return false;
		}
		return true;
	}
}
