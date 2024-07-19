/* Copyright (C) 2008, 2009, 2010 Stefan Frenz
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
 * MthdSymbols: generate output for method names
 *
 * @author S. Frenz
 * @version 101226 adopted changed SymbolInformer
 * version 091218 added support for chaining method blocks
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080701 adopted move of TextBuffer to osio
 * version 080118 initial version
 */

public class MthdSymbols extends SymbolInformer
{
	private TextBuffer myBuffer;
	private int mthdNameParPos, mthdNextMthdPos, mthdFirstMthdPos;
	private boolean chainMethodBlocks;
	
	public static void printValidParameters(TextPrinter v)
	{
		v.println("   chain        chain method blocks");
	}
	
	public String getName()
	{
		return "MthdSymbols";
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if ("chain".equals(parm))
		{
			chainMethodBlocks = true;
			return true;
		}
		v.println("MthdSymbols accepts only the following parameters:");
		printValidParameters(v);
		return false;
	}
	
	public boolean generateSymbols(UnitList newUnits, Context ictx)
	{
		int addedMthdSize, unitNameLen;
		Unit unit;
		Mthd mthds, lastMthd = null;
		
		ctx = ictx; //required for checkVrbl, access to ictx should be prefered
		myBuffer = new TextBuffer();
		if ((mthdNameParPos = checkVrbl(ictx.rteSMthdBlock, "namePar")) == AccVar.INV_RELOFF)
			return false;
		if (chainMethodBlocks)
		{
			if ((mthdNextMthdPos = checkVrbl(ictx.rteSMthdBlock, "nextMthd")) == AccVar.INV_RELOFF || (mthdFirstMthdPos = checkVrbl(ictx.rteSMthdBlock, "firstMthd")) == AccVar.INV_RELOFF)
				return false;
		}
		addedMthdSize = (4 + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits;
		if (chainMethodBlocks)
		{
			ictx.symGenSize += ctx.arch.relocBytes; //one static reference
			addedMthdSize += ctx.arch.relocBytes; //one instance reference
		}
		while (newUnits != null)
		{
			myBuffer.used = 0;
			(unit = newUnits.unit).pack.printFullQID(myBuffer);
			myBuffer.print('.');
			unit.printNameWithOuter(myBuffer);
			myBuffer.print('.');
			unitNameLen = myBuffer.used;
			mthds = unit.mthds;
			while (mthds != null)
			{
				if (mthds.outputLocation != null)
				{
					ictx.symGenSize += addedMthdSize;
					myBuffer.used = unitNameLen; //reset to unit name
					mthds.printNamePar(myBuffer);
					ictx.arch.putRef(mthds.outputLocation, mthdNameParPos, ictx.allocateString(new String(myBuffer.data, 0, myBuffer.used)), 0);
					if (chainMethodBlocks)
					{
						if (lastMthd == null)
						{ //enter first method in static field
							ctx.arch.putRef(ictx.rteSMthdBlock.outputLocation, mthdFirstMthdPos, mthds.outputLocation, 0);
						}
						else
						{ //enter method in last method
							ctx.arch.putRef(lastMthd.outputLocation, mthdNextMthdPos, mthds.outputLocation, 0);
						}
						lastMthd = mthds;
					}
				}
				mthds = mthds.nextMthd;
			}
			newUnits = newUnits.next;
		}
		return true;
	}
}
