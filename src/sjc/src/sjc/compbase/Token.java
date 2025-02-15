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

package sjc.compbase;

/**
 * Token: basic entity that may be created by a language parser and that may be a jump destination
 *
 * @author S. Frenz
 * @version 091109 got flowWarn from Stmt
 * version 091102 added srcStart and srcEnd
 * version 090207 added copyright notice
 * version 070111 added ": " in printPos and compErr
 * version 070104 added newline in compErr
 * version 061229 removed firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class Token
{
	public int fileID, line, col; //file identifier (administrated by Context), line and column
	public int srcStart, srcLength; //first character in file, source code length of token
	
	public final static String ERR_UNREACHABLE_CODE = "unreachable code";
	
	public Token(int fid, int il, int ic)
	{
		fileID = fid;
		line = il;
		col = ic;
	}
	
	public void printPos(Context ctx, String msg)
	{
		ctx.printPos(fileID, line, col);
		if (msg != null)
		{
			ctx.out.print(':');
			ctx.out.print(' ');
			ctx.out.print(msg);
		}
	}
	
	public void compErr(Context ctx, String msg)
	{
		ctx.out.print("### compile-error at ");
		ctx.printPos(fileID, line, col);
		if (msg != null)
		{
			ctx.out.print(':');
			ctx.out.print(' ');
			ctx.out.println(msg);
		}
		else
			ctx.out.println();
		ctx.err = true;
	}
	
	public void flowWarn(Context ctx, String msg)
	{
		printPos(ctx, "FLOW-WARNING: ");
		ctx.out.println(msg);
	}
}
