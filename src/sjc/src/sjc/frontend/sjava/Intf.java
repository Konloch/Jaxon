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
import sjc.debug.DebugWriter;

/**
 * Intf: interface-dependent part of java-units
 *
 * @author S. Frenz
 * @version 120923 moved position of writing variables to debug writer to be done before methods
 * version 120228 cleaned up "import sjc." typo
 * version 110122 fixed alignment if needsAlignedVrbls is set
 * version 101222 added check for newly inserted needsAlignedVrbls
 * version 100504 reduced compErr-messages
 * version 100428 added support for parents-array
 * version 100114 reorganized constant object handling
 * version 091215 adopted changed Unit
 * version 091022 adopted changes in RelationManager
 * version 091021 adopted changed modifier declarations
 * version 091020 added relation tracking
 * version 091001 adopted changed memory interface
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090207 added copyright notice
 * version 081227 better error output for variable names already in use
 * version 080702 adopted changed symInfo-debug-interface
 * version 080622 better error output for invalidly initialized static variables, fixed static new for dynaMem-mode
 * version 080616 made use of JUnit.fixDynaAddresses
 * version 080614 added support of constant variables
 * version 080121 fixed ownership of inherited methods and variables
 * version 071215 added empty enterInheritableReferences
 * version 071214 beautyfied error output
 * version 070801 removed methods no longer needed after Unit-redesign
 * version 070731 adopted change from QualID[] to QualIDList
 * version 070730 removed all parent-references to Intf and Clss
 * version 070727 adopted change of Mthd.id from PureID to String, moved extsID to Unit
 * version 070714 checking genAllUnitDesc
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070522 skipping of descriptor allocation if never accessed
 * version 070511 added genConstObj
 * version 070504 added genDescriptor
 * version 070127 added checkInstVarInit
 * version 070114 reduced access level where possible
 * version 070113 added list of included interfaces in runtime descriptor
 * version 070111 adapted change in printPos and compErr
 * version 061211 removed isImplemented and insertStrings as it is never used
 * version 061203 optimized calls to printPos and compErr
 * version 061030 changed detection of indirectCall
 * version 060607 initial version
 */

public class Intf extends JUnit
{
	protected Intf(QualID ip, QualIDList ii, int im, int fid, int il, int ic)
	{
		super(fid, il, ic);
		pack = ip;
		impt = ii;
		modifier = im | Modifier.M_INDIR;
		initStat = new JMthd(null, Modifier.M_STAT, fid, il, ic);
		//note: there is no dynamic initialization, but method calls are dynamic (needed for resolve)
		initDyna = new JMthd(null, 0, fid, il, ic);
	}
	
	protected boolean resolveIntfExtsIpls(Context ctx)
	{
		QualIDList list, cmp;
		
		list = extsImplIDList;
		while (list != null)
		{
			if (!resolveExtsIplsQID(list.qid, ctx))
				return false;
			if (!list.qid.unitDest.resolveInterface(ctx))
				return false;
			if ((list.qid.unitDest.modifier & Modifier.M_INDIR) == 0)
			{
				list.qid.printPos(ctx, "class can not be extended by interface ");
				ctx.out.println(name);
				return false;
			}
			cmp = extsImplIDList;
			while (cmp != list)
			{
				if (cmp.qid.unitDest == list.qid.unitDest)
				{
					cmp.qid.printPos(ctx, "interface listet twice in ");
					ctx.out.println(name);
					return false;
				}
				cmp = cmp.nextQualID;
			}
			if (ctx.relations != null)
				ctx.relations.addRelation(list.qid.unitDest, this);
			list = list.nextQualID;
		}
		return true;
	}
	
	protected boolean checkInstVarInit(Vrbl var, Context ctx)
	{
		var.printPos(ctx, ": instance varibles not supported in interfaces");
		return false;
	}
	
	protected boolean resolveMthdExtsIpls(Context ctx)
	{
		return true; //no method blocks to be resolved
	}
	
	public boolean assignOffsets(boolean doClssOff, Context ctx)
	{
		Vrbl checkVrbl;
		
		if (offsetsAssigned)
			return true; //not called for struct, already set in checkStructSpecials
		if (offsetError)
			return false;
		if (!doClssOff)
		{
			ctx.out.println("invalid call to Intf.assignOffsets");
			offsetError = true;
			return false;
		}
		if (!ctx.rteSIntfDesc.assignOffsets(true, ctx))
			return false;
		//initialize with SIntfDesc
		clssScalarTableSize = ctx.rteSIntfDesc.clssScalarTableSize;
		clssRelocTableEntries = ctx.rteSIntfDesc.clssRelocTableEntries;
		instScalarTableSize = ctx.rteSIntfDesc.instScalarTableSize;
		instRelocTableEntries = ctx.rteSIntfDesc.instRelocTableEntries;
		instIndirScalarTableSize = ctx.rteSIntfDesc.instIndirScalarTableSize;
		//enter variables
		checkVrbl = vars;
		while (checkVrbl != null)
		{
			switch (checkVrbl.location)
			{
				case Vrbl.L_CLSSREL:
					modifier |= Modifier.MA_ACCSSD;
					if (ctx.embedded)
					{
						checkVrbl.relOff = ctx.ramSize;
						ctx.ramSize -= checkVrbl.minSize * ctx.arch.relocBytes; //no alignment
					}
					else
						checkVrbl.relOff = -(statRelocTableEntries -= checkVrbl.minSize) * ctx.arch.relocBytes;
					break;
				case Vrbl.L_CONSTDC: //constants do not need space
				case Vrbl.L_CONST:
					break;
				default:
					ctx.out.println("### internal error: unknown location of variable");
					offsetError = true;
					return false;
			}
			checkVrbl = checkVrbl.nextVrbl;
		}
		//if there were statics, align class-scalars to stack-alignment
		if (statScalarTableSize != 0)
			clssScalarTableSize = (clssScalarTableSize + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits;
		if (ctx.dynaMem)
			fixDynaAddresses(ctx);
		//everything done
		return offsetsAssigned = true;
	}
	
	public boolean genDescriptor(Context ctx)
	{
		Vrbl checkVrbl;
		
		if (ctx.genAllUnitDesc || ctx.genIntfParents)
			modifier |= Modifier.MA_ACCSSD; //parents-listing requires all SIntfDesc to be created
		if (!ctx.dynaMem && (modifier & Modifier.MA_ACCSSD) == 0)
		{
			if (ctx.verbose)
			{
				printPos(ctx, "skipping descriptor for interface ");
				if (pack != null)
				{
					pack.printFullQID(ctx.out);
					ctx.out.print('.');
				}
				ctx.out.println(name);
			}
		}
		else if (!allocateDescriptor(clssScalarTableSize + statScalarTableSize, clssRelocTableEntries + statRelocTableEntries, ctx.rteSIntfDesc.outputLocation, ctx))
		{
			outputError = true;
			return false;
		}
		//outputAddr is now valid and will not be changed anymore
		else if (!ctx.embedded)
		{ //fix dynamic varPos
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				switch (checkVrbl.location)
				{
					case Vrbl.L_CLSSSCL:
						checkVrbl.relOff += clssScalarTableSize;
						break;
					case Vrbl.L_CLSSREL:
						checkVrbl.relOff -= clssRelocTableEntries * ctx.arch.relocBytes;
						break;
				}
				checkVrbl = checkVrbl.nextVrbl;
			}
		}
		//else: nothing to fix, varPos contains valid address
		return true;
	}
	
	protected boolean checkDeclarations(Context ctx)
	{
		Mthd meM, cmpM, lastM;
		Vrbl meV, cmpV;
		boolean sigOK = true;
		QualIDList list;
		
		//check declared methods
		meM = mthds;
		lastM = null;
		while (meM != null)
		{
			meM.relOff = ++indirMthdTableEntries;
			cmpM = mthds;
			while (cmpM != meM)
			{
				if (meM.matches(cmpM, ctx))
				{ //name and signature identical
					if (meM.retType.compareType(cmpM.retType, false, ctx) != TypeRef.C_EQ)
					{
						meM.printPos(ctx, "method ");
						ctx.out.print(meM.name);
						ctx.out.print(" declared twice in interface ");
						ctx.out.println(name);
						sigOK = false;
					}
				}
				cmpM = cmpM.nextMthd;
			}
			lastM = meM;
			meM = meM.nextMthd;
		}
		//copy methods of other interfaces
		list = extsImplIDList;
		while (list != null)
		{
			meM = list.qid.unitDest.mthds;
			while (meM != null)
			{
				cmpM = searchMethod(meM, ctx);
				if (cmpM != null)
				{ //meMthod with same name and signature exists
					if (meM.retType.compareType(cmpM.retType, false, ctx) != TypeRef.C_EQ //not same return-type
							|| ((cmpM.modifier | meM.modifier) & Modifier.M_PUB) != (meM.modifier & Modifier.M_PUB))
					{ //reduces visibility
						meM.printPos(ctx, "method ");
						ctx.out.print(meM.name);
						ctx.out.print(" has not same return-type or reduces visibility in interface ");
						ctx.out.println(name);
						sigOK = false;
					}
					//else: ignore meMethod
				}
				else
				{ //no matching meMethod found -> copy
					if (lastM == null)
					{ //no meMethod until now
						(mthds = meM.copy()).owner = this;
						lastM = mthds;
					}
					else
					{ //append method
						(lastM.nextMthd = meM.copy()).owner = this;
						lastM = lastM.nextMthd;
					}
					lastM.relOff = ++indirMthdTableEntries;
				}
				meM = meM.nextMthd;
			}
			list = list.nextQualID;
		}
		//check declared variables
		meV = vars;
		while (meV != null)
		{
			if ((meV.modifier & (Modifier.M_STAT | Modifier.M_FIN)) != (Modifier.M_STAT | Modifier.M_STAT))
			{
				cmpV = vars;
				while (cmpV != meV)
				{
					if (meV.name.equals(cmpV.name))
					{ //name
						varDeclError(meV, "declared twice", ctx);
						sigOK = false;
					}
					cmpV = cmpV.nextVrbl;
				}
				if (meV.init == null)
				{
					varDeclError(meV, "needs initialization", ctx);
					sigOK = false;
				}
				list = extsImplIDList;
				while (list != null)
				{
					if (list.qid.unitDest.searchVariable(meV.name, ctx) != null)
					{
						meV.printPos(ctx, "warning: name of variable ");
						ctx.out.print(meV.name);
						ctx.out.print(" already in use in parent of ");
						ctx.out.println(name);
					}
					list = list.nextQualID;
				}
			}
			else
			{ //should never happen, parser should have not accepted this variable
				compErr(ctx, "interface variable is not final static");
				sigOK = false;
			}
			meV = meV.nextVrbl;
		}
		//return true if everything was OK
		return sigOK;
	}
	
	public UnitList getRefUnit(Unit refUnit, boolean insert)
	{
		return null; //interfaces never import any classes
	}
	
	public IndirUnitMapList enterInheritableReferences(Object objLoc, IndirUnitMapList lastIntf, Context ctx)
	{
		//nothing to do or return
		return null;
	}
	
	public boolean genOutput(Context ctx)
	{
		Vrbl var;
		Object tmp, addr;
		int tmpOff;
		UnitList parentsList;
		
		//avoid multiple genOutputs
		if (outputGenerated)
			return true;
		if (outputError)
			return false;
		//initialize optional parents-Array
		if (ctx.genIntfParents && extsImplIDList != null)
		{
			parentsList = listAllInterfaceParents(null, ctx); //gets all parents (enumerated max..0)
			tmp = ctx.mem.allocateArray(parentsList.relOff + 1, 1, -1, 0, ctx.rteSArray.outputLocation);
			ctx.arch.putRef(outputLocation, ctx.rteSIntfParents, tmp, 0);
			tmpOff = -(ctx.rteSArray.instRelocTableEntries + 1) * ctx.arch.relocBytes;
			while (parentsList != null)
			{ //insert all parents in the array
				ctx.arch.putRef(tmp, tmpOff - parentsList.relOff * ctx.arch.relocBytes, parentsList.unit.outputLocation, 0);
				parentsList = parentsList.next;
			}
		}
		//initialize object variables
		if (ctx.dynaMem)
		{
			var = vars;
			while (var != null)
			{
				switch (var.location)
				{
					case Vrbl.L_CLSSREL:
						if (!var.init.isCompInitConstObject(ctx))
						{
							var.init.printPos(ctx, "value of static reference variable is not constant");
							ctx.out.println();
							outputError = true;
							return false;
						}
						if ((tmp = var.init.getConstInitObj(ctx).outputLocation) == null)
						{
							var.init.compErr(ctx, "static initialization with invalid destination");
							outputError = true;
							return false;
						}
						if (ctx.embedded)
						{
							addr = ctx.ramInitLoc;
							tmpOff = ctx.embConstRAM ? ctx.ramOffset : 0;
						}
						else
						{
							addr = outputLocation;
							tmpOff = 0;
						}
						ctx.arch.putRef(addr, var.relOff, tmp, tmpOff);
						break;
					case Vrbl.L_CONST:
					case Vrbl.L_CONSTDC:
						//variable is constant (used or only declared), do not assign anything
						break;
					default:
						ctx.out.print("invalid location during assign of static variable ");
						ctx.out.println(var.name);
						outputError = true;
						return false;
				}
				var = var.nextVrbl;
			}
		}
		return outputGenerated = true;
	}
	
	public void writeDebug(Context ctx, DebugWriter dbw)
	{
		Mthd mthd;
		
		dbw.startUnit("interface", this);
		if ((modifier & Modifier.MA_ACCSSD) == 0)
			dbw.markUnitAsNotUsed();
		else
		{
			dbw.hasUnitOutputLocation(outputLocation);
			dbw.hasUnitFields(clssRelocTableEntries, clssScalarTableSize, statRelocTableEntries, statScalarTableSize, instRelocTableEntries, instScalarTableSize, instIndirScalarTableSize);
			writeVarsAndConstObjDebug(ctx, dbw);
			dbw.startMethodList();
			mthd = mthds;
			while (mthd != null)
			{
				dbw.hasMethod(mthd, true);
				mthd = mthd.nextMthd;
			}
			dbw.endMethodList();
		}
		dbw.endUnit();
	}
	
	private void varDeclError(Vrbl meV, String msg, Context ctx)
	{
		meV.printPos(ctx, "variable ");
		ctx.out.print(meV.name);
		ctx.out.print(' ');
		ctx.out.print(msg);
		ctx.out.print(" in interface ");
		ctx.out.println(name);
	}
}
