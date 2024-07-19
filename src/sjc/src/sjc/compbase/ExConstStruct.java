/* Copyright (C) 2010, 2019 Stefan Frenz
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
 * ExConstStruct: constant reference
 *
 * @author S. Frenz
 * @version 190322 added scalar const handling to support non-final static struct initialization
 * version 101015 adopted changed Expr
 * version 100312 adopted changed ExConstInitObj
 * version 100114 reorganized constant object handling
 * version 100113 initial version
 */

public class ExConstStruct extends ExConstInitObj
{
	private final int addr;
	
	public ExConstStruct(int constantAddress, int fid, int il, int ic)
	{
		super(fid, il, ic);
		addr = constantAddress;
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		dest.owner = unitContext;
		dest.minSize = -1;
		dest.type = this;
		outputLocation = ctx.mem.getStructOutputObject(null, addr);
		if (preferredType == null || !preferredType.isStructType())
		{
			compErr(ctx, "ExConstStruct needs struct as preferredType");
			return false;
		}
		getTypeOf(preferredType);
		if (!ctx.doNotInsertConstantObjectsDuringResolve)
		{
			nextConstInit = unitContext.constObjList;
			unitContext.constObjList = this;
		}
		constObject = true;
		return true;
	}
	
	public boolean generateObject(Context ctx, boolean doFlash)
	{
		//nothing to do
		return true;
	}
	
	public int getOutputLocationOffset(Context ctx)
	{
		return 0; //do not modifiy any explicitly given struct even in embConstRAM mode
	}
	
	public int calcConstantType(Context ctx)
	{
		switch (ctx.arch.relocBytes)
		{
			case 2:
				return T_SHRT;
			case 4:
				return T_INT;
			case 8:
				return T_LONG;
		}
		return 0;
	}
	
	public int getConstIntValue(Context ctx)
	{
		return addr;
	}
	
	public long getConstLongValue(Context ctx)
	{
		return ((long) addr) & 0xFFFFFFFFL;
	}
	
	public String getDebugValue()
	{
		return "const-ref";
	}
}
