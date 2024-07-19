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
 * JUnit: java-specific behaviour of units
 *
 * @author S. Frenz
 * @version 121020 added support for getSourceType
 * version 120501 got annotation check from Clss to do it before variable size checking
 * version 120228 cleaned up "import sjc." typo
 * version 120227 cleaned up "package sjc." typo
 * version 110624 adopted changed Context
 * version 110211 fixed resolving of final but non-static variables
 * version 101231 adopted changed Unit
 * version 101015 adopted changed Expr
 * version 100826 added code for in-system compilation
 * version 100512 adopted changed Modifier/Marks
 * version 100504 adopted changed Unit
 * version 100409 adopted changed TypeRef
 * version 100408 reformatted source, made use of compError instead of local error output
 * version 100401 added support for ctx.flashClass
 * version 100114 reorganized constant object handling
 * version 091215 moved writeVarsAndConstObjDebug to Unit
 * version 091209 moved generation of constant objects into the corresponding ExConstObj classes
 * version 091116 adopted simplified Mthd-signature
 * version 091112 added support for implicit conversion and explicitTypeConversion
 * version 091111 adopted changed Mthd-resolve-signature
 * version 091026 adopted changed minimumAccessLevel return value
 * version 091021 adopted changed modifier declarations
 * version 091005 moved container of constant objects to compbase package, adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090918 added support for package private
 * version 090724 adopted changed Expr
 * version 090718 added optimization for zero-initialized instance variables, adopted changed Expr, added support for writeCheckFinalVars
 * version 090508 fixed unit search order 1) imported units, 2) local package, 3) imported packages
 * version 090301 adopted move of allocateMultiArray to Context
 * version 090207 added copyright notice
 * version 080703 adopted changed symInfo-debug-interface, added recursive parent-search
 * version 080630 added search of inner unit in imported units
 * version 080624 added search of unit in outer class if existing
 * version 080622 added support for static class initialization
 * version 080616 added dependsOn-check for constant objects, added support for constant new
 * version 080614 got strings and arrays from Clss as it may be used in Intf
 * version 080506 added support for implicit conversion in class variable initialization
 * version 080414 got instInitVars from Clss, adopted change in Mthd
 * version 080122 moved $outer-checks to Clss
 * version 080106 added support for direct non-interface descendants in variable initialization
 * version 080105 added support for extended inner units
 * version 071222 added support for inner units
 * version 070909 optimized signature of Expr.resolve
 * version 070903 adopted changes in AccVar.checkNameAgainst*
 * version 070731 adopted change of QualID[] to QualIDList
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 fixed unit variable location setting
 * version 070501 moved prepareOutput to compbase.Unit
 * version 070303 added support for movable indirect scalars
 * version 070128 fixed static/dynamic check for initialized variables
 * version 070127 optimized variable initialization, added initialization of instance variables
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061203 optimized calls to printPos and compErr
 * version 061027 added support for STRUCT
 * version 060723 added support for extension of explicit standard constructor in parent
 * version 060707 added type-check for class-variables
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public abstract class JUnit extends Unit
{
	private final static int R_NOTSTARTED = 0; //needs to be 0 for initialization of ...ResolveState
	private final static int R_MODSTARTED = 1;
	private final static int R_MODVALID = 2;
	private final static int R_STARTED = 3;
	private final static int R_SUCCESS = 4;
	private final static int R_ERROR = 5;
	
	//required fields for resolving
	protected QualIDList impt;
	private int intfResolveState, mthdResolveState; //...ResolveState==R_NOTSTARTED initialized automatically
	
	protected JUnit(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public boolean hasValidInterface()
	{
		return intfResolveState == R_SUCCESS;
	}
	
	public QualIDList getImportList()
	{
		return impt;
	}
	
	protected abstract boolean resolveIntfExtsIpls(Context ctx);
	
	protected abstract boolean checkInstVarInit(Vrbl var, Context ctx);
	
	protected abstract boolean resolveMthdExtsIpls(Context ctx);
	
	protected abstract boolean checkDeclarations(Context ctx);
	
	protected boolean checkAnnotations(Vrbl v, FilledAnno a, Context ctx)
	{
		a.printPos(ctx, "annotation not supported here");
		return false; //default: no annotation allowed
	}
	
	protected boolean validateModifierAfterImportResolve(Context ctx)
	{
		return true; //default: nothing to do
	}
	
	public Unit searchUnitInView(StringList what, boolean isRecursion)
	{
		Unit res;
		StringList cur;
		
		//check ourself
		if (what.next == null && what.str.equals(name))
			return this;
		//check inner units
		cur = what;
		if (cur.next != null && cur.str.equals(name))
			cur = cur.next; //full qualified type with ourself in front
		res = innerUnits;
		while (res != null)
		{
			if (cur.str.equals(res.name))
			{ //found current inner unit
				if ((cur = cur.next) == null)
					return res; //found all
				res = res.innerUnits; //get next level
			}
			else
				res = res.nextUnit; //try next unit
		}
		//find in outer units
		if (outerUnit != null)
			return outerUnit.searchUnitInView(what, false);
		//find in imports if not recursive search
		if (!isRecursion)
		{
			//find in unit imports
			if ((res = searchUnitInImports(what, false)) != null)
				return res;
			//find in package
			if ((res = pack.packDest.searchUnit(what)) != null)
				return res;
			//find in package imports
			if ((res = searchUnitInImports(what, true)) != null)
				return res;
		}
		if (extsID != null && extsID.unitDest != null)
			return extsID.unitDest.searchUnitInView(what, true);
		//not found
		return null;
	}
	
	public void resetResolveState(boolean newOutput, Context ctx)
	{
		if (newOutput)
		{
			offsetsAssigned = false;
			outputGenerated = false;
			outputLocation = null;
			constObjList = null;
		}
		if (extsID != null && extsID.unitDest == ctx.langRoot)
			extsID = null;
		mthdResolveState = intfResolveState = R_NOTSTARTED;
	}
	
	private Unit searchUnitInImports(StringList what, boolean packagesNotUnits)
	{
		Unit res;
		QualIDList list;
		StringList cur;
		
		list = impt;
		while (list != null)
		{
			if (packagesNotUnits)
			{ //search only in packages
				if (list.qid.type == QualID.Q_IMPORTPACK)
				{ //import package
					if ((res = list.qid.packDest.searchUnit(what)) != null)
						return res;
				}
			}
			else
			{ //search only in units
				if (list.qid.type == QualID.Q_IMPORTUNIT)
				{ //import unit
					if (what.str.equals(list.qid.unitDest.name))
					{
						if (what.next == null)
							return list.qid.unitDest; //direct match
						//search inner units
						cur = what.next;
						res = list.qid.unitDest.innerUnits;
						while (res != null)
						{
							if (cur.str.equals(res.name))
							{ //found current inner unit
								if ((cur = cur.next) == null)
									return res; //found all
								res = res.innerUnits; //get next level
							}
							else
								res = res.nextUnit; //try next unit
						}
					}
				}
			}
			list = list.nextQualID;
		}
		return null;
	}
	
	public boolean validateModifier(Context ctx)
	{
		boolean error = false;
		QualIDList qidList;
		
		switch (intfResolveState)
		{
			case R_NOTSTARTED:
				break; //normal mode
			case R_MODSTARTED: //this indicates cyclic call of CompileUnit.validateModifier
				ctx.out.println("error: cyclic unit.validateModifier - check extends- and implements-statements");
				ctx.out.print("in unit ");
				ctx.out.println(name);
				intfResolveState = R_ERROR;
				//no break, return false
			case R_ERROR:
				return false;
			case R_MODVALID:
			case R_STARTED:
			case R_SUCCESS:
				return true;
		}
		intfResolveState = R_MODSTARTED;
		//test for special names
		if (SJava.isSpecialName(name) && this != ctx.structClass && this != ctx.flashClass)
		{ //internally assigned STRUCT and FLASH are valid
			printPos(ctx, "invalid use of keyword for class ");
			ctx.out.println(name);
			intfResolveState = R_ERROR;
			return false;
		}
		//check if root entered
		if (ctx.root == null)
		{
			ctx.out.print("error: resolving invalid unit ");
			ctx.out.print(name);
			ctx.out.println(" not possible");
			intfResolveState = R_ERROR;
			return false;
		}
		//check import-statements
		qidList = impt;
		while (qidList != null)
		{
			if (qidList.qid.type == QualID.Q_IMPORTPACK)
				qidList.qid.packDest = ctx.root.searchSubPackage(qidList.qid.name, false);
			else
				qidList.qid.unitDest = ctx.root.searchUnit(qidList.qid.name);
			if (qidList.qid.packDest == null && qidList.qid.unitDest == null)
			{
				qidList.qid.printPos(ctx, "could not resolve import-statement ");
				qidList.qid.printFullQID(ctx.out);
				ctx.out.print(" in unit ");
				ctx.out.println(name);
				error = true;
			}
			else if (ctx.relations != null)
				ctx.relations.addImport(qidList.qid.unitDest, this);
			qidList = qidList.nextQualID;
		}
		//get state of ctx.explicitConversion
		if (ctx.explicitTypeConversion)
			marker |= Marks.K_EXPC;
		//basic checks done, do unit dependant checks
		if (error || !validateModifierAfterImportResolve(ctx))
		{
			intfResolveState = R_ERROR;
			return false;
		}
		intfResolveState = R_MODVALID;
		return true;
	}
	
	public boolean resolveInterface(Context ctx)
	{
		boolean error = false;
		Vrbl var;
		Mthd mthd;
		VrblList vrblList;
		
		//check if already resolved
		switch (intfResolveState)
		{
			case R_NOTSTARTED:
			case R_MODSTARTED: //this should never happen as validateModifier should always be called before resolveInterface
				compErr(ctx, "resolveInterface called before modifier is fixed");
				ctx.out.println();
				intfResolveState = R_ERROR;
				return false;
			case R_MODVALID:
				break; //normal mode
			case R_STARTED: //this indicates cyclic call of CompileUnit.resolveInterface
				ctx.out.println("error: cyclic unit.resolveInterface - check extends- and implements-statements");
				ctx.out.print("in unit ");
				ctx.out.println(name);
				intfResolveState = R_ERROR;
				return false;
			case R_SUCCESS:
				return true; //already done, nothing to do
			case R_ERROR:
				return false; //silently abort, we printed an error-message already
			default: //this should never happen
				compErr(ctx, "invalid CompileUnit.intfResolveState");
				intfResolveState = R_ERROR;
				return false;
		}
		//start resolving
		intfResolveState = R_STARTED;
		//abort now on all of the following errors
		if (error //abort if there was an error in import-statements
				|| !resolveIntfExtsIpls(ctx) //check extends and implements, has to be done differently in Clss and Intf
				|| (outerUnit != null && !outerUnit.resolveInterface(ctx)))
		{ //check outer if existing
			intfResolveState = R_ERROR;
			return false;
		}
		//check methods
		mthd = mthds;
		while (mthd != null)
		{
			if (!mthd.checkNameAndType(this, ctx))
			{
				ctx.out.print(" in unit ");
				ctx.out.println(name);
				intfResolveState = R_ERROR;
				return false;
			}
			mthd = mthd.nextMthd;
		}
		//check vars
		var = vars;
		while (var != null)
		{
			//check annotations
			FilledAnno anno = var.getAnnotation();
			if (anno != null && !checkAnnotations(var, anno, ctx))
			{
				ctx.out.print(" in class ");
				ctx.out.println(name);
				intfResolveState = R_ERROR;
				return false;
			}
			//check basics, enter size
			if (!var.type.resolveType(this, ctx) || !var.checkNameAgainstVrbl(vars, ctx) || !var.checkNameAgainstUnits(innerUnits, ctx) || !var.enterSize(var.location == Vrbl.L_NOTRDY ? Vrbl.L_UNIT : var.location, ctx) || ((var.modifier & Modifier.M_STAT) == 0 && var.init != null && !var.init.isConstZero() && !checkInstVarInit(var, ctx)))
			{
				ctx.out.print(" in unit ");
				ctx.out.println(name);
				intfResolveState = R_ERROR;
				return false;
			}
			//do specials if variable is final and non-static and not initialized
			if (var.init == null && (var.modifier & (Modifier.M_FIN | Modifier.M_STAT)) == Modifier.M_FIN)
			{
				vrblList = new VrblList(var);
				vrblList.next = writeCheckFinalVars;
				writeCheckFinalVars = vrblList;
			}
			//ok, get next variable
			var = var.nextVrbl;
		}
		//check MethodTable and check implementation of interfaces
		if (!checkDeclarations(ctx))
		{
			intfResolveState = R_ERROR;
			return false;
		}
		//everything OK
		intfResolveState = R_SUCCESS;
		return true;
	}
	
	public boolean resolveMethodBlocks(Context ctx)
	{
		Vrbl var;
		Mthd mthd;
		boolean success = true;
		
		//to enable super-redirection and other optimizations, ensure to have parents resolved already
		switch (mthdResolveState)
		{
			case R_NOTSTARTED:
				break; //normal mode
			case R_STARTED: //this indicates cyclic call of CompileUnit.resolveMethodBlocks
				ctx.out.println("error: cyclic unit.resolveMethodBlocks - check extends- and implements-statements");
				ctx.out.print("in unit ");
				ctx.out.println(name);
				mthdResolveState = R_ERROR;
				return false;
			case R_SUCCESS:
				return true; //already done, nothing to do
			case R_ERROR:
				return false; //silently abort, we printed an error-message already
			default: //this should never happen
				ctx.out.println("### invalid CompileUnit.mthdResolveState ###");
				mthdResolveState = R_ERROR;
				return false;
		}
		mthdResolveState = R_STARTED;
		if (!resolveMthdExtsIpls(ctx))
		{
			mthdResolveState = R_ERROR;
			return false;
		}
		//initialization of variables belongs to code
		var = vars;
		while (var != null)
		{
			if ((var.modifier & (Modifier.M_FIN | Modifier.M_STAT)) != (Modifier.M_FIN | Modifier.M_STAT))
			{ //this is a not constant variable (constants resolved seperately)
				if (var.init != null)
				{ //check if variable initialized
					mthd = (var.modifier & Modifier.M_STAT) != 0 ? initStat : initDyna; //get static/dynamic context
					if (!var.init.resolve(this, mthd, Expr.RF_CHECKREAD, var.type, ctx))
					{
						ctx.out.print(" in unit ");
						ctx.out.println(name);
						success = false;
					}
					else if (!SJava.checkVarInitType(var, (marker & Marks.K_EXPC) != 0, this, ctx))
					{
						ctx.out.println(" in initialization of unit-variable");
						success = false;
					}
				}
			}
			var = var.nextVrbl;
		}
		//resolve method-blocks
		mthd = mthds;
		while (mthd != null)
		{
			if (!mthd.resolve(ctx))
			{
				ctx.out.print(" in unit ");
				ctx.out.println(name);
				success = false;
			}
			mthd = mthd.nextMthd;
		}
		if (initStat != null && (initStat.modifier & Modifier.M_NDCODE) != 0 && !initStat.resolve(ctx))
		{
			ctx.out.print(" in static init of unit ");
			ctx.out.println(name);
			success = false;
		}
		//done, success will be true if everything was resolved
		if (success)
		{
			mthdResolveState = R_SUCCESS;
			return true;
		}
		mthdResolveState = R_ERROR;
		return false;
	}
	
	protected boolean resolveExtsIplsQID(QualID qid, Context ctx)
	{ //called by Clss and TIntf only
		int accessLevel;
		//search for unit
		if ((qid.unitDest = searchUnitInView(qid.name, false)) != null //use normal search function
				|| (qid.unitDest = ctx.defUnits.searchUnit(qid.name)) != null //search in default units
				|| (qid.unitDest = ctx.root.searchUnit(qid.name)) != null)
		{//search in root package
			if (qid.unitDest == this)
			{
				qid.printPos(ctx, "cannot extend/implement itself");
				return false;
			}
			if ((accessLevel = minimumAccessLevel(qid, qid.unitDest.name, qid.unitDest, qid.unitDest.modifier, true, ctx)) == Modifier.M_ERROR)
				return false;
			qid.unitDest.modifier |= accessLevel;
			return true; //found destination
		}
		//not found
		qid.printPos(ctx, "could not resolve ");
		qid.printFullQID(ctx.out);
		ctx.out.print(" in unit ");
		ctx.out.println(name);
		return false;
	}
	
	protected void fixDynaAddresses(Context ctx)
	{
		ExConstInitObj obj = constObjList;
		
		if (obj != null)
		{
			modifier |= Modifier.MA_ACCSSD;
			//enter information
			while (obj != null)
			{
				obj.dest.relOff = -(++statRelocTableEntries + clssRelocTableEntries) * ctx.arch.relocBytes;
				obj.dest.location = Vrbl.L_CLSSREL;
				obj = obj.nextConstInit;
			}
		}
	}
	
	public String getSourceType()
	{
		return "java";
	}
}
