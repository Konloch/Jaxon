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

package sjc.osio;

/**
 * TextReader: interface to read text
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100513 changed initData and removed abstract modifier
 * version 100114 removed len as data now must not contain extra bytes
 * version 091109 fixed column
 * version 091102 made pos and data public to be read outside package
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080202 removed eof as it is not required anymore
 * version 070331 got system independent code
 * version 060712 streamlined version
 * version 060607 initial version
 */

public class TextReader
{
	public char nextChar, lookAhead;
	public int line, col, pos;
	public byte[] data;
	
	protected boolean errorOccurred;
	
	public boolean initData(byte[] idata)
	{
		line = 1;
		col = 1;
		pos = 0;
		nextChar = '\0';
		lookAhead = '\0';
		if ((data = idata) == null)
		{
			errorOccurred = true;
			return false;
		}
		if (data.length >= 1)
		{
			nextChar = (char) ((int) data[0] & 0xFF);
			if (data.length >= 2)
				lookAhead = (char) ((int) data[1] & 0xFF);
		}
		return true;
	}
	
	public void readChar()
	{
		if (errorOccurred)
			return;
		pos++;
		switch (nextChar = lookAhead)
		{
			case '\n':
				line++;
				col = 0;
				break;
			case '\0':
				if (pos > data.length)
					errorOccurred = true;
				return;
			case '\r':
				break;
			default:
				col++;
		}
		if (pos + 1 >= data.length)
			lookAhead = '\0';
		else
			lookAhead = (char) data[pos + 1];
	}
}
