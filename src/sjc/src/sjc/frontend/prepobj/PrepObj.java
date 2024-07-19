/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.frontend.prepobj;

import sjc.compbase.*;
import sjc.frontend.ExVal;
import sjc.frontend.Language;
import sjc.frontend.SScanner;
import sjc.osio.TextReader;

/**
 * PrepObj: frontend language implementation for compile-time prepared objects
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100513 adopted changed TextReader
 * version 091214 fixed error reporting after values
 * version 091210 initial version
 */

public class PrepObj extends Language
{
	private final static String MISSING_IDENTIFIER = "identifier expected";
	
	private Context ctx;
	private TextReader inText;
	private SScanner s;
	private Unit myUnit;
	private Vrbl lastVar;
	private int curFID;
	
	//---###--- language interface ---###---
	
	protected void init(Context ictx)
	{
		ctx = ictx;
		inText = new TextReader();
		s = new SScanner();
	}
	
	protected boolean fileCompetence(String name)
	{
		return name.endsWith(".pob");
	}
	
	protected boolean scanparseFile(StringList fileName)
	{
		boolean success = true;
		
		//create unit if not done already
		if (myUnit == null)
		{
			myUnit = new PObjUnit();
			myUnit.name = "Data";
			//enter unit in package
			myUnit.pack = new QualID(new StringList("prepobj"), QualID.Q_PACKAGE, fileName.tablePos, -1, -1);
			if ((myUnit.pack.packDest = ctx.root.searchSubPackage(myUnit.pack.name, true)) == null)
			{
				ctx.out.println("name-conflict for package prepobj");
				return false;
			}
			myUnit.pack.packDest.addUnit(myUnit);
			ctx.addUnit(myUnit);
		}
		//try to parse the file
		if (!inText.initData(ctx.osio.readFile(fileName.str)))
		{
			ctx.out.print("Error opening input-file: ");
			ctx.out.println(fileName.str);
			success = false;
		}
		else
		{
			s.init(inText, curFID = fileName.tablePos, ctx);
			if (!tokenize())
			{
				ctx.out.print("...parsing ");
				ctx.out.print(fileName.str);
				ctx.out.println(" failed");
				success = false;
			}
		}
		return success;
	}
	
	//---###--- simple parser ---###---
	
	private boolean tokenize()
	{
		TypeRef type;
		String name;
		Vrbl myObj;
		
		while (s.nxtSym.type > 0)
		{ //>0: no error, valid input -> compile a variable definition
			//need a type and a name
			if ((type = typeRef()) == null)
				return false;
			if (!has(SScanner.S_ID))
			{
				parserError("name exptected");
				return false;
			}
			name = s.nxtSym.strBuf;
			accept();
			//create variable
			myObj = new Vrbl(name, Modifier.M_FIN | Modifier.M_PUB | Modifier.M_STAT | Modifier.MF_ISWRITTEN, curFID, s.nxtSym.syline, s.nxtSym.sycol);
			myObj.type = type;
			myObj.owner = myUnit;
			myObj.location = Vrbl.L_CONSTDC;
			//handle assignment
			if (!accept(SScanner.S_ASN, SScanner.RES))
			{
				parserError("expected \"=\" after variable declaration");
				return false;
			}
			if ((myObj.init = initExpr()) == null)
				return false;
			if (!accept(SScanner.S_DEL, SScanner.D_SEM))
			{
				parserError("expected \";\" after variable init");
				return false;
			}
			//enter variable in list
			if (lastVar == null)
				lastVar = myUnit.vars = myObj;
			else
			{
				lastVar.nextVrbl = myObj;
				lastVar = myObj;
			}
		}
		return true;
	}
	
	private QualID qualIdent()
	{
		StringList list, last;
		int syl, syc;
		
		if (!has(SScanner.S_ID))
		{
			parserError(MISSING_IDENTIFIER);
			return null;
		}
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		last = list = new StringList(null, s.nxtSym.strBuf);
		accept();
		while (accept(SScanner.S_DEL, SScanner.D_DOT))
		{
			if (has(SScanner.S_ID))
			{
				last = new StringList(last, s.nxtSym.strBuf);
				accept();
			}
			else
			{
				parserError(MISSING_IDENTIFIER);
				return null;
			}
		}
		return new QualID(list, QualID.Q_UNIT, curFID, syl, syc);
	}
	
	private TypeRef typeRef()
	{
		TypeRef t;
		
		if (has(SScanner.S_TYP))
		{
			(t = new TypeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol)).baseType = s.nxtSym.par;
			accept();
		}
		else
		{
			(t = new TypeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol)).baseType = TypeRef.T_QID;
			if ((t.qid = qualIdent()) == null)
			{
				parserError("missing type");
				return null;
			}
		}
		while (accept(SScanner.S_ENC, SScanner.E_SOC))
			t.arrDim++;
		return t;
	}
	
	private ExObjInit initObj(TypeRef requestedType)
	{
		ExObjInit block = new ExObjInit(requestedType, curFID, s.nxtSym.syline, s.nxtSym.sycol);
		ExObjInitField first = null, last = null;
		
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("expected \"{\" before init block");
			return null;
		}
		while (has(SScanner.S_ID))
		{
			if (first == null)
				first = last = new ExObjInitField(curFID, s.nxtSym.syline, s.nxtSym.sycol);
			else
			{
				last.next = new ExObjInitField(curFID, s.nxtSym.syline, s.nxtSym.sycol);
				last = last.next;
			}
			last.name = s.nxtSym.strBuf;
			accept();
			if (!accept(SScanner.S_ASN, SScanner.RES))
			{
				parserError("expected \"=\" after field name");
				return null;
			}
			if ((last.init = initExpr()) == null)
				return null;
			if (!accept(SScanner.S_DEL, SScanner.D_SEM))
			{
				parserError("expected \";\" after value");
				return null;
			}
		}
		if (!accept(SScanner.S_ENC, SScanner.E_BC))
		{
			parserError("expected \"}\" after init block");
			return null;
		}
		block.vars = first;
		return block;
	}
	
	private Expr initExpr()
	{
		Expr ex;
		TypeRef type;
		
		if (has(SScanner.S_NUM))
			return num(); //numeric value
		if (has(SScanner.S_SCT))
		{ //constant string
			ex = new ExStr(s.nxtSym.strBuf, curFID, s.nxtSym.syline, s.nxtSym.sycol);
			accept();
			return ex;
		}
		//all other values need a type and a value
		if ((type = typeRef()) == null)
			return null;
		if (type.arrDim == 0)
			return initObj(type); //create another object
		//create an array
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("expected \"{\" for array init");
			return null;
		}
		if ((ex = arrayInit(type)) == null)
			return null;
		if (!accept(SScanner.S_ENC, SScanner.E_BC))
		{
			parserError("expected \"}\" after array init");
			return null;
		}
		return ex;
	}
	
	private Expr num()
	{
		ExVal num;
		
		if (!has(SScanner.S_NUM))
		{
			parserError("numeric value expected");
			return null;
		}
		num = new ExVal(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		switch (num.baseType = s.nxtSym.par)
		{
			case SScanner.T_BYTE:
			case SScanner.T_SHRT:
			case SScanner.T_INT:
			case SScanner.T_CHAR:
			case SScanner.T_BOOL:
			case SScanner.T_FLT:
				num.intValue = s.nxtSym.intBuf;
				break;
			case SScanner.T_LONG:
			case SScanner.T_DBL:
				num.longValue = s.nxtSym.longBuf;
				break;
			case SScanner.T_NULL:
				break; //nothing to copy
			default:
				parserError("### internal error in Parser.getOperandFragment: unknown S_NUM-type ###");
				return null;
		}
		accept();
		return num;
	}
	
	private ExArrayInit arrayInit(TypeRef forcedType)
	{
		ExArrayInit init;
		Expr ex;
		FilledParam last = null;
		int syl, syc;
		
		init = new ExArrayInit(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		init.forcedType = forcedType;
		do
		{
			if (has(SScanner.S_ENC, SScanner.E_BC))
				break; //ignore comma if followed by a closing bracket, support empty array initialization
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if ((ex = initExpr()) == null)
				return null;
			if (last == null)
				last = init.par = new FilledParam(ex, curFID, syl, syc);
			else
				last = last.nextParam = new FilledParam(ex, curFID, syl, syc);
		} while (accept(SScanner.S_DEL, SScanner.D_COM));
		return init;
	}
	
	//---###--- helper ---###---
	
	private boolean has(int t)
	{
		return s.nxtSym.type == t;
	}
	
	private boolean has(int t, int p)
	{
		return s.nxtSym.type == t && s.nxtSym.par == p;
	}
	
	private void accept()
	{
		s.next();
	}
	
	private boolean accept(int t, int p)
	{
		if (has(t, p))
		{
			s.next();
			return true;
		}
		return false;
	}
	
	private void parserError(String msg)
	{
		ctx.printPos(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		ctx.out.print(": parser-error: ");
		ctx.out.println(msg);
	}
}
