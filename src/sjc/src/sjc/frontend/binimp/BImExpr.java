/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2015 Stefan Frenz
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

package sjc.frontend.binimp;

import sjc.compbase.*;
import sjc.debug.CodePrinter;
import sjc.frontend.ExVal;

/**
 * BImExpr: dummy expression pointing to binary imported data
 *
 * @author S. Frenz
 * @version 151026 fixed unsigned data output in printExpression
 * version 121014 added support for code printer
 * version 101015 adopted changed Expr
 * version 100312 added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091209 now extending changed ExConstInitObj
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090207 added copyright notice
 * version 070909 optimized signature of Expr.resolve
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070511 optimized by moving functionallity to Importer
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted changed interface of Expr
 * version 061128 added support for embedded mode
 * version 060607 initial version
 */

public class BImExpr extends ExConstInitObj
{
	private final byte[] data;
	private FilledParam par;
	
	protected BImExpr(byte[] id, int fid)
	{
		super(fid, -1, -1);
		data = id;
	}
	
	public void printExpression(CodePrinter prnt)
	{
		FilledParam cur, last = null;
		ExVal val;
		if (par == null)
			for (int i = 0; i < data.length; i++)
			{
				cur = new FilledParam(val = new ExVal(fileID, -1, -1), fileID, -1, -1);
				if (i == 0)
					par = cur;
				val.baseType = StdTypes.T_BYTE;
				val.intValue = ((int) data[i]) & 0xFF;
				if (last != null)
					last.nextParam = cur;
				last = cur;
			}
		prnt.exprArrayInit(this, par);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		dest.owner = unitContext;
		dest.minSize = -1;
		dest.type = this;
		baseType = TypeRef.T_BYTE;
		arrDim = 1;
		if (!ctx.doNotInsertConstantObjectsDuringResolve && (resolveFlags & RF_DEAD_CODE) == 0)
		{
			nextConstInit = unitContext.constObjList;
			unitContext.constObjList = this;
		}
		constObject = true;
		if ((dest.type.typeSpecial & Modifier.MM_FLASH) != 0)
		{
			inFlash = true;
			ctx.needSecondGenConstObj = true;
		}
		return true;
	}
	
	public boolean generateObject(Context ctx, boolean doFlash)
	{
		Object arr;
		int off, arrOff;
		
		if (inFlash != doFlash)
			return true;
		if ((outputLocation = arr = ctx.mem.allocateArray(data.length, 1, 1, StdTypes.T_BYTE, null)) == null)
		{
			ctx.out.println("error in allocating memory while creating binary imported array");
			return false;
		}
		//enter values
		if (ctx.indirScalars)
		{
			arr = ctx.mem.getIndirScalarObject(arr);
			arrOff = ctx.rteSArray.instIndirScalarTableSize;
		}
		else
			arrOff = ctx.rteSArray.instScalarTableSize;
		for (off = 0; off < data.length; off++)
			ctx.mem.putByte(arr, arrOff + off, data[off]);
		return true;
	}
	
	public String getDebugValue()
	{
		return "binimp-data";
	}
}
