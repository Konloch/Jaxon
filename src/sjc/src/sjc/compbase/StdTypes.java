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

package sjc.compbase;

import sjc.osio.TextPrinter;

/**
 * StdTypes: platform and language independent standard-types
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100925 moved char between shrt and int
 * version 100401 removed T_MARK
 * version 090207 added copyright notice
 * version 080614 merged T_CLSS and T_INTF to T_DESC
 * version 080517 added baseTypeCounter
 * version 070628 added types of TypeRef
 * version 060607 initial version
 */

public class StdTypes
{
	public final static int T_BYTE = 1; //"byte"
	public final static int T_SHRT = 2; //"short"
	public final static int T_CHAR = 3; //"char"
	public final static int T_INT = 4; //"int"
	public final static int T_LONG = 5; //"long"
	public final static int T_FLT = 6; //"float"
	public final static int T_DBL = 7; //"double"
	public final static int T_BOOL = 8; //"boolean"
	public final static int MAXBASETYPE = 8; //max number of real basic type
	public final static int T_NULL = -1; //null-type, unresolved
	public final static int T_NNPT = -2; //null-type, pointer
	public final static int T_NDPT = -3; //null-type, double sized pointer
	public final static int T_PACK = -4; //package (internal use during dereferenzation only)
	public final static int T_DESC = -5; //class (internal use during dereferenzation only)
	public final static int T_PTR = -6; //single pointer
	public final static int T_DPTR = -7; //double sized pointer (interfaces...)
	public final static int T_MAGC = -8; //magic (internal use during dereferenzation only)
	public final static int T_VOID = -9;
	public final static int T_QID = -10;
	
	public static void printStdType(int type, TextPrinter v)
	{
		switch (type)
		{
			case T_BYTE:
				v.print("byte");
				return;
			case T_SHRT:
				v.print("short");
				return;
			case T_INT:
				v.print("int");
				return;
			case T_LONG:
				v.print("long");
				return;
			case T_FLT:
				v.print("float");
				return;
			case T_DBL:
				v.print("double");
				return;
			case T_CHAR:
				v.print("char");
				return;
			case T_BOOL:
				v.print("boolean");
				return;
			case T_NULL:
				v.print("null-u");
				return;
			case T_NNPT:
				v.print("null-p");
				return;
			case T_NDPT:
				v.print("null-d");
				return;
			case T_PACK:
				v.print("Tpackage");
				return;
			case T_DESC:
				v.print("Tdesc");
				return;
			case T_PTR:
				v.print("TPTR");
				return;
			case T_DPTR:
				v.print("TDPTR");
				return;
			case T_MAGC:
				v.print("TMAGIC");
				return;
		}
		v.print("!invType!");
	}
}
