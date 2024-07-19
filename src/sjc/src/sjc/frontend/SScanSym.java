/* Copyright (C) 2005, 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.frontend;


/**
 * SScanSym: a symbol used as transfer between SScanner and parsers
 *
 * @author S. Frenz
 * @version 091209 moved to frontend package
 * version 091102 added sypos and syposE
 * version 091018 removed unneeded variables
 * version 090207 added copyright notice
 * version 070114 reduced access level where possible
 * version 060607 initial version
 */

public class SScanSym
{
	public int type, par; //type and parameter of symbol
	public int syline, sycol; //line and column of start of symbol
	public int sypos, syposE; //position of start/end of symbol
	public int intBuf; //int-buffer, used for S_NUM / T_BYTE, T_SHORT, T_CHAR, T_INT, T_BOOL, T_FLT
	public long longBuf; //long-buffer, used for S_NUM / T_LONG, T_DBL
	public String strBuf; //buffer for string-constants
	
	protected SScanSym()
	{
		type = SScanner.RES;
		par = SScanner.RES;
	}
}
