/* Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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

/**
 * UnitDummy: placeholder for the class containing a static variable to access the raw information
 *
 * @author S. Frenz
 * @version 121020 removed "final abstract" modifier, added support for getSourceType
 * version 110624 adopted changed Context
 * version 100504 reduced compErr-messages
 * version 100409 adopted changed TypeRef
 * version 100114 reorganized constant object handling
 * version 091215 adopted changed Unit
 * version 091209 added support for constant objects
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080707 adopted changed signature of Unit.searchUnitInView
 * version 071222 adopted changes in Unit
 * version 071215 added enterInheritableReferences
 * version 070928 fixed class hierarchy
 * version 070903 adopted changes in AccVar.checkNameAgainst*
 * version 070801 initial version from symbols.SymbolUnit
 */

public class UnitDummy extends Unit
{
	public UnitDummy()
	{
		//initialize ourself
		super(-1, -1, -1);
		modifier = Modifier.M_PUB;
		//initialize initStat
		initStat = new MthdDummy(null, Modifier.M_PUB | Modifier.M_STAT, -1, -1, -1);
	}
	
	public UnitList getRefUnit(Unit refUnit, boolean insert)
	{
		//we won't reference anybody else
		return null;
	}
	
	public boolean resolveInterface(Context ctx)
	{
		Vrbl var;
		
		extsID = ctx.langRoot.getQIDTo();
		//check only vars as there is no method block
		var = vars;
		while (var != null)
		{
			if (!var.type.resolveType(this, ctx) || !var.checkNameAgainstVrbl(vars, ctx) || !var.enterSize(var.location == Vrbl.L_NOTRDY ? Vrbl.L_UNIT : var.location, ctx))
			{
				ctx.out.print(" in dummy-unit ");
				ctx.out.println(name);
				return false;
			}
			var = var.nextVrbl;
		}
		return true;
	}
	
	public boolean hasValidInterface()
	{
		//self-generated interface is valid
		return true;
	}
	
	public boolean resolveMethodBlocks(Context ctx)
	{
		Vrbl var = vars;
		
		//the basic UnitDummy only supports variables
		while (var != null)
		{
			if (!var.resolveConstant(this, ctx))
			{
				ctx.out.println(" in dummy unit");
				return false;
			}
			var = var.nextVrbl;
		}
		return true;
	}
	
	public Unit searchUnitInView(StringList name, boolean isRecursion)
	{
		//there are no imports and no inner units -> no view
		return null;
	}
	
	public boolean assignOffsets(boolean doClssOff, Context ctx)
	{
		Vrbl checkVrbl;
		ExConstInitObj checkConstObj;
		int offTmp;
		Unit parent;
		
		if (offsetError)
			return false;
		if (offsetsAssigned)
			return true;
		if (!doClssOff)
		{ //all other must enter everything at once
			ctx.out.println("invalid call to UnitDummy.assignOffsets");
			offsetError = true;
			return false;
		}
		//check parent
		if ((parent = extsID.unitDest) == null)
		{
			ctx.out.println("langRoot not set in UnitDummy.assignOffsets");
			offsetError = true;
			return false;
		}
		if (!parent.assignOffsets(true, ctx))
			return false;
		//copy values from parent
		clssScalarTableSize = parent.clssScalarTableSize;
		clssRelocTableEntries = parent.clssRelocTableEntries;
		if (ctx.leanRTE && this == ctx.rteSMthdBlock)
		{ //reduce size of method blocks
			instScalarTableSize = instRelocTableEntries = instIndirScalarTableSize = 0;
		}
		else
		{ //normal object
			instScalarTableSize = parent.instScalarTableSize;
			instRelocTableEntries = parent.instRelocTableEntries;
			instIndirScalarTableSize = parent.instIndirScalarTableSize;
		}
		checkVrbl = vars;
		while (checkVrbl != null)
		{
			switch (checkVrbl.location)
			{
				case Vrbl.L_CLSSSCL:
					modifier |= Modifier.MA_ACCSSD;
					if (ctx.embedded)
					{
						checkVrbl.relOff = ctx.ramSize;
						ctx.ramSize += checkVrbl.minSize; //no alignment
					}
					else
					{
						//align
						offTmp = checkVrbl.minSize - 1;
						statScalarTableSize = (statScalarTableSize + offTmp) & ~offTmp;
						//get space
						checkVrbl.relOff = statScalarTableSize;
						statScalarTableSize += checkVrbl.minSize;
					}
					break;
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
				case Vrbl.L_STRUCT:
				default:
					ctx.out.print("### internal error: unknown location of variable in dummy-unit ");
					ctx.out.println(name);
					offsetError = true;
					return false;
			}
			checkVrbl = checkVrbl.nextVrbl;
		}
		if (ctx.dynaMem)
		{
			checkConstObj = constObjList;
			if (checkConstObj != null)
			{
				modifier |= Modifier.MA_ACCSSD;
				//enter information
				while (checkConstObj != null)
				{
					checkConstObj.dest.relOff = -(++statRelocTableEntries + clssRelocTableEntries) * ctx.arch.relocBytes;
					checkConstObj.dest.location = Vrbl.L_CLSSREL;
					checkConstObj = checkConstObj.nextConstInit;
				}
			}
		}
		return offsetsAssigned = true;
	}
	
	public boolean genDescriptor(Context ctx)
	{
		Vrbl checkVrbl;
		
		//allocate space for class-descriptor
		if (!allocateDescriptor(clssScalarTableSize + statScalarTableSize, clssRelocTableEntries + statRelocTableEntries, ctx.rteSClassDesc.outputLocation, ctx))
		{
			outputError = true;
			return false;
		}
		//clss* will not be changed anymore
		if (!ctx.embedded)
		{ //fix relative positions
			checkVrbl = vars;
			while (checkVrbl != null)
			{ //will break on first var of parent
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
		return true;
	}
	
	public IndirUnitMapList enterInheritableReferences(Object objLoc, IndirUnitMapList lastIntf, Context ctx)
	{
		//nothing to do and no list to indirect maps, but respect parent
		return extsID != null ? extsID.unitDest.enterInheritableReferences(objLoc, lastIntf, ctx) : null;
	}
	
	public boolean genOutput(Context ctx)
	{
		//nothing to do
		return outputGenerated = true;
	}
	
	public String getSourceType()
	{
		return "dummy";
	}
	
	public void writeDebug(Context ctx, DebugWriter dbw)
	{
		dbw.startUnit("dummy unit", this);
		dbw.hasUnitOutputLocation(outputLocation);
		writeVarsAndConstObjDebug(ctx, dbw);
		dbw.endUnit();
	}
}
