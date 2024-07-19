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
import sjc.frontend.Language;
import sjc.frontend.SScanner;
import sjc.osio.TextReader;

/**
 * SJava: administration of the SJava-language and access to the SJava-routines
 *
 * @author S. Frenz
 * @version 121017 added ignore-unit-flag for struct and flash dummy units
 * version 120227 fixed implicit basetype var init conversion check, cleaned up "package sjc." typo
 * version 110705 made internally created units static
 * version 110624 fixed package of internally created units
 * version 110429 fixed implicit conversion check and error message
 * version 100513 adopted changed TextReader
 * version 100512 clarified checkVarInitType, adopted changed Clss
 * version 100402 added SJC-special
 * version 100401 removed MARKER- and DEFINE-special, added FLASH-special
 * version 100113 fixed modifier of struct dummy class
 * version 091209 adopted movement of SScanner to frontend package
 * version 091112 added support for unified vrbl init checks
 * version 091102 added support for attaching source to Context
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 070801 added test to avoid re-creation of STRUCT-dummy
 * version 070114 reduced access level where possible
 * version 061222 dynamized Marker
 * version 061211 setting CALLED-flag of runtime methods
 * version 061202 minimal changes
 * version 061129 dynamized Magic
 * version 061027 added support for STRUCT
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public class SJava extends Language
{
	public final static String KEY_THIS = "this", KEY_SUPER = "super";
	public final static String KEY_MAGIC = "MAGIC", KEY_STRUCT = "STRUCT";
	public final static String KEY_FLASH = "FLASH", KEY_SJC = "SJC";
	private Context ctx;
	private SScanner s;
	private JParser p;
	private TextReader inText;
	
	protected static boolean isSpecialName(String name)
	{
		return name.equals(KEY_THIS) || name.equals(KEY_SUPER) || name.equals(KEY_MAGIC) || name.equals(KEY_STRUCT) || name.equals(KEY_FLASH) || name.equals(KEY_SJC);
	}
	
	protected static boolean checkVarInitType(Vrbl var, boolean explConv, Unit unitContext, Context ctx)
	{
		int cmpRes;
		boolean success = true;
		
		if (var.init != null && (cmpRes = var.type.compareType(var.init, true, ctx)) != TypeRef.C_EQ && cmpRes != TypeRef.C_TT)
		{
			TypeRef destType = null;
			if (!var.type.isObjType() && !explConv)
				destType = var.init.getBaseTypeConvertionType(var.type, false);
			else if (cmpRes != TypeRef.C_NP && var.type.isIntfType())
				destType = var.type;
			if (destType == null || (var.init = ExEnc.getConvertedResolvedExpr(var.init, destType, unitContext, ctx)) == null)
				success = false;
		}
		if (success)
			return true;
		if (var.init != null)
			var.init.printPos(ctx, "incompatible or not implicitly convertible types");
		return false;
	}
	
	protected void init(Context iCtx)
	{
		StringList strTmp;
		Magic magic;
		
		//store required variables
		ctx = iCtx;
		inText = new TextReader();
		s = new SScanner();
		p = new JParser();
		strTmp = new StringList(null, "java");
		strTmp.next = new StringList(null, "lang");
		//if there is another language that defines the default package, insert check here
		ctx.defUnits = ctx.root.searchSubPackage(strTmp, true);
		//add dummy-class for STRUCT-extension
		if (ctx.defUnits.searchUnit(KEY_STRUCT) == null)
		{
			ctx.structClass = new Clss(new QualID(strTmp, 0, -1, 0, 0), null, Modifier.M_ABSTR | Modifier.M_PUB | Modifier.M_STRUCT | Modifier.M_STAT, Marks.K_IGNU, -1, 0, 0);
			ctx.structClass.name = KEY_STRUCT;
			ctx.structClass.pack = ctx.defUnits.getQIDTo();
			ctx.defUnits.addUnit(ctx.structClass);
			ctx.addUnit(ctx.structClass);
		}
		//add dummy-class for FLASH-extension
		if (ctx.defUnits.searchUnit(KEY_FLASH) == null)
		{
			ctx.flashClass = new Clss(new QualID(strTmp, 0, -1, 0, 0), null, Modifier.M_ABSTR | Modifier.M_PUB | Modifier.MM_FLASH | Modifier.M_STAT, Marks.K_IGNU, -1, 0, 0);
			ctx.flashClass.name = KEY_FLASH;
			ctx.flashClass.pack = ctx.defUnits.getQIDTo();
			ctx.defUnits.addUnit(ctx.flashClass);
			ctx.addUnit(ctx.flashClass);
		}
		//initialize internal special functions
		(magic = new Magic(ctx)).nextConfig = ctx.config;
		ctx.config = magic;
	}
	
	protected boolean fileCompetence(String name)
	{
		return name.endsWith(".java");
	}
	
	protected boolean scanparseFile(StringList fileName)
	{
		boolean success = true;
		
		//try to parse the file
		if (!inText.initData(ctx.osio.readFile(fileName.str)))
		{
			ctx.out.print("Error opening input-file: ");
			ctx.out.println(fileName.str);
			success = false;
		}
		else
		{
			ctx.attachSource(fileName.tablePos, inText.data);
			s.init(inText, fileName.tablePos, ctx);
			if (!p.tokenize(s, fileName.tablePos, ctx))
			{
				ctx.out.print("...parsing ");
				ctx.out.print(fileName.str);
				ctx.out.println(" failed");
				success = false;
			}
		}
		return success;
	}
}
