/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2019, 2024 Stefan Frenz
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

package sjc.frontend.sjava;

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;
import sjc.frontend.ExVal;

/**
 * ExEnc: bracket enclosure of an expression, may be a conversion
 *
 * @author S. Frenz
 * @version 240329 added check for invalid boolean-convertion
 * version 190322 added const stuct handling to support non-final static struct initialization
 * version 121031 added special code printer case if no conversion for const value / deref has to be printed
 * version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 100927 fixed unsignedness of chars
 * version 100623 keeping convertTo for structs to enable re-resolving
 * version 100504 adopted changed Expr
 * version 100409 adopted changed TypeRef
 * version 100114 reorganized constant object handling
 * version 100113 added support for ExConstRef
 * version 091112 added getConvertedExpr
 * version 091005 adopted changed Expr with support for preferredType
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090218 fixed loading of converted constant double
 * version 090207 added copyright notice
 * version 080712 added conversion check for unneccessary conversion
 * version 080613 adopted hasEffect->effectType
 * version 080401 added support for special conversion to STRUCT
 * version 080119 adopted changed signature of Expr.canGenAddr
 * version 070909 optimized signature of Expr.resolve
 * version 070828 avoiding double resolving of inserted conversion
 * version 070817 redesign of constant checking, now also supporting from/to float/double
 * version 070812 avoid internal conversion of constant float/double
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 moved conversion of object to ExConv, adapted change in printPos and compErr
 * version 070106 added canGenAddr
 * version 070101 adopted change in genCall
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060628 added support for static compilation
 * version 060621 added support for implicit conversion-resolve
 * version 060620 added missing conversion in getConstLongValue
 * version 060619 bugfix for register-allocation in genCondJmp
 * version 060607 initial version
 */

public class ExEnc extends ExCheckType
{
	private final static String EXCRANGE = "value exceeds range";
	private final static String RANGEUNCH = "range in conversion unchecked";
	private final static String WARNONLY = " (warning)";
	private final static String INSCONV = "inserted implicit conversion";
	
	protected Expr ex;
	protected TypeRef convertTo;
	
	private int calcConstType; //filled in implConvResolve
	private int constInt; //filled by getIntOfEnc and getFloatOfEnc
	private long constLong; //filled by getLongOfEnc and getDoubleOfEnc
	
	protected ExEnc(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		if (convertTo == null && (ex instanceof ExVal || ex instanceof ExDeRef))
			ex.printExpression(codePrnt);
		else
			codePrnt.exprEnc(convertTo, ex);
	}
	
	protected static Expr getConvertedResolvedExpr(Expr ex, TypeRef destType, Unit unitContext, Context ctx)
	{
		ExEnc enc;
		enc = new ExEnc(ex.fileID, ex.line, ex.col);
		enc.convertTo = destType;
		enc.ex = ex;
		if (!enc.implConvResolve(unitContext, ctx))
		{
			ctx.out.print(" in ");
			ctx.out.print(INSCONV);
			return null;
		}
		if (ctx.verbose)
		{
			enc.printPos(ctx, INSCONV);
			ctx.out.print(" from ");
			enc.ex.printType(ctx.out);
			ctx.out.print(" to ");
			enc.printType(ctx.out);
			ctx.out.println();
		}
		return enc;
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		if (convertTo != null && !convertTo.resolveType(unitContext, ctx))
		{
			ctx.out.print(" in conversion");
			return false;
		}
		if (convertTo != null)
			resolveFlags |= RF_CHECKREAD;
		if (!ex.resolve(unitContext, mthdContext, resolveFlags, convertTo == null ? preferredType : convertTo, ctx))
			return false;
		return implConvResolve(unitContext, ctx);
	}
	
	protected boolean implConvResolve(Unit unitContext, Context ctx)
	{
		//ex and convertTo has already been resolved, just check
		int myInt = 0;
		long myLong = 0l;
		
		if (convertTo != null)
		{
			getTypeOf(convertTo);
			if (isStructType())
			{
				if (ex.getLeftOfRightMostExpr().baseType != StdTypes.T_MAGC)
				{
					printPos(ctx, "need MAGIC-expression/statement for STRUCT-conversion");
					return false;
				}
				if (!ex.isStructType())
				{
					printPos(ctx, "can not convert from object to struct without MAGIC");
					ctx.out.print("enc:");
					printType(ctx.out);
					ctx.out.print(" ex:");
					ex.printType(ctx.out);
					ctx.out.println();
					return false;
				}
				calcConstType = ex.calcConstantType(ctx);
				switch (calcConstType)
				{
					case T_INT:
						constInt = ex.getConstIntValue(ctx);
						break;
					case T_LONG:
						constLong = ex.getConstLongValue(ctx);
						break;
				}
			}
			else if (baseType < 0 || arrDim > 0)
			{
				if (!prepareObjectConversion(ex, convertTo, unitContext, ctx))
				{
					ctx.out.print(" in conversion");
					return false;
				}
			}
			else
			{
				if (ex.baseType < 0 || ex.arrDim > 0)
				{
					printPos(ctx, "can not convert between standard- and object-types");
					return false;
				}
			}
			//check if conversion is required
			if (compareType(ex, false, ctx) == TypeRef.C_EQ)
			{
				if (ctx.verbose)
				{
					printPos(ctx, "conversion not neccessary");
					ctx.out.println();
				}
				convertTo = null; //remove conversion as it is not required
			}
			else if (ex.baseType == StdTypes.T_BOOL || convertTo.baseType == StdTypes.T_BOOL)
			{ //invalid conversion from/to boolean
				printPos(ctx, "boolean is not convertible");
				return false;
			}
		}
		else
		{
			getTypeOf(ex);
			effectType = EF_NORM; //this expression makes only sense as statement if not converted
		}
		//check for constant
		if ((myInt = ex.calcConstantType(ctx)) == T_INT || myInt == T_LONG)
		{
			if (myInt == T_INT)
				myInt = ex.getConstIntValue(ctx);
			else
				myLong = ex.getConstLongValue(ctx);
			//is constant
			switch (baseType)
			{ //toType
				case T_BYTE:
					if (getIntOfEnc(myInt, myLong, ctx) && (int) ((byte) constInt) != constInt && (constInt & 0xFF) != constInt)
					{
						printPos(ctx, EXCRANGE);
						ctx.out.println(WARNONLY);
					}
					constInt = (byte) constInt;
					calcConstType = T_INT;
					break;
				case T_SHRT:
					if (getIntOfEnc(myInt, myLong, ctx) && (int) ((short) constInt) != constInt && (constInt & 0xFFFF) != constInt)
					{
						printPos(ctx, EXCRANGE);
						ctx.out.println(WARNONLY);
					}
					constInt = (short) constInt;
					calcConstType = T_INT;
					break;
				case T_CHAR:
					if (getIntOfEnc(myInt, myLong, ctx) && (int) ((char) constInt) != constInt)
					{
						printPos(ctx, EXCRANGE);
						ctx.out.println(WARNONLY);
					}
					constInt = (char) constInt;
					calcConstType = T_INT;
					break;
				case T_INT:
					getIntOfEnc(myInt, myLong, ctx);
					calcConstType = T_INT;
					break;
				case T_LONG:
					getLongOfEnc(myInt, myLong, ctx);
					calcConstType = T_LONG;
					break;
				case T_FLT:
					getFloatOfEnc(myInt, myLong, ctx);
					calcConstType = T_INT;
					break;
				case T_DBL:
					getDoubleOfEnc(myInt, myLong, ctx);
					calcConstType = T_LONG;
					break;
			}
		}
		//everything ok
		return true;
	}
	
	public int calcConstantType(Context ctx)
	{
		return calcConstType;
	}
	
	public int getConstIntValue(Context ctx)
	{
		return constInt;
	}
	
	public long getConstLongValue(Context ctx)
	{
		return constLong;
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return convertTo == null && ex.isCompInitConstObject(ctx);
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return isCompInitConstObject(ctx) ? ex.getConstInitObj(ctx) : null;
	}
	
	public void genOutputAddr(int reg, Context ctx)
	{
		if (convertTo == null)
			ex.genOutputAddr(reg, ctx);
		else
			compErr(ctx, "ExEnc.genOutputAddr is invalid for converted types");
	}
	
	public boolean canGenAddr(Unit unitContext, boolean allowSpecialWriteAccess, int resolveFlags, Context ctx)
	{
		return convertTo == null && ex.canGenAddr(unitContext, allowSpecialWriteAccess, resolveFlags, ctx);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int oreg, restore;
		
		switch (calcConstantType(ctx))
		{
			case T_INT:
				ctx.arch.genLoadConstVal(reg, getConstIntValue(ctx), getRegType(ctx));
				break;
			case T_LONG:
				ctx.arch.genLoadConstDoubleOrLongVal(reg, getConstLongValue(ctx), getRegType(ctx) == StdTypes.T_DBL);
				break;
			default:
				if (convertTo == null || isStructType())
					ex.genOutputVal(reg, ctx);
				else
				{
					restore = ctx.arch.prepareFreeReg(0, 0, reg, ex.getRegType(ctx));
					oreg = ctx.arch.allocReg();
					ex.genOutputVal(oreg, ctx);
					if (baseType > 0 && arrDim == 0)
						ctx.arch.genConvertVal(reg, oreg, baseType, ex.baseType);
					else
						genIsType(reg, oreg, true, ctx);
					ctx.arch.deallocRestoreReg(oreg, reg, restore);
				}
		}
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction elseDest, Context ctx)
	{
		if (convertTo != null)
			compErr(ctx, "ExEnc.genOutputCondJmp with conversion is invalid");
		else
			ex.genOutputCondJmp(jumpDest, isTrue, elseDest, ctx);
	}
	
	//get integer number constInt, return true if caller has to check overflow
	private boolean getIntOfEnc(int i, long l, Context ctx)
	{
		switch (ex.baseType)
		{ //fromType
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
			case T_INT:
				constInt = i;
				return true;
			case T_LONG:
				if ((long) (constInt = (int) l) != l && (l & 0xFFFFFFFFl) != l)
				{
					printPos(ctx, EXCRANGE);
					ctx.out.println(WARNONLY);
					return false;
				}
				return true;
			case T_FLT:
				constInt = (int) ctx.arch.real.getLongOfFloat(i);
				printPos(ctx, RANGEUNCH);
				ctx.out.println(WARNONLY);
				return false;
			case T_DBL:
				constInt = (int) ctx.arch.real.getLongOfDouble(l);
				printPos(ctx, RANGEUNCH);
				ctx.out.println(WARNONLY);
				return false;
		}
		compErr(ctx, "ExEnc.getIntOfEnc failed");
		return false;
	}
	
	//get integer number constLong, return true if caller has to check overflow
	private boolean getLongOfEnc(int i, long l, Context ctx)
	{
		switch (ex.baseType)
		{ //fromType
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
			case T_INT:
				constLong = i;
				return false;
			case T_LONG:
				constLong = l;
				return false;
			case T_FLT:
				constLong = ctx.arch.real.getLongOfFloat(i);
				printPos(ctx, RANGEUNCH);
				ctx.out.println(WARNONLY);
				return false;
			case T_DBL:
				constLong = ctx.arch.real.getLongOfDouble(l);
				printPos(ctx, RANGEUNCH);
				ctx.out.println(WARNONLY);
				return false;
		}
		compErr(ctx, "ExEnc.getLongOfEnc failed");
		return false;
	}
	
	//get floating number constInt, return true if caller has to check overflow
	private boolean getFloatOfEnc(int i, long l, Context ctx)
	{
		switch (ex.baseType)
		{ //fromType
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
			case T_INT:
				constInt = ctx.arch.real.buildFloat(i, 0);
				return false;
			case T_LONG:
				constInt = ctx.arch.real.buildFloat(l, 0);
				return false;
			case T_FLT:
				constInt = i;
				return false;
			case T_DBL:
				constInt = ctx.arch.real.buildFloatFromDouble(l);
				return false;
		}
		compErr(ctx, "ExEnc.getIntOfEnc failed");
		return false;
	}
	
	//get floating number constLong, return true if caller has to check overflow
	private boolean getDoubleOfEnc(int i, long l, Context ctx)
	{
		switch (ex.baseType)
		{ //fromType
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
			case T_INT:
				constLong = ctx.arch.real.buildDouble(i, 0);
				return false;
			case T_LONG:
				constLong = ctx.arch.real.buildDouble(l, 0);
				return false;
			case T_FLT:
				constLong = ctx.arch.real.buildDoubleFromFloat(i);
				return false;
			case T_DBL:
				constLong = l;
				return false;
		}
		compErr(ctx, "ExEnc.getLongOfEnc failed");
		return false;
	}
}
