/* Copyright (C) 2006, 2007, 2008, 2009, 2010 Stefan Frenz
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

import sjc.compbase.StringList;

/**
 * OsIO: interface to operating system input output routines
 *
 * @author S. Frenz
 * @version 100513 removed getNewTextReader
 * version 100423 simplified checkMagicAccess
 * version 100421 added checkMagicAccess
 * version 090311 added getTimeInfo
 * version 090303 initial version
 */

public abstract class OsIO
{
	public abstract byte[] readFile(String fname);
	
	public abstract boolean isDir(String name);
	
	public abstract StringList listDir(String name, boolean recurse);
	
	public abstract BinWriter getNewBinWriter();
	
	public abstract TextPrinter getNewFilePrinter(String filename);
	
	public abstract long getTimeInfo();
	
	public boolean checkMagicAccess()
	{
		return true;
	} //has to be overwritten to enable controlled MAGIC-access
}
