/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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

package sjc.frontend.binimp;

import sjc.compbase.*;
import sjc.frontend.Language;

/**
 * BinImp: frontend language implementation for binary imported data
 *
 * @author S. Frenz
 * @version 120228 cleaned up "import sjc." typo
 * version 120227 cleaned up "package sjc." typo
 * version 091209 adopted changed UnitDummy and ExConstInitObj
 * version 091102 adopted changed code block storage
 * version 091021 adopted changed modifier declarations
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080613 added support for binary imported methods
 * version 070909 adopted changed ExAccVrbl
 * version 070727 adopted changed type of id from PureID to String
 * version 070713 moved functionality of Importer hereto
 * version 070114 reduced access level where possible
 * version 061211 removed checkEnvironment
 * version 061202 optimized static modes
 * version 060818 added support to import an internal byte array
 * version 060607 initial version
 */

public class BinImp extends Language
{
	private Context ctx;
	private BImUnit myUnit;
	
	protected void init(Context iCtx)
	{
		ctx = iCtx;
	}
	
	protected boolean fileCompetence(String name)
	{
		return name.endsWith(".bib") || name.endsWith(".bim");
	}
	
	public boolean addByteArray(byte[] data, int startoffset, int stopoffset, String name)
	{
		byte[] tmp;
		int i;
		
		//copy data only if neccessary
		if (startoffset != 0 || stopoffset != data.length)
		{
			tmp = new byte[stopoffset - startoffset];
			for (i = startoffset; i < stopoffset; i++)
				tmp[i - startoffset] = data[i];
			data = tmp;
		}
		//import array
		return importByteData(data, name, -1);
	}
	
	protected boolean scanparseFile(StringList fileName)
	{
		byte[] data;
		
		if ((data = ctx.osio.readFile(fileName.str)) == null)
		{
			ctx.out.print("Error opening input-file: ");
			ctx.out.println(fileName.str);
			return false;
		}
		if (fileName.str.endsWith(".bib"))
			return importByteData(data, null, fileName.tablePos);
		if (fileName.str.endsWith(".bim"))
			return importMthdData(data, fileName.tablePos);
		ctx.out.println("BinImp-call without file-competence");
		return false;
	}
	
	private boolean importByteData(byte[] data, String name, int fileID)
	{
		Vrbl newVar;
		TypeRef type;
		
		if (myUnit == null)
		{
			//create unit
			myUnit = new BImUnit();
			myUnit.name = "ByteData";
			//enter unit in package
			myUnit.pack = new QualID(new StringList("binimp"), QualID.Q_PACKAGE, fileID, -1, -1);
			if ((myUnit.pack.packDest = ctx.root.searchSubPackage(myUnit.pack.name, true)) == null)
			{
				ctx.out.println("name-conflict for package binimp");
				return false;
			}
			myUnit.pack.packDest.addUnit(myUnit);
			ctx.addUnit(myUnit);
		}
		//create variable
		if (name == null)
			name = ctx.getPlainNameOfFile(fileID);
		if (name == null)
		{
			ctx.out.println("invalid name during import");
			return false;
		}
		newVar = new Vrbl(name, Modifier.M_FIN | Modifier.M_PUB | Modifier.M_STAT | Modifier.MF_ISWRITTEN, fileID, -1, -1);
		newVar.type = type = new TypeRef(fileID, -1, -1);
		newVar.location = Vrbl.L_CONSTDC;
		newVar.owner = myUnit;
		type.baseType = StdTypes.T_BYTE;
		type.arrDim = 1;
		newVar.init = new BImExpr(data, fileID);
		newVar.nextVrbl = myUnit.vars;
		myUnit.vars = newVar;
		return true;
	}
	
	private boolean importMthdData(byte[] data, int fileID)
	{
		DataBlockList newData;
		String name = ctx.getPlainNameOfFile(fileID);
		
		newData = ctx.codeBlocks;
		while (newData != null)
		{
			if (newData.name.equals(name))
			{
				ctx.out.print("name conflict for binary imported machine-code ");
				ctx.printPos(fileID, -1, -1);
				ctx.out.print(" named ");
				ctx.out.println(name);
				return false;
			}
			newData = newData.nextDataBlock;
		}
		newData = new DataBlockList();
		newData.data = data;
		newData.name = name;
		newData.nextDataBlock = ctx.codeBlocks;
		ctx.codeBlocks = newData;
		return true;
	}
}
