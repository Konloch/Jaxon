/* Copyright (C) 2007, 2008, 2009, 2010, 2011, 2015 Stefan Frenz
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
 * RawSymbols: generate output for all packages, units and methods; append byte stream to image
 *
 * @author S. Frenz
 * @version 151031 adopted changed TextBuffer
 * version 110624 adopted changed Context
 * version 110608 fixed langroot-extension check
 * version 110607 added option to change BootStrap class
 * version 101226 adopted changed SymbolInformer
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090207 added copyright notice
 * version 080706 adopted removal of printExts
 * version 080701 adopted move of TextBuffer to osio
 * version 080614 adopted changed Unit.searchVariables
 * version 080118 use of SymbolInformer.NOPARMREQ
 * version 080115 adopted changes in SymbolInformer.generateSymbols
 * version 080114 removed checkParameter
 * version 070729 removed creation of internal variable and inserted check of declaration instead
 * version 070727 adopted changed type of id from PureID to String
 * version 070713 initial version
 */

public class RawSymbols extends SymbolInformer
{
	private TextBuffer myBuffer;
	private String clssBootstrap = "symbols.RawInfo";
	
	public static void printValidParameters(TextPrinter v)
	{
		v.println(NOPARMREQ);
	}
	
	public String getName()
	{
		return "RawSymbols";
	}
	
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if (parm == null || parm.length() < 1)
		{
			v.println("invalid BootStrap class");
			return false;
		}
		v.print("setting BootStrap class to ");
		v.println(clssBootstrap = parm);
		return true;
	}
	
	public boolean generateSymbols(UnitList newUnits, Context ictx)
	{
		Unit myUnit;
		Vrbl myVar;
		StringList list;
		Object addr;
		int off = 0, addrOff;
		
		//remember Context
		ctx = ictx;
		//check declared byte[] symbols.RawInfo.info
		list = StringList.buildStringList(clssBootstrap);
		if ((myUnit = ctx.root.searchUnit(list)) == null || (myUnit.modifier & (Modifier.M_ABSTR | Modifier.M_INDIR | Modifier.M_STRUCT)) != 0)
		{
			ctx.out.print("unit ");
			ctx.out.print(clssBootstrap);
			ctx.out.println(" required for RawSymbols (non-abstract, non-indir, non-struct)");
			return false;
		}
		if ((myVar = myUnit.searchVariable("info", ctx)) == null || (myVar.modifier & (Modifier.M_STAT | Modifier.M_STRUCT)) != Modifier.M_STAT || myVar.type.baseType != TypeRef.T_BYTE || myVar.type.arrDim != 1)
		{
			ctx.out.print("variable static byte[] info needed in bootstrap class ");
			ctx.out.println(clssBootstrap);
			return false;
		}
		//generate symbol information
		myBuffer = new TextBuffer();
		genPackSymbols(ctx.root);
		myBuffer.print('!');
		//allocate memory in image
		if ((addr = ctx.mem.allocateArray(myBuffer.used, 1, 1, StdTypes.T_BYTE, null)) == null)
		{
			ctx.out.println("error in allocating memory for symbol information string");
			return false;
		}
		//enter address of target in unit variable
		ctx.arch.putRef(myUnit.outputLocation, myVar.relOff, addr, 0);
		//get real address start
		if (ctx.indirScalars)
		{
			addr = ctx.mem.getIndirScalarObject(addr);
			addrOff = ctx.rteSArray.instIndirScalarTableSize;
		}
		else
			addrOff = ctx.rteSArray.instScalarTableSize;
		//copy information
		while (off < myBuffer.used)
			ctx.mem.putByte(addr, addrOff++, (byte) myBuffer.data[off++]);
		//everything ok
		return true;
	}
	
	private void genPackSymbols(Pack pack)
	{
		while (pack != null)
		{
			myBuffer.print('P');
			pack.printFullName(myBuffer);
			myBuffer.print('!');
			genUnitSymbols(pack.units);
			genPackSymbols(pack.subPacks);
			pack = pack.nextPack;
		}
	}
	
	private void genUnitSymbols(Unit unit)
	{
		while (unit != null)
		{
			myBuffer.print('U');
			myBuffer.print(unit.name);
			myBuffer.print('#');
			myBuffer.printHexFix(unit.modifier, 8);
			myBuffer.print(ctx.mem.getAddrAsInt(unit.outputLocation, 0));
			if (unit.extsID != null && unit.extsID.unitDest != ctx.langRoot)
				unit.extsID.printFullQID(myBuffer);
			myBuffer.print('!');
			genMthdSymbols(unit.mthds);
			unit = unit.nextUnit;
		}
	}
	
	private void genMthdSymbols(Mthd mthd)
	{
		while (mthd != null)
		{
			myBuffer.print('M');
			mthd.printNamePar(myBuffer);
			myBuffer.print('#');
			myBuffer.printHexFix(mthd.modifier, 8);
			myBuffer.printHexFix(ctx.mem.getAddrAsInt(mthd.outputLocation, 0), 8);
			myBuffer.print('!');
			mthd = mthd.nextMthd;
		}
	}
}
