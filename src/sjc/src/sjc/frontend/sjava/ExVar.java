/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2019 Stefan Frenz
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

import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * ExVar: access to a variable
 *
 * @author S. Frenz
 * @version 190424 fixed outer-inner reference
 * version 120925 added support for code printer
 * version 120227 cleaned up "package sjc." typo
 * version 110624 adopted changed Context
 * version 101231 adopted changed Unit
 * version 101218 adopted changed Pack/Unit
 * version 101015 adopted changed Expr
 * version 100504 cleaned up Clss conversion, made use of AccVar.getInitExpr and Unit.getImportList, reduced compErr-messages
 * version 100114 reorganized constant object handling
 * version 091026 adopted changed minimumAccessLevel return value
 * version 091021 adopted changed modifier declarations
 * version 091020 added relation tracking
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted move of modifier flags from Vrbl to AccVar, adopted changed Expr
 * version 090616 changed type of id from PureID to String
 * version 090508 fixed unit search order 1) imported units, 2) local package, 3) imported packages
 * version 090221 changed all access level checks to centralized version, fixed outerLevel calculation
 * version 090218 made use of centralized access level check
 * version 090207 added copyright notice
 * version 090206 moved isAddrOnStack to ExAccVrbl
 * version 080911 fixed access to other inner class in outer class through name of class
 * version 080813 fixed access to not resolved variables in constant resolving
 * version 080616 added check for invalid constant objects
 * version 080614 adopted merging of T_CLSS/T_INTF in StdTypes, adopted changed Unit.searchVariable
 * version 080610 removed conversion to JMthd as it is no longer required
 * version 080523 fixed error message for not possible access of constant
 * version 080306 fixed too restrictive package-check of bugfix from 080305
 * version 080305 fixed invalid package-match if no package result possible
 * version 080220 varResolve limits search if resolving deref
 * version 080105 adopted changed signature of Unit.isParent
 * version 071227 added support for inner units
 * version 070913 adopted change in Mthd
 * version 070909 optimized signature of Expr.resolve, adopted changed ExAccVrbl
 * version 070813 fixed unit context during resolving of static variable in dynaMem mode
 * version 070809 fixed owner during resolving of foreign variable
 * version 070731 adopted change of QualID[] to QualIDList
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 moved array stuff to ExDeArray
 * version 070104 fixed addrIsOnStack
 * version 061203 optimized calls to printPos and compErr
 * version 061202 optimized static modes
 * version 061129 static TypeRef object moved dynamically to Context
 * version 061127 added support for embedded mode
 * version 061111 added checks for invalid struct-array-derefs
 * version 061107 added support for indexed struct-variables
 * version 061105 added support for variables inside a STRUCT
 * version 061030 changed detection of indirectCall
 * version 061027 optimized check for special names
 * version 060804 added ensureClassContext
 * version 060803 added check for ownership of static variables
 * version 060714 changed bound/null check to support runtime handler
 * version 060705 changed deref-execution order (now Java-standard)
 * version 060628 added support for static compilation
 * version 060620 bugfix long-int
 * version 060619 bugfix for register-allocation in genCondJmp
 * version 060607 initial version
 */

public class ExVar extends ExAccVrbl
{
	private final static String NOTINITIALIZED1 = "variable ";
	private final static String NOTINITIALIZED2 = " may not have been initialized";
	
	protected String id;
	
	protected ExVar(String ip, int fid, int il, int ic)
	{
		super(fid, il, ic);
		id = ip;
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprVar(dest, id, isThis);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		if (id.equals("this"))
			return varResolveThis(unitContext, mthdContext, ctx);
		if (!varResolve(null, unitContext, mthdContext, unitContext, mthdContext, resolveFlags, false, preferredType, ctx))
			return false;
		if (dest == null)
		{ //destination is class or package
			printPos(ctx, "var ");
			ctx.out.print(id);
			ctx.out.print(" not found");
			return false;
		}
		return true;
	}
	
	protected boolean varResolveThis(Unit inUnit, Mthd inMthd, Context ctx)
	{
		if ((inMthd.modifier & Modifier.M_STAT) != 0)
		{
			printPos(ctx, "\"this\" can not be used in static methods");
			return false;
		}
		baseType = T_QID;
		qid = inUnit.getQIDTo();
		isThis = true;
		return true;
	}
	
	protected boolean varResolveSuper(Unit inUnit, Mthd inMthd, Context ctx)
	{
		Unit parent;
		if (inUnit == ctx.langRoot || inUnit.extsID == null)
		{
			printPos(ctx, "\"super\" is not possible in lang-root");
			return false;
		}
		if ((inMthd.modifier & Modifier.M_STAT) != 0)
		{
			printPos(ctx, "\"super\" not allowed in static context");
		}
		parent = inUnit.extsID.unitDest;
		baseType = T_QID;
		qid = parent.getQIDTo();
		if (ctx.dynaMem)
			importedClass = inUnit.getRefUnit(parent, true);
		return true;
	}
	
	//has also to resolve units and packets for ExDeRef
	protected boolean varResolve(Pack inPack, Unit inUnit, Mthd inMthd, Unit unitContext, Mthd mthdContext, int resolveFlags, boolean asDeRef, TypeRef preferredType, Context ctx)
	{
		Param vParam = null;
		Vrbl vLocal = null, dvar;
		QualIDList impt = null;
		int accessLevel;
		
		//check for this, super, MAGIC and MARKER
		if (SJava.isSpecialName(id))
		{
			printPos(ctx, "keyword used in improper way");
			return false;
		}
		//search name
		if (inMthd != null)
		{
			vParam = inMthd.param;
			vLocal = inMthd.vars;
		}
		//else: already initialized with null
		if (inUnit != null)
			impt = inUnit.getImportList();
		if (asDeRef || (!resolveInVar(vLocal, ctx) && !resolveInParam(vParam, ctx)))
		{
			if (inUnit != null && inUnit.innerUnits != null && resolveInInner(inUnit.innerUnits, ctx))
			{ //check inner units
				//TODO: is this affected by checkRead?
				return unitContext.minimumAccessLevel(this, id, qid.unitDest, qid.unitDest.modifier, inUnit == unitContext, ctx) != Modifier.M_ERROR;
			}
			//access not through inner class
			if (inUnit == null || !resolveInUnit(inUnit, ctx))
			{
				if (!resolveInPackage(inPack, true) && (asDeRef || (inUnit == null || (!resolveInImptList(impt, false) && !resolveInPackage(inUnit.pack.packDest, asDeRef) && !resolveInImptList(impt, true) && !resolveInPackage(ctx.root, true) && !resolveInPackage(ctx.defUnits, false) && !resolveInPackage(ctx.rte, false)))))
				{
					if (inUnit != null && inUnit.outerUnit != null)
					{ //try to resolve in outer unit
						if (varResolve(inPack, inUnit.outerUnit, inMthd, unitContext, mthdContext, resolveFlags, asDeRef, preferredType, ctx))
						{
							if (dest == null)
							{ //found "variable" which is class name of inner class of shared outer class
								if (qid.unitDest != null)
									return true;
								compErr(ctx, "outer-inner reference does not point to unit");
								return false;
							}
							//access of variable
							if ((resolveFlags & RF_CHECKREAD) != 0 && (dest.modifier & Modifier.MF_ISWRITTEN) == 0)
							{
								printPos(ctx, NOTINITIALIZED1);
								ctx.out.print(dest.name);
								ctx.out.print(NOTINITIALIZED2);
								return false;
							}
							dvar = (Vrbl) dest;
							if ((dvar.modifier & Modifier.M_STAT) == 0)
							{ //access via instance
								if ((inUnit.modifier & Modifier.M_STAT) != 0)
								{
									printPos(ctx, "can not access instance variable of outer unit in static unit");
									return false;
								}
								outerLevel++; //increase level
								outerAccessStart = inUnit; //last write == outest unit wins
							}
							else
							{ //access via class descriptor
								ensureClassContext = true;
								if (ctx.dynaMem)
									importedClass = unitContext.getRefUnit(dvar.owner, true);
							}
							if (ctx.relations != null)
								ctx.relations.addRelation(dest.owner, null, dest, unitContext, mthdContext);
							return true; //found variable in outer unit
						}
						return false; //error message already printed
					}
					//variable not found
					printPos(ctx, "identifier ");
					ctx.out.print(id);
					ctx.out.print(" not found");
					return false;
				}
				//check access level of class or interface
				if (baseType != StdTypes.T_PACK)
				{
					if ((accessLevel = unitContext.minimumAccessLevel(this, id, qid.unitDest, qid.unitDest.modifier, true, ctx)) == Modifier.M_ERROR)
						return false;
					qid.unitDest.modifier |= accessLevel;
				}
				
			}
			else
			{ //check unit-variable
				dvar = (Vrbl) dest;
				if (inMthd == null || dvar == null)
				{
					compErr(ctx, "inMthd/dvar==null");
					return false;
				}
				//check if owner is resolved already
				if (dvar.location == AccVar.L_NOTRDY && !dvar.owner.resolveInterface(ctx))
					return false; //output already done
				//check access level
				if ((accessLevel = unitContext.minimumAccessLevel(this, dvar.name, dvar.owner, dvar.modifier, inUnit == unitContext, ctx)) == Modifier.M_ERROR)
					return false;
				dvar.modifier |= accessLevel | Modifier.MA_ACCSSD;
				//check if constant
				if (dvar.location == AccVar.L_CONSTDC || dvar.location == AccVar.L_CONSTTR || dvar.location == AccVar.L_CONST)
				{
					if (dvar.location == AccVar.L_CONSTDC && !dvar.resolveConstant(dvar.owner, ctx))
						return false; //output already done
					if (dvar.location != AccVar.L_CONST)
					{
						printPos(ctx, "unresolvable or cyclic initialization of variable");
						return false;
					}
					if (dvar.init != null && (constType = dvar.init.calcConstantType(ctx)) == 0 && !(constObject = dvar.init.isCompInitConstObject(ctx)))
					{
						printPos(ctx, "unknown type of constant variable");
						return false;
					}
					ExConstInitObj coi;
					if (constObject && (!dvar.init.isCompInitConstObject(ctx) || (coi = dvar.init.getConstInitObj(ctx)) == null || (dest = coi.dest) == null))
					{
						compErr(ctx, "invalid constant object");
						return false;
					}
				}
				//check static access
				if ((dvar.modifier & Modifier.M_STAT) == 0)
				{
					if ((inMthd.modifier & Modifier.M_STAT) != 0)
					{
						printPos(ctx, "can not access dynamic variable in static context");
						return false;
					}
				}
				else
				{
					if ((inUnit != dvar.owner || (inMthd.modifier & Modifier.M_STAT) == 0) && (constObject || (dvar.modifier & Modifier.M_FIN) == 0))
					{
						ensureClassContext = true;
						qid = dvar.owner.getQIDTo();
						if (ctx.dynaMem)
							importedClass = unitContext.getRefUnit(dvar.owner, true);
					}
				}
				//everything ok
				getTypeOf(dvar.type);
			}
			if (ctx.relations != null && dest != null)
				ctx.relations.addRelation(dest.owner, null, dest, unitContext, mthdContext);
		}
		//else: local and parameter variable need not to be checked for access level
		//check initialization
		if (dest != null && (resolveFlags & RF_CHECKREAD) != 0 && (dest.modifier & Modifier.MF_ISWRITTEN) == 0)
		{
			printPos(ctx, NOTINITIALIZED1);
			ctx.out.print(dest.name);
			ctx.out.print(NOTINITIALIZED2);
			return false;
		}
		//everything OK
		return true;
	}
	
	public int getConstIntValue(Context ctx)
	{
		Expr init;
		if ((init = dest.getInitExpr(ctx)) == null)
		{
			compErr(ctx, "init expr of dest is null in getConstIntValue");
			return 0;
		}
		return init.getConstIntValue(ctx);
	}
	
	public long getConstLongValue(Context ctx)
	{
		Expr init;
		if ((init = dest.getInitExpr(ctx)) == null)
		{
			compErr(ctx, "init expr of dest is null in getConstLongValue");
			return 0;
		}
		return init.getConstLongValue(ctx);
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return constObject ? dest.getConstInitObj(ctx) : null;
	}
	
	private boolean resolveInVar(Vrbl vars, Context ctx)
	{
		while (vars != null)
		{
			if (id.equals(vars.name))
			{
				dest = vars;
				getTypeOf(dest.type);
				return true;
			}
			vars = vars.nextVrbl;
		}
		return false;
	}
	
	private boolean resolveInParam(Param vars, Context ctx)
	{
		while (vars != null)
		{
			if (id.equals(vars.name))
			{
				dest = vars;
				getTypeOf(dest.type);
				return true;
			}
			vars = vars.nextParam;
		}
		return false;
	}
	
	private boolean resolveInInner(Unit unit, Context ctx)
	{
		while (unit != null)
		{
			if (id.equals(unit.name))
			{
				qid = unit.getQIDTo();
				baseType = StdTypes.T_DESC;
				return true;
			}
			if (resolveInInner(unit.innerUnits, ctx))
				return true;
			unit = unit.nextUnit;
		}
		return false;
	}
	
	private boolean resolveInUnit(Unit unit, Context ctx)
	{
		if ((dest = unit.searchVariable(id, ctx)) == null)
			return false;
		qid = unit.getQIDTo();
		getTypeOf(dest.type);
		return true;
	}
	
	private boolean resolveInPackage(Pack pack, boolean allowPacks)
	{
		Pack packDest;
		Unit unitDest;
		
		if (pack == null)
			return false;
		if (allowPacks && (packDest = pack.searchSubPackage(id)) != null)
		{
			qid = packDest.getQIDTo();
			baseType = StdTypes.T_PACK;
			return true;
		}
		if ((unitDest = pack.searchUnit(id)) != null)
		{
			qid = unitDest.getQIDTo();
			baseType = StdTypes.T_DESC;
			return true;
		}
		return false;
	}
	
	private boolean resolveInImptList(QualIDList impt, boolean packagesNotUnits)
	{
		while (impt != null)
		{
			if (packagesNotUnits)
			{
				if (impt.qid.type == QualID.Q_IMPORTPACK)
				{
					if (resolveInPackage(impt.qid.packDest, true))
						return true;
				}
			}
			else
			{
				if (impt.qid.type == QualID.Q_IMPORTUNIT)
				{
					if (impt.qid.unitDest.name.equals(id))
					{
						qid = impt.qid;
						baseType = StdTypes.T_DESC;
						return true;
					}
				}
			}
			impt = impt.nextQualID;
		}
		return false;
	}
	
	public void genOutput(Context ctx)
	{ //expression that consists only of this ExVar -> value is not of interest
		compErr(ctx, "ExVar.genOutput called which has no effect");
	}
}
