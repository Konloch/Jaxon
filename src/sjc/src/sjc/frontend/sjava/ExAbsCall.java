/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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

/**
 * ExAbsCall: abstract expression with basic resolving for all types of calls
 *
 * @author S. Frenz
 * @version 120404 added extra parameter to beautify super visibility condition check, changed error message
 * version 120402 added special visibility condition for super-call
 * version 110219 added support for native calls in embedded mode
 * version 101231 adopted changed Unit
 * version 101210 adopted changed Architecture
 * version 101202 fixed offset calculation in call of native methods
 * version 101128 added check for native callback methods
 * version 101015 adopted changed Expr
 * version 100929 added support for implicit base type conversions
 * version 100902 fixed overloaded-check in searchCalledMethod
 * version 100512 adopted changed Modifier/Marks
 * version 100505 optimized searchCalledMethod, adopted changed Mthd
 * version 100421 added check for interrupt methods
 * version 100319 adopted changed Mthd
 * version 091026 adopted changed minimumAccessLevel return value
 * version 091021 adopted changed minimumAccessLevel checks and modifier declarations
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090916 stopped method searching before switching to outer unit if a matching method was found
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090619 adopted change in Architecture
 * version 090218 made use of centralized access level check
 * version 090207 added copyright notice
 * version 080712 fixed implicit conversion of interface parameters
 * version 080607 clarified error output
 * version 080629 added check to support noInlineMthdObj-option
 * version 080614 adopted changed Unit.searchVariable
 * version 080610 added throwable reporting
 * version 080106 added support for constructors of anonymous inner classes
 * version 080105 adopted changed signature of Unit.isParent
 * version 071227 cleaned up checkImplicitConverts, added support for inner units
 * version 071001 added support for native method calls
 * version 070909 optimized signature of Expr.resolve
 * version 070812 added support for float and double
 * version 070731 adopted renaming of id to name
 * version 070727 adopted change of Mthd.id from PureID to String
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 made genOutputSinglePar static and optimized parameter access
 * version 070521 setting M_NDDESC of overloaded methods
 * version 070506 better error message
 * version 070114 fixed access control for methods, reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070101 adopted change in genCall
 * version 061231 added check to avoid recursive inlining
 * version 061225 support for inline code instead of call
 * version 061211 setting flag for called methods
 * version 061203 optimized calls to printPos and compErr
 * version 061111 minor changes
 * version 061030 changed detection of indirectCall
 * version 060721 added optimization for pushed constants
 * version 060628 added support for static compilation
 * version 060621 implicit conversion for object->interface
 * version 060607 initial version
 */

public abstract class ExAbsCall extends Expr
{
	private static final String UNCLEARREGS = "ExAbsCall.genOutputPar has not enough free regs (reduce complexity)";
	
	protected FilledParam par;
	protected Mthd dest;
	protected int outerLevel; //used for access of outer units
	protected Unit outerAccessStart; //used for access of outer units
	protected boolean implContextSwitch;
	private boolean callViaIntf;
	
	protected ExAbsCall(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	protected boolean resolveInMthd(String name, boolean asConstr, Unit inUnit, Mthd inMthd, Unit unitContext, Mthd mthdContext, int resolveFlags, boolean partOfSuper, Context ctx)
	{
		FilledParam pa;
		Mthd omthd;
		QualIDList curThrows;
		int accessLevel;
		
		//test for specials
		if (SJava.isSpecialName(name))
		{
			printPos(ctx, "invalid use of reserved name \"");
			ctx.out.print(name);
			ctx.out.print("\" in call");
			return false;
		}
		//resolve parameter
		pa = par;
		while (pa != null)
		{
			if (!pa.expr.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, null, ctx))
				return false;
			pa = pa.nextParam;
		}
		//check if implicit constructor
		if (asConstr && !inUnit.explConstr)
		{
			if (par != null)
			{
				printPos(ctx, "implicit constructor must not have parameters");
				return false;
			}
			if ((inUnit.modifier & Modifier.M_INDIR) != 0)
			{
				printPos(ctx, "can not make instance of interface");
				return false;
			}
			callViaIntf = false;
			dest = inUnit.initDyna;
		}
		//search matching method/constructor
		else
		{
			if (inUnit.name.charAt(0) == '$' && name.equals(inUnit.name))
			{ //check if constructor for anonymous class
				if (inUnit.extsID.unitDest == null)
				{
					printPos(ctx, "invalid constructor call inside anonymous class");
					return false;
				}
				name = inUnit.extsID.unitDest.name; //get name of parent
			}
			//search destination
			callViaIntf = (inUnit.modifier & Modifier.M_INDIR) != 0;
			if ((dest = searchCalledMethod(inUnit, asConstr, (mthdContext.marker & Marks.K_EXPC) == 0, name, par, ctx)) == null)
				return false;
			if (!checkImplicitConverts(unitContext, ctx))
				return false;
		}
		//check modifiers
		if (((inMthd.modifier & Modifier.M_STAT) != 0) && (dest.modifier & Modifier.M_STAT) == 0)
		{
			printPos(ctx, "can not call dynamic method in static context");
			return false;
		}
		if ((dest.marker & (Marks.K_INTR | Marks.K_NTCB)) != 0)
		{
			printPos(ctx, "can not call interrupt method ");
			dest.printNamePar(ctx.out);
			ctx.out.print(" directly");
			return false;
		}
		if ((accessLevel = unitContext.minimumAccessLevel(dest, dest.name, dest.owner, dest.modifier, inUnit == unitContext || partOfSuper, ctx)) == Modifier.M_ERROR)
			return false;
		dest.modifier |= accessLevel | Modifier.MA_ACCSSD;
		omthd = dest.ovldMthd;
		while (omthd != null)
		{
			omthd.modifier |= Modifier.M_NDDESC;
			omthd = omthd.ovldMthd;
		}
		//check throwables
		if ((curThrows = dest.throwsList) != null)
			while (curThrows != null)
			{
				if (ctx.excChecked.isParent(curThrows.qid.unitDest, ctx) && !mthdContext.handlesThrowable(this, curThrows.qid.unitDest, ctx))
					return false;
				curThrows = curThrows.nextQualID;
			}
		//everything OK
		return true;
	}
	
	private Mthd searchCalledMethod(Unit inUnit, boolean asConstr, boolean allowBaseTypeConv, String mthdName, FilledParam par, Context ctx)
	{
		Mthd mthd, dest = null;
		int parCnt = 0, cur, best = -1, found = 0, curLevel = 0;
		FilledParam tmpPar;
		Unit curClass = inUnit, lastInner = inUnit;
		
		if (asConstr && (inUnit.modifier & Modifier.M_INDIR) != 0)
		{
			printPos(ctx, "indirect unit does not have a constructor");
			return null;
		}
		tmpPar = par;
		while (tmpPar != null)
		{
			parCnt++;
			tmpPar = tmpPar.nextParam;
		}
		mthd = inUnit.mthds;
		do
		{
			while (mthd != null)
			{
				if (asConstr == mthd.isConstructor //check only if method/constructor,
						&& mthd.parCnt == parCnt && mthdName.equals(mthd.name))
				{ //name and number of parameters match
					cur = mthd.checkParamConversions(par, allowBaseTypeConv && (mthd.marker & Marks.K_EXPC) == 0, ctx);
					if (cur != -1)
					{ //convertable
						if (best == -1 || cur < best)
						{ //nothing found or better match than match bevor, use current
							dest = mthd;
							if ((outerLevel = curLevel) > 0)
								outerAccessStart = inUnit;
							best = cur;
							found = 1;
						}
						else if (cur == best)
						{ //match with same count of conversions, possibly ambiguous
							//check if method is not overloaded => ambiguous
							if (!dest.isOverloadedBy(mthd) && !mthd.isOverloadedBy(dest))
								found++;
						}
					}
				}
				mthd = mthd.nextMthd;
			}
			if (dest != null && (dest.modifier & Modifier.M_STAT) != 0)
				break; //terminate search: found static destination
			if (curClass.extsID != null)
			{ //switch to parent
				mthd = (curClass = curClass.extsID.unitDest).mthds;
			}
			else if (lastInner.outerUnit != null && found == 0)
			{ //switch to last known class and switch to outer, if not method found so far
				mthd = (lastInner = curClass = lastInner.outerUnit).mthds;
				curLevel++;
			}
			else
				break; //terminate search: no other class to search in
		} while (curClass != null);
		if (found == 1)
			return dest;
		if (asConstr && par == null && inUnit.explStdConstr)
		{
			//there is an explicit standard constructor, but we did not find it => search parent
			return searchCalledMethod(inUnit.extsID.unitDest, true, allowBaseTypeConv, inUnit.extsID.unitDest.name, null, ctx);
		}
		printPos(ctx, null);
		if (asConstr)
			ctx.out.print(": constructor ");
		else
			ctx.out.print(": method ");
		ctx.out.print(mthdName);
		ctx.out.print("(");
		if (par != null)
			par.printTypes(ctx.out);
		if (found == 0)
		{
			ctx.out.print(") not found");
			if (asConstr)
				ctx.out.print(" (has parent an explicit constructor?)");
		}
		else
			ctx.out.print(") matches multiple times");
		return null;
	}
	
	protected void genOutputPar(Context ctx)
	{ //called from genOutputVal and ExDeref
		FilledParam pa;
		int off, reg, type;
		
		pa = par;
		if ((dest.modifier & Modifier.M_NAT) != 0)
		{ //special case: C-style parameter order
			ctx.arch.genReserveNativeStack(dest.parSize); //allocate space for parameters
			off = 0; //start at stack pointer offset +0
			while (pa != null)
			{
				type = pa.expr.getRegType(ctx);
				if (ctx.arch.prepareFreeReg(0, 0, 0, type) != 0)
				{
					pa.printPos(ctx, UNCLEARREGS);
					ctx.out.println();
					ctx.err = true;
					return;
				}
				reg = ctx.arch.allocReg();
				pa.expr.genOutputVal(reg, ctx);
				ctx.arch.genStoreNativeParameter(off, reg, type);
				ctx.arch.deallocRestoreReg(reg, 0, 0);
				pa = pa.nextParam;
				switch (type)
				{ //increase stack offset for next parameter
					case StdTypes.T_DPTR:
						off += ctx.arch.relocBytes * 2;
						break;
					case StdTypes.T_PTR:
						off += ctx.arch.relocBytes;
						break;
					default:
						off += getMinSize(type);
				}
				off = (off + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits;
			}
		}
		else
			while (pa != null)
			{ //normal case: generate and push parameters
				genOutputSinglePar(pa.expr, ctx); //generate current parameter
				pa = pa.nextParam;
			}
	}
	
	protected static void genOutputSinglePar(Expr paEx, Context ctx)
	{ //called from genOutputPar and ExNew
		int type, reg;
		
		type = paEx.getRegType(ctx);
		if (type == 0)
		{
			paEx.compErr(ctx, "parameter can not have result void");
			return;
		}
		switch (paEx.calcConstantType(ctx))
		{
			case T_INT:
				ctx.arch.genPushConstVal(paEx.getConstIntValue(ctx), type);
				break;
			case T_LONG:
				switch (type)
				{
					case T_LONG:
						ctx.arch.genPushConstDoubleOrLongVal(paEx.getConstLongValue(ctx), false);
						break;
					case T_DBL:
						ctx.arch.genPushConstDoubleOrLongVal(paEx.getConstLongValue(ctx), true);
						break;
					default:
						paEx.compErr(ctx, "unknown long constant type");
						return;
				}
				break;
			default:
				if (ctx.arch.prepareFreeReg(0, 0, 0, type) != 0)
				{
					paEx.printPos(ctx, UNCLEARREGS);
					ctx.out.println();
					ctx.err = true;
					return;
				}
				reg = ctx.arch.allocReg();
				paEx.genOutputVal(reg, ctx);
				ctx.arch.genPush(reg, type);
				ctx.arch.deallocRestoreReg(reg, 0, 0);
		}
	}
	
	protected void genOutputCall(int reg, int obj, Context ctx)
	{ //called from ExDeref and ExCall.genOutputVal (latter not for intf)
		int type;
		boolean forceInline;
		
		if ((dest.modifier & Modifier.M_NAT) != 0)
		{ //call native method
			ctx.arch.genCallNative(!ctx.embedded ? dest.nativeAddress.owner.outputLocation : ctx.ramLoc, dest.nativeAddress.relOff, ctx.dynaMem, dest.parSize, (dest.marker & Marks.K_NWIN) != 0);
		}
		else if (!callViaIntf)
		{ //normal call via object or class
			if (!implContextSwitch)
			{
				if (outerLevel > 0)
					loadOuterInstance(ctx); //caller must have saved instance context already
			}
			else if (ctx.dynaMem)
			{
				ctx.arch.genLoadUnitContext(ctx.arch.regClss, dest.relOff + ctx.arch.relocBytes);
			}
			if ((forceInline = (dest.marker & Marks.K_FINL) != 0) && ctx.arch.mayInline() && !dest.inGenOutput)
			{
				dest.genInlineOutput(ctx);
			}
			else
			{
				if (forceInline && ctx.noInlineMthdObj)
				{
					printPos(ctx, "non-inline call of inline-marked method ");
					dest.printNamePar(ctx.out);
					ctx.out.println(" required (remove mark or change compilation mode)");
					ctx.err = true;
					return;
				}
				if (!ctx.dynaMem && ((dest.modifier & (Modifier.M_STAT | Modifier.M_FIN | Modifier.M_PRIV)) != 0 || (dest.modifier & Modifier.M_OVERLD) == 0))
					ctx.arch.genCallConst(dest, dest.parSize);
				else
					ctx.arch.genCall(dest.relOff, ctx.arch.regClss, dest.parSize);
			}
		}
		else
		{ //call via interface-object (only from ExDeRef)
			ctx.arch.genCallIndexed(obj, ctx.rteSIntfMap.instScalarTableSize + dest.relOff * 4, dest.parSize);
		}
		//move result to desired register
		if (reg != 0 && (type = getRegType(ctx)) != 0)
			ctx.arch.genMoveFromPrimary(reg, type);
		//else: no desired register existing => void method or result not interesting
	}
	
	private void loadOuterInstance(Context ctx)
	{
		int level = 0, reg;
		Vrbl outer;
		Unit currentUnit = outerAccessStart;
		
		do
		{
			if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
			{
				printPos(ctx, UNCLEARREGS);
				ctx.out.println();
				ctx.err = true;
				return;
			}
			if (currentUnit == null || (outer = currentUnit.searchVariable(Unit.OUTERVARNAME, ctx)) == null)
			{
				compErr(ctx, "could not find outer variable");
				return;
			}
			reg = ctx.arch.allocReg();
			ctx.arch.genLoadVarVal(reg, ctx.arch.regInst, null, outer.relOff, StdTypes.T_PTR);
			ctx.arch.genLoadInstContext(reg);
			ctx.arch.deallocRestoreReg(reg, 0, 0);
			currentUnit = currentUnit.outerUnit;
		} while (++level < outerLevel);
	}
	
	private boolean checkImplicitConverts(Unit unitContext, Context ctx)
	{ //dest and par are valid at this time
		FilledParam fp = par;
		Param dp = dest.param;
		int cmpRes;
		ExEnc enc; //enclosure for implicit conversion
		
		while (fp != null)
		{
			if (dp == null)
			{
				compErr(ctx, "number of filledParam > number of declaredParam");
				return false;
			}
			if (dp.type.baseType != TypeRef.T_QID && dp.type.arrDim == 0)
			{ //this is the case which we dont support
				if (dp.type.baseType != fp.expr.baseType)
				{ //if we come to this point, implicit base type conversion was enabled
					if ((fp.expr = ExEnc.getConvertedResolvedExpr(fp.expr, dp.type, unitContext, ctx)) == null)
						return false;
				}
			}
			if (fp.expr.baseType == T_NULL)
			{
				if (dp.type.isIntfType())
					fp.expr.baseType = T_NDPT;
				else
					fp.expr.baseType = T_NNPT;
			}
			else
			{
				cmpRes = fp.expr.compareType(dp.type, true, ctx); //get type-compare
				if (cmpRes == TypeRef.C_OT && dp.type.isIntfType())
				{ //insert implicit conversion of object to interface
					enc = new ExEnc(fileID, line, col);
					enc.convertTo = dp.type;
					enc.ex = fp.expr;
					if (!enc.implConvResolve(unitContext, ctx))
					{
						ctx.out.print(" in inserted implicit conversion");
						return false;
					}
					fp.expr = enc;
				}
			}
			fp = fp.nextParam;
			dp = dp.nextParam;
		}
		return true;
	}
}
