/* Copyright (C) 2008, 2009, 2010, 2012 Stefan Frenz
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
import sjc.osio.TextPrinter;

/**
 * CodeInfo: symbol information writer containing code information
 *
 * @author S. Frenz
 * @version 120326 added closing of printer
 * version 100331 adopted changed DebugWriter
 * version 100115 adopted codeStart-movement
 * version 091105 initial version
 */

public class CodeInfo extends DebugWriter
{
	protected TextPrinter finalOut;
	protected Context ctx;
	
	public CodeInfo(String filename, Context ic)
	{
		if ((finalOut = ic.osio.getNewFilePrinter(filename)) == null)
		{
			ic.out.println("current system does not support writing to text files");
			finalOut = ic.out;
		}
		ctx = ic;
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
	}
	
	public void finalizeImageInfo()
	{
		finalOut.close();
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
	}
	
	public void startUnit(String type, Unit unit)
	{
	}
	
	public void markUnitAsNotUsed()
	{
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
	}
	
	public void startVariableList()
	{
	}
	
	public void hasVariable(Vrbl var)
	{
	}
	
	public void endVariableList()
	{
	}
	
	public void startMethodList()
	{
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
		int i;
		
		if (mthd.codeSize == 0)
			return;
		finalOut.print("code: ");
		finalOut.print("0x");
		finalOut.printHexFix(ctx.mem.getAddrAsInt(mthd.outputLocation, ctx.codeStart), 8);
		finalOut.print(' ');
		finalOut.print(mthd.codeSize);
		finalOut.print(" 0x");
		finalOut.printHexFix(ctx.mem.getAddrAsInt(mthd.outputLocation, ctx.codeStart + mthd.codeSize - 1), 8);
		finalOut.println();
		
		finalOut.print("mthd: ");
		if (mthd.retType != null)
		{
			mthd.retType.printType(finalOut);
			finalOut.print(' ');
		}
		else
			finalOut.print("#constr# ");
		mthd.owner.printNameWithOuter(finalOut);
		finalOut.print('.');
		mthd.printNamePar(finalOut);
		finalOut.println();
		
		finalOut.print("file: ");
		finalOut.print(ctx.getNameOfFile(mthd.fileID));
		finalOut.println();
		
		finalOut.print("line: ");
		finalOut.println(mthd.line);
		
		if (mthd.lineInCodeOffset != null)
		{
			finalOut.print("slhi:");
			for (i = 0; i < mthd.lineInCodeOffset.length; i++)
			{
				if (i > 0 && (i & 7) == 0)
					finalOut.println();
				finalOut.print(' ');
				finalOut.print(mthd.lineInCodeOffset[i]);
			}
			finalOut.println();
		}
		
		finalOut.println();
	}
	
	public void endMethodList()
	{
	}
	
	public void startStatObjList()
	{
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
	}
	
	public void endStatObjList()
	{
	}
	
	public void startImportedUnitList()
	{
	}
	
	public void hasImportedUnit(UnitList ul)
	{
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endUnit()
	{
	}
}
