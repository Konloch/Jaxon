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

package sjc.frontend;

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * ExVal: constant value
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 101014 fixed implicit char conversion
 * version 091209 moved to frontend package
 * version 091018 simplified setting of preferredType, declining original float base type
 * version 091017 fixed setting of preferredType for original float base type
 * version 091005 adopted changed Expr with support for preferredType in resolving
 * version 090724 adopted changed Expr
 * version 090718 adopted move of modifier flags from Vrbl to AccVar, adopted changed Expr
 * version 090207 added copyright notice and replaced genLoadConstNullAddr by genLoadConstVal
 * version 070909 optimized signature of Expr.resolve
 * version 070809 added support for float and double
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore, optimized genOutputVal
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061127 optimized genOutputCondJmp
 * version 060607 initial version
 */

public class ExVal extends Expr
{
	public int intValue; //keeps data for byte, short, char, int, float and boolean
	public long longValue; //keeps data for long and double
	
	public ExVal(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprVal(this, intValue, longValue);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		//nothing to resolve for a constant value, but check if there is a preferred type
		if (preferredType != null && preferredType.arrDim == 0 && compareType(preferredType, false, ctx) != TypeRef.C_EQ)
		{
			if (calcConstantType(ctx) == T_INT && baseType != T_FLT)
			{
				switch (preferredType.baseType)
				{
					case T_BYTE:
						if ((intValue & 0xFFFFFF80) == 0)
							baseType = T_BYTE;
						break;
					case T_SHRT:
						if ((intValue & 0xFFFF8000) == 0)
							baseType = T_SHRT;
						break;
					case T_CHAR:
						if ((intValue & 0xFFFF0000) == 0)
							baseType = T_CHAR;
						break;
					case T_INT:
						baseType = T_INT;
						break;
					case T_LONG:
						if ((intValue & 0x80000000) == 0)
						{
							longValue = intValue;
							intValue = 0;
							baseType = T_LONG;
						}
						break;
				}
			}
		}
		return true;
	}
	
	public boolean isConstZero()
	{
		switch (baseType)
		{
			case T_BYTE:
			case T_SHRT:
			case T_INT:
			case T_CHAR:
			case T_BOOL:
			case T_FLT:
				return intValue == 0;
			case T_DBL:
			case T_LONG:
				return longValue == 0l;
			case T_NULL:
			case T_NNPT:
			case T_NDPT:
				return true;
		}
		return false; //unknown type, should never occur
	}
	
	public int calcConstantType(Context ctx)
	{
		switch (baseType)
		{
			case T_BYTE:
			case T_SHRT:
			case T_INT:
			case T_CHAR:
			case T_BOOL:
			case T_FLT:
				return T_INT;
			case T_LONG:
			case T_DBL:
				return T_LONG;
		}
		return 0;
	}
	
	public int getConstIntValue(Context ctx)
	{
		return intValue;
	}
	
	public long getConstLongValue(Context ctx)
	{
		return longValue;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		boolean asDouble = false;
		switch (baseType)
		{
			case T_BYTE:
			case T_SHRT:
			case T_INT:
			case T_CHAR:
			case T_BOOL:
			case T_FLT:
				ctx.arch.genLoadConstVal(reg, intValue, baseType);
				return;
			case T_DBL:
				asDouble = true; //no break
			case T_LONG:
				ctx.arch.genLoadConstDoubleOrLongVal(reg, longValue, asDouble);
				return;
			case T_NULL:
				compErr(ctx, "ExVal.genOutputVal with unresolved null-type");
				return;
			case T_NNPT:
				ctx.arch.genLoadConstVal(reg, 0, StdTypes.T_PTR);
				return;
			case T_NDPT:
				ctx.arch.genLoadConstVal(reg, 0, StdTypes.T_DPTR);
				return;
		}
		compErr(ctx, "ExVal.genOutputVal for unsupported type");
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction elseDest, Context ctx)
	{
		if (baseType != T_BOOL)
		{
			compErr(ctx, "ExVal.genOutputJmp with invalid type");
			return;
		}
		if (intValue == (isTrue ? 1 : 0))
			ctx.arch.genJmp(jumpDest);
	}
}
