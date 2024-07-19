/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2015, 2016 Stefan Frenz
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

import sjc.debug.DebugWriter;
import sjc.osio.TextPrinter;
import sjc.relations.RelationElement;

/**
 * Unit: basic abstraction for a class or interface
 *
 * @author S. Frenz
 * @version 161212 added printNameWithOuter with delimiter
 * version 151108 added allocation debug hint
 * version 121020 added getSourceType
 * version 120228 added support for validateModifier
 * version 120227 cleaned up "package sjc." typo
 * version 110615 fixed isParent check for inherited interfaces
 * version 101231 fixed access level check for protected
 * version 101218 replaced getTypeTo by getQIDTo
 * version 100923 fixed outerUnit check
 * version 100826 added code for in-system recompilation
 * version 100611 allowed access to other private unit in same outer unit
 * version 100512 added marker
 * version 100504 added getImportList
 * version 100428 added listAllInterfaceParents
 * version 100407 fixed check for destUnit==null in minimumAccessLevel
 * version 100331 adopted changed DebugWriter
 * version 100312 added support for second run of genConstObj
 * version 100223 added support for doNotCheckVisibility
 * version 100114 reorganized constant object handling
 * version 091215 got writeVarsAndConstObjDebug from JUnit
 * version 091209 merged constant object lists and got genConstObj
 * version 091123 removed no longer needed symHint
 * version 091116 adopted simplified Mthd-signature
 * version 091026 synchronized minimumAccessLevel return values with Modifier flags
 * version 091021 got minimumAccessLevel from TypeRef, removed Modifier-copies
 * version 091004 got container for constant objects from JUnit, added support for preferredTypes in var-resolving
 * version 091001 adopted changed memory interface
 * version 090718 added writeCheckFinalVars
 * version 090508 removed dummy implementation for flow analysis after integration into resolving
 * version 090506 removed temporarly added resolve-check in isParent-method
 * version 090505 added dummy implementation for doFlowAnalysis
 * version 090306 added constant for name of static class init method
 * version 090218 added isOuter method
 * version 090207 added copyright notice
 * version 080707 changed signature of searchUnitInView to avoid invalid recursion
 * version 080706 removed printExts
 * version 080702 adopted changed symInfo-debug-interface
 * version 080614 added indirMthdTableEntries, added searching of variables in implemented interfaces
 * version 080105 added plausibility check in isParent
 * version 080102 added method to print name of unit including outer units
 * version 071227 added support for inner units, moved searchCalledUnits to ExAbsCall
 * version 071215 added enterInheritableReferences to support semantically correct re-filling of class descriptors
 * version 070816 renamed parameter "name" in searchCalledMethod to clarify difference to Unit.name
 * version 070801 got offset* from Clss
 * version 070731 changed extsImplID[] to extsImplIDList
 * version 070727 got extsID* and implID from Clss and Intf, added symHint
 * version 070714 added printExts
 * version 070713 optimized alloceDescriptor
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070615 took over OutputObject values
 * version 070527 added final-flag and short-path to inline array variable
 * version 070522 added flag to support check of descriptor necessity
 * version 070511 added default method to create constant objects
 * version 070506 added support for runtime environment without objects
 * version 070504 added genDescriptor
 * version 070501 moved prepareOutput hereto
 * version 070303 added instIndirScalarTableSize;
 * version 061202 added stat*Table*
 * version 061030 changed indirectCalls to modifier
 * version 060723 added flag for explicit standard constructor
 * version 060607 initial version
 */

public abstract class Unit extends Token
{
	public final static String OUTERVARNAME = "$outer";
	public final static String STATICMTHDNAME = "$statinit";
	
	//levels of access with minimum visibility level of destination
	private final static String ERR_NOTVIS = " is not visible";
	
	//required fields for resolving
	public QualID pack, extsID; //containing package, extended direct unit
	public QualIDList extsImplIDList; //extended/implemented indirect units
	public String name;
	public int modifier, marker;
	public Object outputLocation;
	public boolean explConstr, explStdConstr; //needs or has explicit constructors, has expl std-constr
	public Vrbl vars, inlArr; //declared vars, special inline var
	public VrblList instInitVars; //declared and initialized vars that have to coded in constructor
	public VrblList writeCheckFinalVars; //declared final vars that have to be written in constructor
	public Mthd initStat, initDyna, mthds;
	public Unit nextUnit, outerUnit, innerUnits;
	public boolean offsetsAssigned, offsetError, outputGenerated, outputError;
	public ExConstInitObj constObjList;
	//required fields for interface-checks, overloading and code-generation
	public int statRelocTableEntries, statScalarTableSize;
	public int clssRelocTableEntries, clssScalarTableSize;
	public int instRelocTableEntries, instScalarTableSize, instIndirScalarTableSize;
	public int indirMthdTableEntries;
	public RelationElement myRelations;
	
	private QualID qidOfThis;
	
	public Unit(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	//this is what different languages have to implement differently
	public abstract UnitList getRefUnit(Unit refUnit, boolean insert);
	
	public abstract boolean resolveInterface(Context ctx);
	
	public abstract boolean hasValidInterface();
	
	public abstract boolean resolveMethodBlocks(Context ctx);
	
	public abstract Unit searchUnitInView(StringList name, boolean isRecursion);
	
	public abstract boolean assignOffsets(boolean doClssOff, Context ctx);
	
	public abstract boolean genDescriptor(Context ctx);
	
	public abstract IndirUnitMapList enterInheritableReferences(Object objLoc, IndirUnitMapList lastIntf, Context ctx);
	
	public abstract boolean genOutput(Context ctx);
	
	public abstract String getSourceType();
	
	public abstract void writeDebug(Context ctx, DebugWriter dbw);
	
	public boolean validateModifier(Context ctx)
	{
		return true; //default: modifier is already valid
	}
	
	public QualIDList getImportList()
	{
		return null; //default: no imports available
	}
	
	public QualID getQIDTo()
	{
		if (qidOfThis == null)
		{
			qidOfThis = new QualID(new StringList(name), QualID.Q_UNIT, fileID, line, col);
			qidOfThis.unitDest = this;
		}
		return qidOfThis;
	}
	
	public boolean isParent(Unit cmp, Context ctx)
	{
		Unit u;
		QualIDList list;
		
		//check validity
		if (cmp == null)
			return false;
		//check ourself
		if (cmp == this)
			return true;
		//check parents and their implementations
		u = cmp;
		while (u != null)
		{
			if (u == this)
				return true;
			list = u.extsImplIDList; //remember units implementations
			if (u.extsID != null)
			{
				if ((u = u.extsID.unitDest) == null)
				{ //destination is not valid ==> resolving error
					compErr(ctx, "unitDest not valid in Unit.isParent");
					ctx.err = true;
					return false;
				}
			}
			else
				u = null;
			//check implementations
			while (list != null)
			{
				if (isParent(list.qid.unitDest, ctx))
					return true;
				list = list.nextQualID;
			}
		}
		//not parent
		return false;
	}
	
	public boolean isOuter(Unit inner)
	{
		while (inner != null)
		{
			if (inner == this)
				return true;
			inner = inner.outerUnit;
		}
		return false;
	}
	
	public int minimumAccessLevel(Token token, String itemMsg, Unit destUnit, int destLevel, boolean allowExtensionAccess, Context ctx)
	{
		Unit myOutest, destOutest;
		if (ctx.doNotCheckVisibility)
			return Modifier.MA_PRIV; //do not check visibility in instance conversion block
		if (itemMsg == null)
			itemMsg = "item";
		//check trivial private
		if (this == destUnit || isOuter(destUnit) || (destUnit != null && destUnit.isOuter(this)))
			return Modifier.MA_PRIV;
		//check multiple inner classes where private is allowed, too
		if (outerUnit != null && destUnit != null && destUnit.outerUnit != null)
		{
			myOutest = outerUnit;
			while (myOutest.outerUnit != null)
				myOutest = myOutest.outerUnit;
			destOutest = destUnit.outerUnit;
			while (destOutest.outerUnit != null)
				destOutest = destOutest.outerUnit;
			if (myOutest == destOutest)
				return Modifier.MA_PRIV;
		}
		//check modifier as call is not inside class
		if (destUnit == null || destUnit.pack == null)
		{
			token.printPos(ctx, itemMsg);
			ctx.out.print(" is invalid");
			return Modifier.M_ERROR;
		}
		//if it has to be public
		if (pack.packDest != destUnit.pack.packDest && (!allowExtensionAccess || !destUnit.isParent(this, ctx)))
		{ //check modifier to be public
			if ((destLevel & Modifier.M_PUB) == 0)
			{
				token.printPos(ctx, itemMsg);
				ctx.out.print(ERR_NOTVIS);
				return Modifier.M_ERROR;
			}
			return Modifier.MA_PUB;
		}
		//if it has to be protected
		if (pack.packDest != destUnit.pack.packDest && destUnit.isParent(this, ctx))
		{ //check modifier to be protected
			if ((destLevel & (Modifier.M_PUB | Modifier.M_PROT)) == 0)
			{ //check modifier to be public or protected
				token.printPos(ctx, itemMsg);
				ctx.out.print(ERR_NOTVIS);
				return Modifier.M_ERROR;
			}
			return Modifier.MA_PROT;
		}
		//as it is not inside a class, lastly check if it has to be package private
		if ((destLevel & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP)) == 0)
		{ //check modifier to be public or protected or package private
			token.printPos(ctx, itemMsg);
			ctx.out.print(ERR_NOTVIS);
			return Modifier.M_ERROR;
		}
		return Modifier.MA_PACP;
	}
	
	public Vrbl searchVariable(String cmp, Context ctx)
	{
		Vrbl me = vars, res = null;
		QualIDList extsList;
		
		while (me != null)
		{
			if (me.name.equals(cmp))
				return me;
			me = me.nextVrbl;
		}
		if (extsID != null)
		{
			if (extsID.unitDest.hasValidInterface() && (me = extsID.unitDest.searchVariable(cmp, ctx)) != null)
				return me;
		}
		extsList = extsImplIDList;
		while (extsList != null)
		{
			if (extsList.qid.unitDest.hasValidInterface() && (me = extsList.qid.unitDest.searchVariable(cmp, ctx)) != null)
			{
				if (res == null)
					res = me;
				else if (res != me)
				{
					ctx.out.print("access to variable ");
					ctx.out.print(cmp);
					ctx.out.println(" is ambiguous");
				}
			}
			extsList = extsList.nextQualID;
		}
		return res;
	}
	
	public Mthd searchMethod(Mthd cmp, Context ctx)
	{
		Mthd me = mthds;
		
		while (me != null)
		{
			if (me.matches(cmp, ctx))
				return me;
			me = me.nextMthd;
		}
		if (extsID == null)
			return null;
		if (!extsID.unitDest.hasValidInterface())
			return null;
		return extsID.unitDest.searchMethod(cmp, ctx);
	}
	
	protected boolean allocateDescriptor(int scalarSize, int relocEntries, Object typeLoc, Context ctx)
	{
		//check if already prepared
		if (outputLocation != null)
			return true;
		//allocate space for class-descriptor
		if ((outputLocation = ctx.mem.allocate(scalarSize, 0, relocEntries, ctx.leanRTE ? null : typeLoc)) == null)
		{
			ctx.out.println("not enough memory");
			outputError = true;
			return false;
		}
		ctx.mem.allocationDebugHint(this);
		return true;
	}
	
	public boolean genConstObj(Context ctx, boolean secondRun)
	{
		ExConstInitObj obj = constObjList;
		while (obj != null)
		{
			if (obj.dependsOn == null || (obj.dependsOn.modifier & Modifier.M_NDCODE) != 0)
			{
				if (!obj.generateObject(ctx, secondRun))
				{
					outputError = true;
					return false;
				}
				if (ctx.dynaMem)
					ctx.arch.putRef(outputLocation, obj.dest.relOff, obj.outputLocation, 0);
			}
			else if (ctx.verbose)
			{
				obj.printPos(ctx, "skipping constant object in unit ");
				ctx.out.println(name);
			}
			obj = obj.nextConstInit;
		}
		//everything done
		return true;
	}
	
	public void printNameWithOuter(TextPrinter v)
	{
		printNameWithOuter(v, '.');
	}
	
	public void printNameWithOuter(TextPrinter v, char delim)
	{
		if (outerUnit != null)
		{
			outerUnit.printNameWithOuter(v);
			v.print(delim);
		}
		v.print(name);
	}
	
	public void writeVarsAndConstObjDebug(Context ctx, DebugWriter dbw)
	{
		Vrbl var;
		ExConstInitObj obj;
		
		dbw.startVariableList();
		var = vars;
		while (var != null)
		{
			dbw.hasVariable(var);
			var = var.nextVrbl;
		}
		dbw.endVariableList();
		dbw.startStatObjList();
		obj = constObjList;
		while (obj != null)
		{
			if (obj.dependsOn == null || (obj.dependsOn.modifier & Modifier.M_NDCODE) != 0)
			{
				dbw.hasStatObj(ctx.dynaMem ? obj.dest.relOff : -1, obj.outputLocation, obj.getDebugValue(), obj.inFlash);
			}
			else
				dbw.hasStatObj(-1, null, obj.getDebugValue(), false);
			obj = obj.nextConstInit;
		}
		dbw.endStatObjList();
	}
	
	//listAllInterfaceParents will list all interface parents in a newly created list and enumerates them
	public UnitList listAllInterfaceParents(UnitList listToExtend, Context ctx)
	{
		UnitList last, cmp;
		QualIDList qids = extsImplIDList;
		while (qids != null)
		{
			Unit u = qids.qid.unitDest;
			cmp = listToExtend;
			while (cmp != null)
			{ //check if unit is already in list
				if (cmp.unit == u)
					break;
				cmp = cmp.next;
			}
			if (cmp == null)
			{ //unit is not in list so far => add it with all parents
				last = new UnitList(u);
				if ((last.next = listToExtend) != null)
					last.relOff = listToExtend.relOff + 1;
				listToExtend = u.listAllInterfaceParents(last, ctx);
			}
			qids = qids.nextQualID;
		}
		return listToExtend;
	}
}
