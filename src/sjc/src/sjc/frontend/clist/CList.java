/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.frontend.clist;

import sjc.compbase.Context;
import sjc.compbase.StringList;
import sjc.frontend.Language;
import sjc.osio.TextReader;

/**
 * CList: list of files to be compiled
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100513 adopted changed TextReader
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080519 fixed file end handling
 * version 080202 adopted removal of TextReader.eof()
 * version 070114 reduced access level where possible
 * version 061211 removed checkEnvironment
 * version 060818 initial version
 */

public class CList extends Language
{
	private final static int LINELENGTH = 255;
	
	private Context ctx;
	private TextReader r;
	private char[] buffer;
	private boolean error;
	
	protected void init(Context iCtx)
	{
		ctx = iCtx;
		r = new TextReader();
		buffer = new char[LINELENGTH];
	}
	
	protected boolean fileCompetence(String name)
	{
		return name.endsWith(".clt");
	}
	
	protected boolean scanparseFile(StringList fileName)
	{
		boolean success = true;
		String res;
		
		error = false;
		if (!r.initData(ctx.osio.readFile(fileName.str)))
		{
			ctx.out.print("error opening compilation list ");
			ctx.out.println(fileName.str);
			return false;
		}
		//each line contains a filename that has to be compiled
		while (r.nextChar != '\0')
		{
			if ((res = getString()) != null && res.length() > 0)
			{
				if (!ctx.fa.scanparse(res))
					success = false;
			}
			if (error)
				return false;
		}
		return success;
	}
	
	private String getString()
	{
		int bufLen = 0;
		boolean noComment = true;
		
		while (r.nextChar == '\n')
			r.readChar(); //skip line feeds
		if (r.nextChar == '\0')
			return null; //nothing to read
		while (r.nextChar != '\n')
		{ //search next line feed
			if (r.nextChar != '\r')
			{ //ignore carriage return
				if (noComment)
				{
					if (r.nextChar == '\t' || r.nextChar == ' ')
						noComment = false;
					else
					{
						buffer[bufLen++] = r.nextChar;
						if (bufLen >= LINELENGTH)
						{
							ctx.out.print("CList: line too long, reduce below ");
							ctx.out.println(LINELENGTH);
							error = true;
							return null;
						}
					}
				}
			}
			if (r.nextChar == '\0')
				return new String(buffer, 0, bufLen);
			r.readChar();
		}
		return new String(buffer, 0, bufLen);
	}
}
