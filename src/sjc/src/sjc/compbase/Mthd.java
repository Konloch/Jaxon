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

import sjc.debug.CodePrinter;
import sjc.osio.TextPrinter;

/**
 * Mthd: platform and language independent functionality of methods to be called
 *
 * @author S. Frenz
 * @version 151031 added support for assembler attaching
 * version 120923 added support for code printer
 * version 100929 added support for implicit base type conversions in calls
 * version 100902 added isOverloadedBy, optimized checkParamConversions
 * version 100826 added code for in-system recompilation
 * version 100505 simplified checkConversions and renamed it to checkParamConversions
 * version 100319 replaced "inInline" by "inGenOutput"
 * version 100115 adopted codeStart-movement
 * version 091123 removed no longer needed symHint
 * version 091116 simplified signatures
 * version 091111 added support for instance final-var-init, optimized resolve-signature
 * version 091109 removed unwrittenVars
 * version 091104 added lineInCodeOffset
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090918 added M_PACP
 * version 090718 added unwrittenVars
 * version 090508 removed dummy implementation for flow analysis as it is integrated in resolve
 * version 090505 added dummy implementation for doFlowAnalysis
 * version 090219 added flag for synchronized
 * version 090207 added copyright notice
 * version 080706 added MA_INTFMD
 * version 080610 got vars from JMthd, added Throwable handling
 * version 080603 added throws-list
 * version 080414 added initVars in resolve prototype to enable correct redirect checking
 * version 071001 added "native" flag and variable containing address for native resolving
 * version 070913 removed getLocalVrbls
 * version 070731 renamed id to name
 * version 070727 changed id from PureID to String, added symHint
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070615 changed handling of addresses that need fixup
 * version 070527 added inlArrSize and flag to detect explicit initialization
 * version 070521 added flag for "method code needed"
 * version 070509 added flag for "descriptor entry needed"
 * version 070327 optimized enterOutputAddr
 * version 070127 added initVars in genOutput to support initialized instance variables
 * version 070114 added flags to optimize visibility level, reduced access level where possible
 * version 061231 added flag to avoid recursive inlining
 * version 061228 added stmtCnt
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public abstract class Mthd extends Token
{
	//default relOff
	public final static int INV_RELOFF = 0x7FFFFFFF;
	
	public String name;
	public TypeRef retType;
	public Param param;
	public Vrbl vars; //changes during resolving, always containing current var-view
	public VrblList checkInitVars; //usually null, set to unwritten unit vars for constructor
	public Mthd nextMthd;
	public Unit owner;
	public int modifier, marker; //marker is initialized to 0
	public int relOff;
	public boolean isConstructor; //default: not a constructor
	public Mthd ovldMthd; //will be null if the method is not overloaded
	public Mthd redirect; //will be null if the method is not redirected
	public Vrbl nativeAddress; //will be null if method if not native
	public QualIDList throwsList; //will be null if method does not throw anything
	//required fields for interface-checks, overloading and code-generation
	public int parSize, parCnt, varSize, retRegType, codeSize, stmtCnt;
	public boolean inGenOutput;
	public Object outputLocation;
	//required fields for line-number information
	public int[] lineInCodeOffset;
	//required field for attached assembler output
	public String asmCode;
	
	private AddrList fixupList;
	protected boolean isResolved;
	
	public Mthd(String in, int im, int fid, int il, int ic)
	{
		super(fid, il, ic);
		name = in;
		modifier = im;
		relOff = INV_RELOFF;
	}
	
	public abstract Mthd copy();
	
	public abstract boolean checkNameAndType(Unit owner, Context ctx);
	
	public abstract boolean resolve(Context ctx);
	
	public abstract void genOutput(Context ctx);
	
	public abstract void genInlineOutput(Context ctx);
	
	public void printCode(CodePrinter prnt)
	{
		prnt.reportError(this, "method does not support code printing");
	}
	
	public void resetResolveState()
	{
		varSize = 0;
		stmtCnt = 0;
		isResolved = false;
	}
	
	public void printSig(TextPrinter v)
	{
		if (retType != null)
			retType.printType(v);
		else
			v.print("#constr#");
		v.print(" ");
		printNamePar(v);
	}
	
	public void printNamePar(TextPrinter v)
	{
		Param par = param;
		
		v.print(name);
		v.print("(");
		if (par != null)
			while (true)
			{
				par.type.printType(v);
				par = par.nextParam;
				if (par != null)
					v.print(',');
				else
					break;
			}
		v.print(")");
	}
	
	public int checkParamConversions(FilledParam cmpPar, boolean doBaseConversions, Context ctx)
	{
		Param myPar;
		int cmpRes, count = 0;
		
		myPar = param;
		while (myPar != null && cmpPar != null)
		{
			cmpRes = cmpPar.expr.compareType(myPar.type, true, ctx);
			if (cmpRes == TypeRef.C_NP || cmpRes == TypeRef.C_TT)
			{
				if (!doBaseConversions || cmpPar.expr.getBaseTypeConvertionType(myPar.type, false) == null)
				{
					return -1; //parameter not convertable to our required type
				}
			}
			if (cmpRes != TypeRef.C_EQ)
				count++;
			myPar = myPar.nextParam;
			cmpPar = cmpPar.nextParam;
		}
		if (myPar == null && cmpPar == null)
			return count; //everything matches, return needed conversions
		return -1; //different param-count
	}
	
	public boolean checkVarWriteState(Token checkPosition, Context ctx)
	{
		VrblList vrblList = owner.writeCheckFinalVars;
		boolean all = true;
		while (vrblList != null)
		{
			if ((vrblList.vrbl.modifier & Modifier.MF_ISWRITTEN) == 0)
			{
				if (!all)
					ctx.out.println(); //already printed a variable
				checkPosition.printPos(ctx, "final variable ");
				ctx.out.print(vrblList.vrbl.name);
				ctx.out.print(" needs to be initialized in constructor");
				all = false;
			}
			vrblList = vrblList.next;
		}
		return all;
	}
	
	public boolean matches(Mthd cmp, Context ctx)
	{
		Param myPar, cmpPar;
		
		if (!name.equals(cmp.name))
			return false; //name different
		myPar = param;
		cmpPar = cmp.param;
		while (myPar != null && cmpPar != null)
		{
			if (myPar.type.compareType(cmpPar.type, false, ctx) != TypeRef.C_EQ)
				return false; //other signature
			myPar = myPar.nextParam;
			cmpPar = cmpPar.nextParam;
		}
		return myPar == null && cmpPar == null; //everything matches
		//different param-count
	}
	
	public boolean isOverloadedBy(Mthd overloader)
	{
		while (overloader.ovldMthd != null)
		{
			if (overloader.ovldMthd == this)
				return true;
			overloader = overloader.ovldMthd;
		}
		return false;
	}
	
	public boolean handlesThrowable(Token whom, Unit thrown, Context ctx)
	{
		QualIDList thr;
		
		if (thrown == null)
		{
			compErr(ctx, "uninitialized thrown in Mthd.handlesThrowable");
			return false;
		}
		if (!ctx.excChecked.isParent(thrown, ctx))
			return true; //not a checked Throwable
		//check declared throwables
		thr = throwsList;
		while (thr != null)
		{
			if (thr.qid.unitDest.isParent(thrown, ctx))
				return true;
			thr = thr.nextQualID;
		}
		//not declared, print error (not done by caller)
		whom.printPos(ctx, "thrown and checked exception ");
		ctx.out.print(thrown.name);
		ctx.out.print(" is not declared");
		return false;
	}
	
	public void enterCodeRef(Object loc, int offset, Context ctx)
	{
		AddrList fixupObject;
		
		if (outputLocation != null)
			ctx.arch.putCodeRef(loc, offset, outputLocation, ctx.codeStart);
		else
		{
			if (ctx.freeAddrLists == null)
				fixupObject = new AddrList();
			else
			{
				fixupObject = ctx.freeAddrLists;
				ctx.freeAddrLists = ctx.freeAddrLists.next;
			}
			fixupObject.loc = loc;
			fixupObject.off = offset;
			fixupObject.next = fixupList;
			fixupList = fixupObject;
		}
	}
	
	public void enterOutputAddr(Object validOutputLocation, Context ctx)
	{
		AddrList toRelease;
		
		outputLocation = validOutputLocation;
		while ((toRelease = fixupList) != null)
		{
			fixupList = fixupList.next;
			ctx.arch.putCodeRef(toRelease.loc, toRelease.off, outputLocation, ctx.codeStart);
			toRelease.next = ctx.freeAddrLists;
			ctx.freeAddrLists = toRelease;
		}
	}
}
