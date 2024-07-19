/* Copyright (C) 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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

package sjc.debug;

import sjc.compbase.*;
import sjc.osio.BinWriter;
import sjc.osio.TextPrinter;

/**
 * SymInfo: default symbol information writer
 *
 * @author S. Frenz
 * @version 120504 added support for structref variables
 * version 120227 cleaned up "package sjc." typo
 * version 110624 adopted changed Context
 * version 101231 fixed access level reducing hint
 * version 100428 removed structAsReference case
 * version 100331 adopted changed DebugWriter
 * version 100115 adopted codeStart-movement
 * version 100114 reorganized constant object handling
 * version 091216 string merge
 * version 091215 added const init obj location hint
 * version 091103 added method code sum per unit
 * version 091027 added unit access level reducing hint
 * version 091026 fixed access level reducing hint
 * version 091021 adopted changed modifier declarations
 * version 091005 removed unneded methods
 * version 091001 adopted changed memory interface
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090626 added info for globalStackExtreme
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080712 changed constructor
 * version 080708 fixed linefeed after markUnitAsNotUsed
 * version 080706 full support for all kinds of information
 * version 080701 initial version
 */

public class SymInfo extends DebugWriter
{
	private final static String SKIPCAND = " (skip candidate)";
	
	private final Context ctx;
	private final TextPrinter out;
	private final BinWriter binOut;
	private int unitMthdCode;
	
	public SymInfo(String filename, Context ic)
	{
		out = (ctx = ic).osio.getNewFilePrinter(filename);
		binOut = ic.osio.getNewBinWriter();
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
		if (!isDecompressor)
			out.println("# --- SJC syminfo--- #");
		else
		{
			out.println();
			out.println("# --- decompressor --- #");
			out.println();
		}
		if (ctx.verbose)
			out.print('v');
		if (ctx.leanRTE)
			out.print('L');
		if (ctx.mem.streamObjects)
			out.print('l');
		if (ctx.indirScalars)
			out.print('M');
		if (ctx.dynaMem)
			out.print('m');
		if (ctx.assignHeapCall)
			out.print('h');
		if (ctx.assignCall)
			out.print('c');
		if (!ctx.doArrayStoreCheck)
			out.print('C');
		if (!ctx.doBoundCheck)
			out.print('B');
		if (ctx.runtimeBound)
			out.print('b');
		if (ctx.runtimeNull)
			out.print('n');
		if (ctx.debugCode)
			out.print('d');
		if (ctx.byteString)
			out.print('y');
		if (ctx.noThrowFrames)
			out.print('Y');
		if (ctx.embedded)
			out.print('e');
		if (ctx.embConstRAM)
			out.print('E');
		if (ctx.globalProfiling)
			out.print('F');
		if (ctx.globalStackExtreme)
			out.print('x');
		if (ctx.alternateObjNew)
			out.print('w');
		if (ctx.noInlineMthdObj)
			out.print('k');
		if (ctx.alignBlockMask != 0)
			out.print('K');
		out.println();
	}
	
	public void finalizeImageInfo()
	{
		out.close();
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
		out.print("BaseAddress: 0x");
		out.printHexFix(baseAddress, 8);
		out.println();
		out.print("Image size: ");
		out.print(memBlockLen);
		out.print(" b = ");
		out.print((memBlockLen + 1023) >>> 10);
		out.println(" kb");
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
		out.print("Code size: ");
		out.print(mthdCodeSize);
		out.print(" b = ");
		out.print((mthdCodeSize + 1023) >>> 10);
		out.print(" kb in ");
		out.print(mthdCount);
		out.println(" methods");
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
		out.print("Strings: ");
		out.print(stringCount);
		out.print(" with ");
		out.print(stringChars);
		out.print(" characters using ");
		out.print(stringMemBytes);
		out.print(" b = ");
		out.print((stringMemBytes + 1023) >>> 10);
		out.println(" kb");
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
		out.print("ramInitAddr: 0x");
		out.printHexFix(ctx.mem.getAddrAsInt(ramInitLoc, 0), 8);
		out.print(", ramInitSize: ");
		out.print(ramSize);
		out.print(" b, constMemorySize: ");
		out.print(constMemorySize);
		out.print(" b = ");
		out.print((constMemorySize + 1023) >>> 10);
		out.println(" kb");
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
		out.print("In-image symbol size: ");
		out.print(symGenSize);
		out.print(" b = ");
		out.print((symGenSize + 1023) >>> 10);
		out.println(" kb");
		out.println();
	}
	
	public void startUnit(String type, Unit unit)
	{
		out.println("----------");
		out.print(type);
		out.print(' ');
		if (unit.pack != null)
		{
			unit.pack.printFullQID(out);
			out.print('.');
		}
		unit.printNameWithOuter(out);
		if (unit.extsID != null && unit.extsID.unitDest != ctx.langRoot)
		{
			out.print(" extends ");
			unit.extsID.unitDest.pack.printFullQID(out);
			out.print('.');
			unit.extsID.unitDest.printNameWithOuter(out);
		}
		if ((unit.modifier & (Modifier.MA_PUB | Modifier.MA_PROT | Modifier.MA_PACP | Modifier.MA_PRIV)) != 0)
			printMinimumAccessLevel(unit.modifier);
		out.print(' ');
	}
	
	public void markUnitAsNotUsed()
	{
		out.println("never used");
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
		if (outputLocation != null)
		{
			out.print("at ");
			out.printHexFix(ctx.mem.getAddrAsInt(outputLocation, 0), 8);
			out.println();
		}
		else
			out.println();
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
		out.print("clssRelocTableEntries: ");
		out.print(clssRelocTableEntries);
		out.print(", clssScalarTableSize: ");
		out.println(clssScalarTableSize);
		out.print("statRelocTableEntries: ");
		out.print(statRelocTableEntries);
		out.print(", statScalarTableSize: ");
		out.println(statScalarTableSize);
		out.print("instRelocTableEntries: ");
		out.print(instRelocTableEntries);
		out.print(", instScalarTableSize: ");
		out.print(instScalarTableSize);
		out.print(", instIndirScalarTableSize: ");
		out.println(instIndirScalarTableSize);
	}
	
	public void startVariableList()
	{
		out.println("- added vars:");
	}
	
	public void hasVariable(Vrbl var)
	{
		switch (var.location)
		{
			case AccVar.L_CONST: //resolved constant
				out.print("          cnst-c/0");
				break;
			case AccVar.L_CLSSSCL: //scalar inside class
				out.printHexFix(ctx.dynaMem ? var.relOff : ctx.mem.getAddrAsInt(!ctx.embedded ? var.owner.outputLocation : ctx.ramLoc, var.relOff), 8);
				out.print("->clss-s/");
				out.print(var.minSize);
				break;
			case AccVar.L_CLSSREL: //reloc inside class
				out.printHexFix(ctx.dynaMem ? var.relOff : ctx.mem.getAddrAsInt(!ctx.embedded ? var.owner.outputLocation : ctx.ramLoc, var.relOff), 8);
				out.print("->clss-r/");
				out.print(-var.minSize);
				break;
			case AccVar.L_INSTSCL: //scalar inside instance
				out.printHexFix(var.relOff, 8);
				out.print("->inst-s/");
				out.print(var.minSize);
				break;
			case AccVar.L_INSTIDS: //indirect accessed scalar inside instance
				out.printHexFix(var.relOff, 8);
				out.print("->indr-s/");
				out.printHexFix(var.relOff, 8);
				break;
			case AccVar.L_INSTREL: //reloc inside instance
				out.printHexFix(var.relOff, 8);
				out.print("->inst-r/");
				out.print(-var.minSize);
				break;
			case AccVar.L_STRUCT: //struct variable
				out.printHexFix(var.relOff, 8);
				out.print("->struct/");
				out.print(var.minSize);
				break;
			case AccVar.L_STRUCTREF: //struct reference with reference to other struct
				out.printHexFix(var.relOff, 8);
				out.print("->strucR/");
				out.print(var.minSize);
				break;
			case AccVar.L_INLARR:
				out.print("      inline array");
				break;
			default:
				out.print("        unres/inv.");
				break;
		}
		out.print(": ");
		var.type.printType(out);
		out.print(" ");
		out.print(var.name);
		if ((var.modifier & Modifier.MA_ACCSSD) == 0)
		{
			out.print(SKIPCAND);
		}
		else
			printMinimumAccessLevel(var.modifier);
		if (var.location == AccVar.L_CONST && var.init != null && var.init.isCompInitConstObject(ctx))
		{
			out.print(" (const init obj at 0x");
			out.printHexFix(ctx.mem.getAddrAsInt(var.getConstInitObj(ctx).outputLocation, 0), 8);
			if (var.getConstInitObj(ctx).inFlash)
				out.print(" in Flash");
			out.print(')');
		}
		out.println();
	}
	
	public void endVariableList()
	{
	}
	
	public void startMethodList()
	{
		out.println("- methods:");
		unitMthdCode = 0;
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
		char[] mthdSig;
		Param par;
		int i;
		String filename;
		StringList tmpPack;
		
		if (indir)
		{
			out.printHexFix(mthd.relOff, 8);
			out.print(": ");
			mthd.printSig(out);
			if ((mthd.modifier & Modifier.MA_ACCSSD) == 0)
				out.println(" (never used)");
			else
				out.println();
		}
		else
		{
			if (mthd.relOff != 0)
				out.printHexFix(mthd.relOff, 8);
			else
				out.print("no entry");
			if ((mthd.modifier & Modifier.M_ABSTR) != 0)
				out.print("  abstract");
			else
			{
				out.print("->");
				out.printHexFix(ctx.mem.getAddrAsInt(mthd.outputLocation, 0), 8);
			}
			out.print(": ");
			mthd.printSig(out);
			if ((mthd.modifier & (Modifier.M_ABSTR | Modifier.M_OVERLD | Modifier.MA_ACCSSD)) == 0 && !ovldMthdAccessed(mthd.ovldMthd))
				out.print(SKIPCAND);
			else
				printMinimumAccessLevel(mthd.modifier);
			if (!ctx.dynaMem && ((mthd.modifier & (Modifier.M_STAT | Modifier.M_FIN | Modifier.M_PRIV)) != 0 || (mthd.modifier & Modifier.M_OVERLD) == 0))
				out.print(" (statCall)");
			else
				out.print(" (dynaCall)");
			if (mthd.redirect != null)
			{
				out.print(" (redirected to ");
				mthd.redirect.owner.pack.printFullQID(out);
				out.print('.');
				out.print(mthd.redirect.owner.name);
				out.println(')');
			}
			else
			{
				out.print(" (stmtCnt==");
				out.print(mthd.stmtCnt);
				out.print(", ");
				unitMthdCode += mthd.codeSize;
				out.print(mthd.codeSize);
				out.println(" bytes)");
				if ((ctx.debugCode || (mthd.marker & Marks.K_DEBG) != 0) && (mthd.modifier & Modifier.M_ABSTR) == 0)
				{
					mthdSig = new char[mthd.parCnt + 1];
					if (mthd.retType != null)
						mthdSig[0] = mthd.retType.getSig();
					else
						mthdSig[0] = 'C';
					par = mthd.param;
					for (i = 1; i <= mthd.parCnt; i++)
					{
						mthdSig[i] = par.type.getSig();
						par = par.nextParam;
					}
					filename = ctx.debugPrefix;
					tmpPack = mthd.owner.pack.name;
					while (tmpPack != null)
					{
						filename = filename.concat(tmpPack.str);
						filename = filename.concat("_");
						tmpPack = tmpPack.next;
					}
					filename = filename.concat(mthd.owner.name);
					filename = filename.concat("_");
					filename = filename.concat(mthd.name);
					filename = filename.concat("_");
					filename = filename.concat(new String(mthdSig));
					filename = filename.concat(".bin");
					if (!binOut.open(filename))
					{
						ctx.out.print("could not open method-code-file ");
						ctx.out.println(filename);
						return;
					}
					else
					{
						ctx.mem.appendImagePart(binOut, mthd.outputLocation, ctx.codeStart, mthd.codeSize);
						binOut.close();
					}
				}
			}
		}
	}
	
	public void endMethodList()
	{
		out.print("- size of code for methods: ");
		out.print(unitMthdCode);
		out.println(" bytes");
	}
	
	public void startStatObjList()
	{
		out.println("- added statically allocated objects:");
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
		if (rela != -1)
		{
			out.printHexFix(rela, 8);
			out.print("->");
		}
		else
			out.print("          ");
		if (loc != null)
			out.printHexFix(ctx.mem.getAddrAsInt(loc, 0), 8);
		else
			out.print("skipped ");
		if (value != null)
		{
			if (inFlash)
				out.print(" in Flash");
			out.print(": ");
			out.print(value);
		}
		out.println();
	}
	
	public void endStatObjList()
	{
	}
	
	public void startImportedUnitList()
	{
		out.println("- added imports:");
	}
	
	public void hasImportedUnit(UnitList ul)
	{
		out.printHexFix(ul.relOff, 8);
		out.print("->");
		out.printHexFix(ctx.mem.getAddrAsInt(ul.unit.outputLocation, 0), 8);
		out.print(": ");
		if (ul.unit.pack != null)
		{
			ul.unit.pack.printFullQID(out);
			out.print(".");
		}
		out.println(ul.unit.name);
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
		out.println("- added interface-maps:");
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
		int i, cnt;
		
		out.printHexFix(ctx.mem.getAddrAsInt(intf.outputLocation, 0), 8);
		out.print(" for \"");
		out.print(intf.intf.name);
		out.println("\":");
		cnt = intf.map.length;
		for (i = 1; i < cnt; i++)
		{
			out.print("   ");
			out.printHexFix(i, 8);
			out.print(" <=> ");
			out.printHexFix(intf.map[i].relOff, 8);
			out.println();
		}
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endUnit()
	{
		out.println();
	}
	
	private void printMinimumAccessLevel(int modifier)
	{
		int maxAccess;
		if ((modifier & Modifier.MA_PUB) != 0)
			maxAccess = Modifier.M_PUB;
		else if ((modifier & (Modifier.MA_PROT | Modifier.M_OVERLD)) != 0)
			maxAccess = Modifier.M_PROT;
		else if ((modifier & Modifier.MA_PACP) != 0)
			maxAccess = Modifier.M_PACP;
		else if ((modifier & Modifier.MA_PRIV) != 0)
			maxAccess = Modifier.M_PRIV;
		else
			return;
		if ((modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP | Modifier.M_PRIV)) < maxAccess)
		{
			out.print(" (may be reduced to ");
			switch (maxAccess)
			{
				case Modifier.M_PROT:
					out.print("protected)");
					break;
				case Modifier.M_PACP:
					out.print("package private)");
					break;
				case Modifier.M_PRIV:
					out.print("private)");
					break;
			}
		}
	}
	
	private boolean ovldMthdAccessed(Mthd ovldMthd)
	{
		while (ovldMthd != null)
		{
			if ((ovldMthd.modifier & Modifier.MA_ACCSSD) != 0)
				return true;
			ovldMthd = ovldMthd.ovldMthd;
		}
		return false;
	}
}
