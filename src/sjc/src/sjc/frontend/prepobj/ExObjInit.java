/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2015 Stefan Frenz
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

package sjc.frontend.prepobj;

import sjc.compbase.*;

/**
 * ExObjInit: to be created object
 *
 * @author S. Frenz
 * @version 151108 added allocation debug hint
 * version 101015 adopted changed Expr
 * version 100409 adopted changed TypeRef
 * version 100312 adopted changed TypeRef and added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091216 removed unneeded variable next
 * version 091210 initial version
 */

public class ExObjInit extends ExConstInitObj
{
	public TypeRef type;
	public ExObjInitField vars;
	public Unit typeUnit;
	private ExObjInitField inlineArrayDefVar;
	
	public ExObjInit(TypeRef requestedType, int fid, int il, int ic)
	{
		super(fid, il, ic);
		type = requestedType;
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		ExObjInitField var, tmp;
		boolean oldDoNotInsertConstantObjectsDuringResolve = false;
		
		dest.owner = unitContext;
		dest.minSize = -1;
		dest.type = type;
		if (!type.resolveType(unitContext, ctx))
			return false;
		typeUnit = type.qid.unitDest;
		if (!ctx.doNotInsertConstantObjectsDuringResolve && (resolveFlags & RF_DEAD_CODE) == 0)
		{
			nextConstInit = unitContext.constObjList;
			unitContext.constObjList = this;
		}
		var = vars;
		while (var != null)
		{
			//check if variable is already defined
			tmp = vars;
			while (tmp != var)
			{
				if (var.name.equals(tmp.name))
				{
					var.printPos(ctx, "variable already defined");
					return false;
				}
				tmp = tmp.next;
			}
			//get target variable and resolve init
			if ((var.destVar = typeUnit.searchVariable(var.name, ctx)) == null)
			{
				var.printPos(ctx, "field ");
				ctx.out.print(var.name);
				ctx.out.print(" not found");
				return false;
			}
			if ((var.destVar.modifier & Modifier.M_ARRLEN) != 0)
			{
				var.printPos(ctx, "inline array length must not be set explicitly");
				return false;
			}
			if (var.destVar.location == Vrbl.L_INLARR)
			{
				if (var.destVar.type.qid != null)
				{
					var.printPos(ctx, "init of inlined object arrays is not yet supported");
					return false;
				}
				inlineArrayDefVar = var;
				oldDoNotInsertConstantObjectsDuringResolve = ctx.doNotInsertConstantObjectsDuringResolve;
				ctx.doNotInsertConstantObjectsDuringResolve = true;
				if (var.destVar.type.typeSpecial != S_INSTINLARR)
				{
					compErr(ctx, "expected inline array type for inline array");
					return false;
				}
				var.destVar.type.typeSpecial = S_NOSPECIAL; //treat as normal array for type compare
			}
			if (!var.init.resolve(unitContext, mthdContext, resolveFlags, var.destVar.type, ctx))
				return false;
			if (var.init.compareType(var.destVar.type, true, ctx) != TypeRef.C_EQ)
			{
				var.printPos(ctx, "need same type in initialization");
				return false;
			}
			if (var.destVar.location == Vrbl.L_INLARR)
			{
				var.destVar.type.typeSpecial = S_INSTINLARR;
				ctx.doNotInsertConstantObjectsDuringResolve = oldDoNotInsertConstantObjectsDuringResolve;
			}
			//everything ok, check next var
			var = var.next;
		}
		//everything ok
		getTypeOf(type);
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
		Object obj, objAcc;
		ExConstInitObj initObj;
		int off;
		ExObjInitField var;
		ExArrayInit arr = null;
		boolean ids;
		int instSTS = typeUnit.instScalarTableSize;
		
		if (inFlash != doFlash)
			return true;
		if (inlineArrayDefVar != null)
		{
			//very special case - therefore remember:
			//inline-variable and indirScalar are not enabled at the same time
			//at the moment only basetype-arrays are supported
			instSTS += (arr = (ExArrayInit) inlineArrayDefVar.init).len * TypeRef.getMinSize(inlineArrayDefVar.init.baseType);
		}
		if ((outputLocation = obj = ctx.mem.allocate(instSTS, typeUnit.instIndirScalarTableSize, typeUnit.instRelocTableEntries, typeUnit.outputLocation)) == null)
		{
			ctx.out.println("error allocating memory while allocation prepared object");
			return false;
		}
		ctx.mem.allocationDebugHint(this);
		var = vars;
		while (var != null)
		{
			ids = false;
			switch (var.destVar.location)
			{
				case Vrbl.L_INSTREL:
					if (!var.init.isCompInitConstObject(ctx))
					{
						var.compErr(ctx, "not constant object in prepared object");
						return false;
					}
					if ((initObj = var.init.getConstInitObj(ctx)) == null || initObj.outputLocation == null)
					{
						var.compErr(ctx, "constant referenced object is not generated");
						return false;
					}
					ctx.arch.putRef(obj, var.destVar.relOff, initObj.outputLocation, initObj.getOutputLocationOffset(ctx));
					break;
				case Vrbl.L_INSTIDS:
					ids = true;
					//no break, has to do the following too
				case Vrbl.L_INSTSCL:
					if (var.init.calcConstantType(ctx) == 0)
					{
						compErr(ctx, "not constant value in prepared object");
						return false;
					}
					objAcc = ids ? ctx.mem.getIndirScalarObject(obj) : obj;
					off = var.destVar.relOff;
					switch (var.destVar.type.baseType)
					{
						case StdTypes.T_BOOL:
						case StdTypes.T_BYTE:
							ctx.mem.putByte(objAcc, off, (byte) var.init.getConstIntValue(ctx));
							break;
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
							ctx.mem.putShort(objAcc, off, (short) var.init.getConstIntValue(ctx));
							break;
						case StdTypes.T_INT:
						case StdTypes.T_FLT:
							ctx.mem.putInt(objAcc, off, var.init.getConstIntValue(ctx));
							break;
						case StdTypes.T_LONG:
						case StdTypes.T_DBL:
							ctx.mem.putLong(objAcc, off, var.init.getConstLongValue(ctx));
							break;
						default:
							compErr(ctx, "invalid base type for field initialization");
							return false;
					}
					break;
				case Vrbl.L_INLARR:
					if (!arr.putElements(obj, typeUnit.instScalarTableSize, false, ctx))
						return false;
					ctx.mem.putInt(obj, var.destVar.relOff, arr.len); //relOff of inlArr-var points to length field
					break;
				default:
					ctx.out.println("invalid var-location for prepared object");
					return false;
			}
			var = var.next;
		}
		return true;
	}
	
	public String getDebugValue()
	{
		return "prepared object";
	}
}
