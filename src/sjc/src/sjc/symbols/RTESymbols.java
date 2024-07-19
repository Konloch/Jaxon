/* Copyright (C) 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.symbols;

import sjc.compbase.*;
import sjc.osio.TextBuffer;
import sjc.osio.TextPrinter;

/**
 * RawSymbols: generate output for all packages, append information at units and methods
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 101231 added noPackOuter, noClassMod and noClassPack options
 * version 101227 removed package list as it is not enough comprehensible
 * version 101226 added support for package list, adopted changed SymbolInformer
 * version 100507 cleaned up
 * version 091125 added owner in methods
 * version 091105 added support for lineInCodeOffset for methods and option noSLHI
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090207 added copyright notice
 * version 080701 adopted move of TextBuffer to osio
 * version 080614 adopted changed Unit.searchVariable
 * version 080115 adopted changes in SymbolInformer.generateSymbols, using SymbolInformer.checkVrbl
 * version 080114 removed checkParameter
 * version 070729 removed no longer needed prepareUnits, added type check for root-variable
 * version 070727 added flag for special (String,int)-method-detection
 * version 070714 initial version
 */

public class RTESymbols extends SymbolInformer
{
	private Unit packUnit;
	private TextBuffer myBuffer;
	private int packNamePos, packOuterPos, packSubPacksPos, packNextPackPos, packUnitsPos;
	private int unitNamePos, unitModifierPos, unitNextUnitPos, unitPackPos, unitMthdsPos;
	private int mthdNameParPos, mthdModifierPos, mthdOwnerPos, mthdNextMthdPos, mthdLineInCodeOffsetPos;
	private int addedUnitSize, addedMthdSize;
	private boolean noSLHI, noClassMod, noClassPack, noPackOuter;
	
	private final static String NOSLHI = "skipping source line information in RTESymbols (";
	
	public static void printValidParameters(TextPrinter v)
	{
		v.println("   noSLHI            exclude source line hint integration information");
		v.println("   noClassMod        do not store modifier of class in class descriptor");
		v.println("   noClassPack       do not link class descriptors to their package");
		v.println("   noPackOuter       do not link packages to their outer one");
	}
	
	public String getName()
	{
		return "RTESymbols";
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if (parm.equals("noSLHI"))
		{
			noSLHI = true;
			return true;
		}
		if (parm.equals("noClassMod"))
		{
			noClassMod = true;
			return true;
		}
		if (parm.equals("noClassPack"))
		{
			noClassPack = true;
			return true;
		}
		if (parm.equals("noPackOuter"))
		{
			noPackOuter = true;
			return true;
		}
		v.println("RTESymbols accepts only the following parameters:");
		printValidParameters(v);
		return false;
	}
	
	public boolean generateSymbols(UnitList newUnits, Context ictx)
	{
		Object rootObj;
		Vrbl rootVar;
		
		ctx = ictx;
		myBuffer = new TextBuffer();
		//allocate root-package-object
		if ((packUnit = ctx.rte.searchUnit("SPackage")) == null || (packUnit.modifier & (Modifier.M_ABSTR | Modifier.M_INDIR | Modifier.M_STRUCT)) != 0)
		{
			ctx.out.println("unit rte.Package required for RTESymbols (non-abstract, non-indir, non-struct)");
			return false;
		}
		if ((rootVar = packUnit.searchVariable("root", ctx)) == null || (rootVar.modifier & (Modifier.M_STAT | Modifier.M_STRUCT)) != Modifier.M_STAT || rootVar.type.baseType != TypeRef.T_QID || rootVar.type.qid.unitDest != packUnit || rootVar.type.arrDim != 0)
		{
			ctx.out.println("variable static SPackage root needed in rte.SPackage");
			return false;
		}
		if ((packNamePos = checkVrbl(packUnit, "name")) == AccVar.INV_RELOFF || (!noPackOuter && (packOuterPos = checkVrbl(packUnit, "outer")) == AccVar.INV_RELOFF) || (packSubPacksPos = checkVrbl(packUnit, "subPacks")) == AccVar.INV_RELOFF || (packNextPackPos = checkVrbl(packUnit, "nextPack")) == AccVar.INV_RELOFF || (packUnitsPos = checkVrbl(packUnit, "units")) == AccVar.INV_RELOFF || (unitNamePos = checkVrbl(ctx.rteSClassDesc, "name")) == AccVar.INV_RELOFF || (!noClassMod && (unitModifierPos = checkVrbl(ctx.rteSClassDesc, "modifier")) == AccVar.INV_RELOFF) || (unitNextUnitPos = checkVrbl(ctx.rteSClassDesc, "nextUnit")) == AccVar.INV_RELOFF || (!noClassPack && (unitPackPos = checkVrbl(ctx.rteSClassDesc, "pack")) == AccVar.INV_RELOFF) || (unitMthdsPos = checkVrbl(ctx.rteSClassDesc, "mthds")) == AccVar.INV_RELOFF || (mthdNameParPos = checkVrbl(ctx.rteSMthdBlock, "namePar")) == AccVar.INV_RELOFF || (mthdOwnerPos = checkVrbl(ctx.rteSMthdBlock, "owner")) == AccVar.INV_RELOFF || (mthdNextMthdPos = checkVrbl(ctx.rteSMthdBlock, "nextMthd")) == AccVar.INV_RELOFF || (mthdModifierPos = checkVrbl(ctx.rteSMthdBlock, "modifier")) == AccVar.INV_RELOFF)
		{
			return false;
		}
		if (noSLHI)
		{
			ctx.out.print(NOSLHI);
			ctx.out.println("explicit off-option given)");
			mthdLineInCodeOffsetPos = AccVar.INV_RELOFF;
		}
		else if ((mthdLineInCodeOffsetPos = checkVrbl(ctx.rteSMthdBlock, "lineInCodeOffset")) == AccVar.INV_RELOFF)
		{
			ctx.out.print(NOSLHI);
			ctx.out.println("missing variable)");
		}
		//calculate size of fields required for symbol information
		addedUnitSize = (4 * ctx.arch.relocBytes + 4 + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits;
		addedMthdSize = (2 * ctx.arch.relocBytes + 4 + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits;
		//generate tree and enter root-object in static root-field
		if ((rootObj = genPackage(ctx.root, null)) == null)
			return false;
		ctx.arch.putRef(packUnit.outputLocation, rootVar.relOff, rootObj, 0);
		//everything done
		return !ctx.err;
	}
	
	private Object genPackage(Pack pack, Object outerObj)
	{
		Object packObj, nameObj, firstPackObj = null, lastPackObj = null;
		
		while (pack != null)
		{
			if ((packObj = ctx.mem.allocate(packUnit.instScalarTableSize, packUnit.instIndirScalarTableSize, packUnit.instRelocTableEntries, packUnit.outputLocation)) == null)
			{
				ctx.out.println("error allocating package");
				ctx.err = true;
				return null;
			}
			if (pack.name != null)
			{
				if ((nameObj = ctx.allocateString(pack.name)) == null)
					return null;
				ctx.arch.putRef(packObj, packNamePos, nameObj, 0);
			}
			if (!noPackOuter)
				ctx.arch.putRef(packObj, packOuterPos, outerObj, 0);
			if (firstPackObj == null)
				lastPackObj = firstPackObj = packObj; //first run
			else
				ctx.arch.putRef(lastPackObj, packNextPackPos, lastPackObj = packObj, 0); //all other runs
			ctx.arch.putRef(packObj, packUnitsPos, genUnit(pack.units, packObj), 0);
			ctx.arch.putRef(packObj, packSubPacksPos, genPackage(pack.subPacks, packObj), 0);
			pack = pack.nextPack;
		}
		return firstPackObj;
	}
	
	private Object genUnit(Unit unit, Object packObj)
	{
		Object firstUnitObj = null, lastUnitObj = null;
		
		while (unit != null)
		{
			if (unit.outputLocation != null && (unit.modifier & Modifier.M_INDIR) == 0 && (ctx.dynaMem || (unit.modifier & Modifier.MA_ACCSSD) != 0))
			{ //skip dummy-units of compiler and not-generated descriptors
				ctx.symGenSize += addedUnitSize;
				if (firstUnitObj == null)
					lastUnitObj = firstUnitObj = unit.outputLocation; //first run
				else
					ctx.arch.putRef(lastUnitObj, unitNextUnitPos, lastUnitObj = unit.outputLocation, 0); //all other runs
				ctx.arch.putRef(unit.outputLocation, unitNamePos, ctx.allocateString(unit.name), 0);
				if (!noClassPack)
					ctx.arch.putRef(unit.outputLocation, unitPackPos, packObj, 0);
				ctx.arch.putRef(unit.outputLocation, unitMthdsPos, genMthds(unit.mthds), 0);
				if (!noClassMod)
					ctx.mem.putInt(unit.outputLocation, unitModifierPos, unit.modifier);
			}
			unit = unit.nextUnit;
		}
		return firstUnitObj;
	}
	
	private Object genMthds(Mthd mthd)
	{
		Object firstMthdObj = null, lastMthdObj = null;
		int command;
		
		while (mthd != null)
		{
			if (mthd.outputLocation != null)
			{ //skip not generated methods
				ctx.symGenSize += addedMthdSize;
				if (firstMthdObj == null)
					lastMthdObj = firstMthdObj = mthd.outputLocation; //first run
				else
					ctx.arch.putRef(lastMthdObj, mthdNextMthdPos, lastMthdObj = mthd.outputLocation, 0); //all other runs
				myBuffer.used = 0;
				mthd.printNamePar(myBuffer);
				ctx.arch.putRef(mthd.outputLocation, mthdNameParPos, ctx.allocateString(new String(myBuffer.data, 0, myBuffer.used)), 0);
				ctx.arch.putRef(mthd.outputLocation, mthdOwnerPos, mthd.owner.outputLocation, 0);
				if (mthd.param != null && mthd.param.type.compareType(ctx.stringType, true, ctx) != TypeRef.C_NP && mthd.param.nextParam != null && mthd.param.nextParam.type.isIntType() && mthd.param.nextParam.nextParam == null)
					command = 0x80000000;
				else
					command = 0;
				ctx.mem.putInt(mthd.outputLocation, mthdModifierPos, mthd.modifier | command);
				if (mthdLineInCodeOffsetPos != AccVar.INV_RELOFF)
				{
					ctx.arch.putRef(mthd.outputLocation, mthdLineInCodeOffsetPos, ctx.allocateIntArray(mthd.lineInCodeOffset), 0);
				}
			}
			mthd = mthd.nextMthd;
		}
		return firstMthdObj;
	}
}
