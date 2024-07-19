/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014 Stefan Frenz
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
import sjc.frontend.ExVal;
import sjc.frontend.SScanner;

/**
 * JParser: parser for the SJava-language
 *
 * @author S. Frenz
 * @version 140507 added support for RefToFlash annotation
 * version 140124 added support for Flash annotation at local variables
 * version 121031 fixed inner unit order
 * version 121020 fixed enhanced for loop for full qualified types
 * version 120413 added support for annotations at variable declarations inside method blocks
 * version 120227 cleaned up "package sjc." typo
 * version 110705 added support for additional semicolon after class declaration and alternative array dimension declaration for methods, inserted valid error message for local classes inside methods, made all top-level units static
 * version 110626 added support for multiple static init blocks per class
 * version 110624 doing package check even ignored units, too
 * version 101222 added SJC.GenDesc
 * version 101125 added NativeCallback
 * version 101027 adopted changed assign+op encoding
 * version 101021 added support for assert statement and SJC.CheckAssert annotation
 * version 100924 allowed dispensable semicolons between unit-fields
 * version 100923 allowed package private for inner units
 * version 100902 fixed modifier checks for inner interfaces
 * version 100512 adopted changed Modifier/Marks
 * version 100510 adopted changed ExClssMthdName
 * version 100504 adopted changed StBreakable structure, adopted changed ExClssMthdName
 * version 100419 added support for annotated parameters in method declarations
 * version 100411 added support for enhanced for loop syntax of jdk1.5
 * version 100402 replaced IGNORECLASS semantic by SJC.IgnoreUnit annotation
 * version 100401 added support for annotations
 * version 091209 adopted movement of SScanner to frontend package
 * version 091208 added support for array initialization in dynamic context and new with array initialization
 * version 091123 added support for trailing comma in array initialization, optimized arrayInit
 * version 091116 added support for empty array initialization
 * version 091109 removed optimization of outer-block list to support abbreviated block detection, added empty statement for potential switch-destinations
 * version 091103 fixed source hinting for expressions and expression lists
 * version 091102 added support for srcStart and srcLength filling in token
 * version 091021 adopted changed modifier declarations
 * version 091018 optimized implementation of stmt(.)
 * version 091013 giving each variable its own TypeRef
 * version 090918 added support for default modifier "package private"
 * version 090916 fixed valid modifiers for interface fields and methods
 * version 090718 added support for MF_WRITTEN flag for global variables and filled parameters, added support for final modifier for locals and params
 * version 090616 adopted changed ExVar and ExCall
 * version 090506 always inserting return-missing object, check for coding is done in StRetMissing
 * version 090430 added support for inserted "return missing" exceptions
 * version 090306 added naming for static init methods
 * version 090221 fixed default access level modifier for classes and interfaces
 * version 090219 optimized modifier checks
 * version 090218 added support for "synchronized" method and block modifier, changed modifier checks from opt-out to opt-in
 * version 090207 added copyright notice
 * version 080622 added support for static class initialization
 * version 080619 added throwUsed-setting for throw-statement
 * version 080614 allowed constant objects for interfaces
 * version 080603 added support for try-catch-finally statement and throws-declaration
 * version 080506 fixed setting of curMthd for var-init
 * version 080118 better comments, ignoring annotations
 * version 080106 added support for anonymous inner classes
 * version 071222 added support for inner units
 * version 071004 fixed interface field modifier check
 * version 071001 added support for "native" method modifier
 * version 070918 added support for multidimensional initialized arrays
 * version 070917 added support for optimized zero-initialization of local variables
 * version 070913 changed handling of declaration of local variables
 * version 070910 fixed ?:-assoviativeness
 * version 070909 added support for labels
 * version 070829 detection of unsupported array declaration in interfaces
 * version 070808 added support for float and double
 * version 070731 changed QualID[] to QualIDList, adopted renaming of id to name
 * version 070727 adopted change of Mthd.id from PureID to String
 * version 070628 adopted changes for "Expr extends TypeRef", inserted abort if invalid ExEnc-expression
 * version 070519 moved setting of variable owner hereto
 * version 070114 reduced access level where possible, default modifier protected for clss-/intfFields
 * version 070113 fixed instanceof for arrays
 * version 070106 replaced array detection
 * version 061228 added method statement statistic
 * version 061202 minimal changes
 * version 061129 optimized assign/check-sequences
 * version 061128 added support for expression list in for loop
 * version 061126 added support for ?:-operator
 * version 061027 moved addUnit to Context
 * version 060623 added progress bar
 * version 060607 initial version
 */

public class JParser
{
	private final static String UNREACHABLE_LABEL = "unreachable label";
	private final static String MISSING_BC_AFTER_ARRAY_INIT = "missing \"}\" after array initialization";
	private final static String MISSING_SEM_AFTER_VRBL = "missing \";\" after variable-declaration";
	
	private SScanner s;
	private int curFID, mthdStmtCnt, loopLevel;
	private Context ctx;
	private int progressCounter;
	private Clss curClass; //required for anonymous inner classes
	private Mthd curMthd; //required for anonymous inner classes
	
	private int mod_modifier; //Java modifiers delivered by getModifier
	private int mod_marker; //SJC special modifiers delivered by getModifier
	private FilledAnno mod_anno; //SJC special annotation modifier delivered by getModifier
	
	protected boolean tokenize(SScanner is, int fileID, Context ic)
	{
		boolean success;
		
		s = is;
		curFID = fileID;
		ctx = ic;
		progressCounter = 0;
		success = doTokenize();
		if (progressCounter > 7)
			ctx.out.println();
		return success;
	}
	
	private boolean doTokenize()
	{
		int mod, mark, syl, syc, syp;
		Unit c = null;
		QualID pack;
		QualIDList impt, lastImpt = null;
		Pack destPack;
		boolean insertUnit;
		
		while (s.nxtSym.type > 0)
		{ //>0: no error, valid input -> compile a complete CompileUnit
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			syp = s.nxtSym.sypos;
			//package
			pack = null;
			if (accept(SScanner.S_OKE, SScanner.O_PACK))
			{
				if ((pack = qualIdent(QualID.Q_PACKAGE)) == null)
					return false;
				if (!accept(SScanner.S_DEL, SScanner.D_SEM))
				{
					parserError("missing \";\" after package");
					return false;
				}
			}
			else
			{
				pack = new QualID(null, QualID.Q_PACKAGE, curFID, syl, syc); //no explicit package, create dummy-root-package
				pack.srcStart = syp;
				pack.srcLength = s.endOfLastSymbol - syp;
				syl = s.nxtSym.syline;
				syc = s.nxtSym.sycol;
				syp = s.nxtSym.sypos;
			}
			//import
			impt = null;
			while (accept(SScanner.S_OKE, SScanner.O_IMPT))
			{
				if (impt == null)
					lastImpt = impt = new QualIDList();
				else
				{
					lastImpt.nextQualID = new QualIDList();
					lastImpt = lastImpt.nextQualID;
				}
				if ((lastImpt.qid = qualIdent(QualID.Q_IMPORTPACK)) == null)
					return false;
				if (!accept(SScanner.S_DEL, SScanner.D_SEM))
				{
					parserError("missing \";\" after import");
					return false;
				}
			}
			//modifiers
			if (!getModifier(true, true))
				return false;
			mod = mod_modifier;
			insertUnit = true; //default: insert unit
			if ((mod_marker & Marks.K_IGNU) != 0)
			{
				insertUnit = false;
				mark = 0;
			}
			else
				mark = mod_marker;
			if (mod_anno != null)
			{
				parserError("invalid SJC-annotation for unit");
				return false;
			}
			//class or interface or annotation
			if (accept(SScanner.S_OKE, SScanner.O_CLSS))
			{
				if ((mod & ~(Modifier.M_PUB | Modifier.M_PROT | Modifier.M_ABSTR | Modifier.M_FIN)) != 0)
				{
					parserError("invalid modifier for class");
					return false;
				}
				mod |= Modifier.M_STAT; //all top-level classes are static
				if ((mod & (Modifier.M_PUB | Modifier.M_PROT)) == 0)
					mod |= Modifier.M_PACP; //default: package private
				if ((c = clssDecl(pack, impt, mod, mark, syl, syc)) == null)
					return false;
			}
			else if (accept(SScanner.S_OKE, SScanner.O_INTF))
			{
				if ((mod & ~(SScanner.M_PUB | SScanner.M_PROT | SScanner.M_FIN)) != 0)
				{
					parserError("invalid modifier for interface");
					return false;
				}
				if ((mod & (SScanner.M_PUB | SScanner.M_PROT)) == 0)
					mod |= Modifier.M_PACP; //default: package private
				if ((c = intfDecl(pack, impt, mod, syl, syc)) == null)
					return false;
			}
			else if (accept(SScanner.S_OKE, SScanner.O_ANDC))
			{
				if ((mod & ~(SScanner.M_PUB | SScanner.M_PROT | SScanner.M_FIN)) != 0)
				{
					parserError("invalid modifier for annotation");
					return false;
				}
				if ((c = annoDecl(pack, impt, mod, syl, syc)) == null)
					return false;
			}
			else
			{
				parserError("class or interface or annotation expected");
				return false;
			}
			//remove remaining semicolons
			while (accept(SScanner.S_DEL, SScanner.D_SEM)) /*remove it*/
				;
			//handle srcStart and srcLength
			c.srcStart = syp;
			c.srcLength = s.endOfLastSymbol - syp;
			//enter unit in package, this reduces needed passes
			if (pack.name == null)
				destPack = ctx.root;
			else
				destPack = ctx.root.searchSubPackage(pack.name, true);
			if (destPack == null)
			{
				ctx.printPos(curFID, syl, syc);
				ctx.out.print(": name-conflict for package ");
				pack.printFullQID(ctx.out);
				ctx.out.print(" in unit ");
				ctx.out.println(c.name);
				return false;
			}
			pack.packDest = destPack;
			if (insertUnit)
			{ //unit has not to be ignored, enter in list
				if (!destPack.addUnit(c))
				{
					ctx.printPos(curFID, syl, syc);
					ctx.out.print(": name-conflict for unit ");
					ctx.out.print(c.name);
					ctx.out.print(" in package ");
					pack.printFullQID(ctx.out);
					ctx.out.println();
					return false;
				}
				ctx.addUnit(c);
			}
			//compile-Unit done
			if ((++progressCounter & 7) == 0)
			{
				if (progressCounter == 8)
					ctx.out.print("   progress: .");
				else
					ctx.out.print('.');
			}
		}
		//success
		return true;
	}
	
	//---###--- eaters ---###---
	
	private boolean getModifier(boolean allowAllModifiers, boolean allowFinalModifier)
	{
		int i;
		
		mod_marker = mod_modifier = 0;
		mod_anno = null;
		//get modifier
		while (has(SScanner.S_MOD))
		{
			if (accept(SScanner.S_MOD, SScanner.M_ANNO))
			{ //annotation modifier
				TypeRef type;
				if ((type = typeRef(false, false)) == null)
				{
					parserError("annotation type expected");
					return false;
				}
				//only handle SJC-annotations
				if (type.qid.name.str.equals("SJC"))
				{ //SJC-annotation
					String aName;
					if (type.qid.name.next == null)
						aName = "";
					else
					{
						if (type.qid.name.next != null && type.qid.name.next.next != null)
						{
							parserError("invalid SJC-annotation");
							return false;
						}
						aName = type.qid.name.next.str;
					}
					boolean annoDone = false;
					if (!has(SScanner.S_ENC, SScanner.E_RO))
					{ //trivial annotation to switch a flag
						annoDone = true; //all if-statements result in annoDone=true (except else-case, where it is reset)
						if (aName.equals("Interrupt"))
							mod_marker |= Marks.K_INTR;
						else if (aName.equals("Debug"))
							mod_marker |= Marks.K_DEBG;
						else if (aName.equals("Inline"))
							mod_marker |= Marks.K_FINL;
						else if (aName.equals("NoInline"))
							mod_marker |= Marks.K_NINL;
						else if (aName.equals("Profile"))
							mod_marker |= Marks.K_PROF;
						else if (aName.equals("NoProfile"))
							mod_marker |= Marks.K_NPRF;
						else if (aName.equals("StackExtreme"))
							mod_marker |= Marks.K_SEPC;
						else if (aName.equals("NoStackExtreme"))
							mod_marker |= Marks.K_NSPC;
						else if (aName.equals("PrintCode"))
							mod_marker |= Marks.K_PRCD;
						else if (aName.equals("NoOptimization"))
							mod_marker |= Marks.K_NOPT;
						else if (aName.equals("GenCode"))
							mod_marker |= Marks.K_FCDG;
						else if (aName.equals("SourceLines"))
							mod_marker |= Marks.K_SLHI;
						else if (aName.equals("ExplicitConversion"))
							mod_marker |= Marks.K_EXPC;
						else if (aName.equals("WinDLL"))
							mod_marker |= Marks.K_NWIN;
						else if (aName.equals("IgnoreUnit"))
							mod_marker |= Marks.K_IGNU;
						else if (aName.equals("GenDesc"))
							mod_marker |= Marks.K_FOCD;
						else if (aName.equals("CheckAssert"))
							mod_marker |= Marks.K_ASRT;
						else if (aName.equals("NativeCallback"))
							mod_marker |= Marks.K_NTCB;
						else if (aName.equals("Flash"))
							mod_modifier |= Modifier.MM_FLASH; //this one is not transitive
						else if (aName.equals("RefToFlash"))
							mod_modifier |= Modifier.MM_REFTOFLASH; //this one is not transitive
						else
							annoDone = false; //unknown flag, create full-blown annotation
					}
					if (!annoDone)
					{ //unknown or non-trivial SJC-annotation
						mod_anno = new FilledAnno(mod_anno, aName, curFID, s.nxtSym.syline, s.nxtSym.sycol);
						StringList lastID = null;
						FilledParam lastPar = null;
						if (accept(SScanner.S_ENC, SScanner.E_RO))
						{
							do
							{ //collect multiple parameters
								String key;
								if ((key = ident()) == null)
								{
									parserError("expected identifier for annotation parameter");
									return false;
								}
								StringList nextID = new StringList(key);
								if (!accept(SScanner.S_ASN, SScanner.RES))
								{
									parserError("expected \"=\" in annotation parameter");
									return false;
								}
								int syl = s.nxtSym.syline;
								int syc = s.nxtSym.sycol;
								Expr e;
								if ((e = expr()) == null)
									return false;
								FilledParam nextPar = new FilledParam(e, curFID, syl, syc);
								if (lastID == null)
								{
									mod_anno.keys = nextID;
									mod_anno.values = nextPar;
								}
								else
								{
									lastID.next = nextID;
									lastPar.nextParam = nextPar;
								}
								lastID = nextID;
								lastPar = nextPar;
							} while (accept(SScanner.S_DEL, SScanner.D_COM));
							if (!accept(SScanner.S_ENC, SScanner.E_RC))
							{
								parserError("expected \")\" after annotation parameters");
								return false;
							}
						}
					}
				}
				else if (accept(SScanner.S_ENC, SScanner.E_RO))
				{ //skip over non-SJC-annotation
					i = 1;
					while (i > 0)
					{
						if (accept(SScanner.S_ENC, SScanner.E_RO))
							i++;
						else if (accept(SScanner.S_ENC, SScanner.E_RC))
							i--;
						else
							accept();
					}
				}
			}
			else
			{ //normal modifier
				if (allowAllModifiers || (allowFinalModifier && s.nxtSym.par == SScanner.M_FIN))
				{
					if ((mod_modifier & s.nxtSym.par) != 0)
					{
						parserError("modifier already given");
						return false;
					}
					mod_modifier |= s.nxtSym.par;
					accept();
				}
				else
				{
					parserError("invalid modifier in this context");
					return false;
				}
			}
		}
		//check modifier: only one of public/protected/private
		i = 0;
		if ((mod_modifier & SScanner.M_PUB) != 0)
			i++;
		if ((mod_modifier & SScanner.M_PROT) != 0)
			i++;
		if ((mod_modifier & SScanner.M_PRIV) != 0)
			i++;
		if (i > 1)
		{
			parserError("more than one modifier public/protected/private");
			return false;
		}
		return true;
	}
	
	private String ident()
	{ //doesn't print errors
		String id;
		
		if (!has(SScanner.S_ID))
			return null;
		id = s.nxtSym.strBuf;
		accept();
		return id;
	}
	
	private QualID qualIdent(int type)
	{
		StringList list, last;
		boolean wildcard = false;
		int syl, syc;
		
		if (!has(SScanner.S_ID))
		{
			parserError("package-identifier expected");
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
			else if (has(SScanner.S_ARI, SScanner.A_MUL))
			{
				if (type != QualID.Q_IMPORTPACK)
				{
					parserError("no wildcard allowed here");
					return null;
				}
				accept();
				wildcard = true;
				break;
			}
			else
			{
				parserError("subpackage-identifier expected");
				return null;
			}
		}
		if (type == QualID.Q_IMPORTPACK && !wildcard)
			type = QualID.Q_IMPORTUNIT;
		return new QualID(list, type, curFID, syl, syc);
	}
	
	private QualIDList qualIDList()
	{
		QualIDList list = null, last = null;
		
		do
		{
			if (list == null)
				last = list = new QualIDList();
			else
			{
				last.nextQualID = new QualIDList();
				last = last.nextQualID;
			}
			if ((last.qid = qualIdent(QualID.Q_UNIT)) == null)
			{
				parserError("identifier expected in identifier-list");
				return null;
			}
		} while (accept(SScanner.S_DEL, SScanner.D_COM));
		return list;
	}
	
	private int getArrDim()
	{
		int d = 0;
		
		while (accept(SScanner.S_ENC, SScanner.E_SOC))
			d++;
		return d;
	}
	
	private TypeRef typeRef(boolean voidOK, boolean checkArray)
	{
		TypeRef t = null;
		
		if (accept(SScanner.S_OKE, SScanner.O_VOID))
		{
			if (voidOK)
				(t = new TypeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol)).baseType = TypeRef.T_VOID;
			else
			{
				parserError("void not allowed here");
				return null;
			}
		}
		else if (has(SScanner.S_TYP))
		{
			(t = new TypeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol)).baseType = s.nxtSym.par;
			accept();
		}
		else
		{
			(t = new TypeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol)).baseType = TypeRef.T_QID;
			if ((t.qid = qualIdent(QualID.Q_UNIT)) == null)
			{
				parserError("missing type");
				return null;
			}
		}
		if (checkArray)
			t.arrDim = getArrDim();
		return t;
	}
	
	private Clss clssDecl(QualID ip, QualIDList ii, int imod, int imark, int il, int ic)
	{
		Clss r;
		
		if (!has(SScanner.S_ID))
		{
			parserError("missing identifier after \"class\"");
			return null;
		}
		(r = new Clss(ip, ii, imod, imark, curFID, il, ic)).name = s.nxtSym.strBuf;
		accept();
		if (accept(SScanner.S_OKE, SScanner.O_EXTS))
		{
			if ((r.extsID = qualIdent(QualID.Q_UNIT)) == null)
			{
				parserError("missing identifier after \"extends\"");
				return null;
			}
		}
		if (accept(SScanner.S_OKE, SScanner.O_IMPL) && (r.extsImplIDList = qualIDList()) == null)
			return null;
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("missing \"{\" after class-identifier");
			return null;
		}
		if (!clssFields(r))
			return null;
		if (!accept(SScanner.S_ENC, SScanner.E_BC))
		{
			parserError("missing \"}\" after class-fields");
			return null;
		}
		return r;
	}
	
	private boolean clssFields(Clss c)
	{
		int mod, mark, syl, syc, type;
		FilledAnno anno;
		TypeRef trf;
		JMthd nm, lm = null, tmp;
		Vrbl nv, lv = null;
		Clss oldCurClass;
		Mthd oldCurMthd;
		StBlock sb;
		Stmt ss;
		
		oldCurClass = curClass;
		curClass = c;
		while (!has(SScanner.S_ENC, SScanner.E_BC))
		{
			while (accept(SScanner.S_DEL, SScanner.D_SEM)) /* discard empty fields */
				;
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if (!getModifier(true, true))
				return false;
			mod = mod_modifier;
			mark = mod_marker;
			anno = mod_anno;
			if (mod == SScanner.M_STAT && accept(SScanner.S_ENC, SScanner.E_BO))
			{
				tmp = ((JMthd) c.initStat);
				tmp.name = Unit.STATICMTHDNAME;
				loopLevel = mthdStmtCnt = 0;
				oldCurMthd = curMthd;
				curMthd = tmp;
				if ((sb = stmtBlock(null, null, false)) == null)
					return false;
				if (tmp.block == null)
				{ //first static block in current class
					tmp.modifier |= Modifier.M_NDCODE;
					tmp.nextMthd = ctx.staticInitMthds;
					ctx.staticInitMthds = tmp;
					tmp.block = new StBlock(null, null, c.fileID, c.line, c.col);
					tmp.block.stmts = sb;
				}
				else
				{ //go through other blocks, append current static block (keep order)
					ss = tmp.block.stmts;
					while (ss.nextStmt != null)
						ss = ss.nextStmt;
					ss.nextStmt = sb;
				}
				tmp.stmtCnt += mthdStmtCnt;
				if (!accept(SScanner.S_ENC, SScanner.E_BC))
				{
					parserError("missing \"}\" after static-init-block");
					return false;
				}
				curMthd = oldCurMthd;
			}
			else
			{
				if ((mod & (SScanner.M_PUB | SScanner.M_PROT | SScanner.M_PRIV)) == 0)
					mod |= Modifier.M_PACP; //default: package private
				if (has(SScanner.S_OKE, SScanner.O_CLSS) || has(SScanner.S_OKE, SScanner.O_INTF) || has(SScanner.S_OKE, SScanner.O_ANDC))
				{
					type = s.nxtSym.par;
					accept();
					if (!innerUnit(c, mod, mark, type, syl, syc))
						return false; //inner class or interface
				}
				else if (lookAhead(SScanner.S_ENC, SScanner.E_RO))
				{ //constructor
					if ((mod & (SScanner.M_PUB | SScanner.M_PROT | Modifier.M_PACP | SScanner.M_PRIV)) != mod)
					{
						parserError("missing return-type of method or invalid constructor");
						return false;
					}
					if ((nm = methodDecl(mod, anno, null, false, syl, syc)) == null)
						return false;
					nm.marker = mark;
					if (lm != null)
						lm.nextMthd = nm; //not first methoddecl
					else
						c.mthds = nm; //store first methoddecl
					lm = nm;
				}
				else
				{ //vardecl or methoddecl
					if ((trf = typeRef(true, true)) == null)
						return false;
					if (lookAhead(SScanner.S_ENC, SScanner.E_RO))
					{ //methoddecl
						if ((mod & (SScanner.M_PUB | SScanner.M_PROT | Modifier.M_PACP | SScanner.M_PRIV | SScanner.M_FIN | SScanner.M_STAT | SScanner.M_ABSTR | SScanner.M_NAT | SScanner.M_SYNC | Modifier.M_NDCODE)) != mod)
						{
							parserError("invalid modifier for method");
							return false;
						}
						if ((mod & (SScanner.M_ABSTR | SScanner.M_NAT)) == (SScanner.M_ABSTR | SScanner.M_NAT))
						{
							parserError("method can not be both abstract and native");
							return false;
						}
						if ((nm = methodDecl(mod, anno, trf, false, syl, syc)) == null)
							return false;
						nm.marker = mark;
						if (lm != null)
							lm.nextMthd = nm; //not first methoddecl
						else
							c.mthds = nm; //store first methoddecl
						lm = nm;
					}
					else
					{ //vardecl
						if ((mod & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP | Modifier.M_PRIV | Modifier.M_FIN | Modifier.M_STAT | Modifier.M_TRANS | Modifier.M_VOLAT | Modifier.MM_FLASH | Modifier.MM_REFTOFLASH)) != mod)
						{
							parserError("invalid modifier for variable");
							return false;
						}
						mod |= Modifier.MF_ISWRITTEN; //all global variables are treated as already written (special handling of final variables is done later)
						if ((nv = varDecl(c, mod, mod_anno, trf)) == null)
							return false;
						if (!accept(SScanner.S_DEL, SScanner.D_SEM))
						{
							parserError(MISSING_SEM_AFTER_VRBL);
							return false;
						}
						if (lv != null)
							lv.nextVrbl = nv; //not first vardecl
						else
							c.vars = nv; //store first vardecl
						lv = nv;
						while (lv.nextVrbl != null)
							lv = lv.nextVrbl; //search last vardecl
					}
				}
			}
		}
		curClass = oldCurClass;
		return true;
	}
	
	private boolean innerUnit(JUnit outer, int mod, int mark, int declSym, int syl, int syc)
	{
		JUnit inner;
		
		switch (declSym)
		{
			case SScanner.O_CLSS: //inner class
				if ((mod & (SScanner.M_PUB | SScanner.M_PROT | Modifier.M_PACP | SScanner.M_PRIV | SScanner.M_STAT | SScanner.M_ABSTR | SScanner.M_STAT | SScanner.M_FIN)) != mod)
				{
					parserError("invalid modifier for inner class");
					return false;
				}
				if ((inner = clssDecl(outer.pack, outer.impt, mod, mark, syl, syc)) == null)
					return false;
				break;
			case SScanner.O_INTF: //inner interface
				if ((mod & (SScanner.M_PUB | SScanner.M_PROT | Modifier.M_PACP | SScanner.M_PRIV | SScanner.M_FIN | SScanner.M_STAT)) != mod)
				{
					parserError("invalid modifier for interface");
					return false;
				}
				mod |= SScanner.M_STAT; //inner interfaces are implicitly static
				if ((inner = intfDecl(outer.pack, outer.impt, mod, syl, syc)) == null)
					return false;
				break;
			case SScanner.O_ANDC: //inner annotation
				if ((mod & (SScanner.M_PUB | SScanner.M_PROT | Modifier.M_PACP | SScanner.M_PRIV | SScanner.M_FIN)) != mod)
				{
					parserError("invalid modifier for annotation");
					return false;
				}
				if ((inner = annoDecl(outer.pack, outer.impt, mod, syl, syc)) == null)
					return false;
				break;
			default:
				parserError("invalid declaration for inner unit");
				return false;
		}
		insertInnerUnit(outer, inner);
		return true;
	}
	
	private void insertInnerUnit(JUnit outer, JUnit inner)
	{
		//insert unit in list of inner units of outer
		if (outer.innerUnits == null)
			outer.innerUnits = inner;
		else
		{
			Unit last = outer.innerUnits;
			while (last.nextUnit != null)
				last = last.nextUnit;
			last.nextUnit = inner;
		}
		//add link to outer unit and add unit to context
		inner.outerUnit = outer;
		ctx.addUnit(inner);
	}
	
	private Param getParam()
	{
		TypeRef t;
		String id;
		Param p;
		
		p = new Param(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		if (!getModifier(false, true))
			return null;
		p.modifier = mod_modifier;
		if ((p.modifier & ~(Modifier.M_FIN | Modifier.MM_FLASH | Modifier.MM_REFTOFLASH)) != 0 || mod_anno != null || mod_marker != 0)
		{
			parserError("invalid SJC-annotation for parameter");
			return null;
		}
		if ((t = typeRef(false, true)) == null)
			return null;
		if (!has(SScanner.S_ID))
		{
			parserError("missing identifier");
			return null;
		}
		id = s.nxtSym.strBuf;
		accept();
		p.name = id;
		p.type = t;
		p.type.arrDim += getArrDim();
		return p;
	}
	
	private JMthd methodDecl(int mod, FilledAnno anno, TypeRef retType, boolean inIntf, int syl, int syr)
	{
		String id;
		JMthd m;
		Param p;
		
		if (!has(SScanner.S_ID))
		{
			parserError("missing identifier of constructor or type of method");
			return null;
		}
		id = s.nxtSym.strBuf;
		accept();
		if (!accept(SScanner.S_ENC, SScanner.E_RO))
		{
			parserError("internal: methodDecl called without \"(\"");
			return null;
		}
		m = new JMthd(id, mod, curFID, s.nxtSym.syline, s.nxtSym.sycol);
		m.anno = anno;
		if (retType == null)
			m.isConstructor = true;
		else
			m.retType = retType;
		if (!has(SScanner.S_ENC, SScanner.E_RC))
		{ //there is at least one parameter
			if ((p = getParam()) == null)
				return null;
			p.modifier |= Modifier.MF_ISWRITTEN; //all parameters are already written when the method is called
			m.param = p;
			while (accept(SScanner.S_DEL, SScanner.D_COM))
			{ //comma-separated list
				p.nextParam = getParam();
				if ((p = p.nextParam) == null)
					return null;
				p.modifier |= Modifier.MF_ISWRITTEN; //all parameters are already written when the method is called
			}
		}
		if (!accept(SScanner.S_ENC, SScanner.E_RC))
		{
			parserError("missing \")\"");
			return null;
		}
		if (retType != null)
			retType.arrDim += getArrDim(); //alternative array dimension declaration
		if (accept(SScanner.S_FLC, SScanner.F_THRWS))
		{
			ctx.throwUsed = true;
			if ((m.throwsList = qualIDList()) == null)
				return null;
		}
		if (inIntf || (mod & (SScanner.M_ABSTR | SScanner.M_NAT)) != 0)
		{
			if (!accept(SScanner.S_DEL, SScanner.D_SEM))
			{
				parserError("missing \";\" after abstract or native method declaration");
				return null;
			}
		}
		else if (!fillMethodBlock(m))
			return null;
		return m;
	}
	
	private boolean fillMethodBlock(JMthd m)
	{
		Mthd oldCurMthd;
		
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("missing \"{\" after non-abstract and non-native method declaration");
			return false;
		}
		loopLevel = mthdStmtCnt = 0;
		oldCurMthd = curMthd;
		curMthd = m;
		if ((m.block = stmtBlock(null, null, m.retType != null && m.retType.baseType != StdTypes.T_VOID)) == null)
			return false;
		m.stmtCnt = mthdStmtCnt;
		if (!accept(SScanner.S_ENC, SScanner.E_BC))
		{
			parserError("missing \"}\" after method-block");
			return false;
		}
		curMthd = oldCurMthd;
		return true;
	}
	
	private ExArrayInit arrayInit()
	{
		ExArrayInit init;
		Expr ex;
		FilledParam last = null;
		int syl, syc;
		
		init = new ExArrayInit(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		do
		{
			if (has(SScanner.S_ENC, SScanner.E_BC))
				break; //ignore comma if followed by a closing bracket, support empty array initialization
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if (accept(SScanner.S_ENC, SScanner.E_BO))
			{
				if ((ex = arrayInit()) == null)
					return null;
				if (!accept(SScanner.S_ENC, SScanner.E_BC))
				{
					parserError(MISSING_BC_AFTER_ARRAY_INIT);
					return null;
				}
			}
			else if ((ex = expr()) == null)
				return null;
			if (last == null)
				last = init.par = new FilledParam(ex, curFID, syl, syc);
			else
				last = last.nextParam = new FilledParam(ex, curFID, syl, syc);
		} while (accept(SScanner.S_DEL, SScanner.D_COM));
		return init;
	}
	
	private Vrbl varDecl(Unit owner, int mod, FilledAnno anno, TypeRef varType)
	{
		String id;
		Vrbl first = null, lf = null, now;
		boolean resetCurMthd = false;
		int origArrDim = varType.arrDim;
		ExArrayInit ai;
		
		if (varType.baseType == TypeRef.T_VOID)
		{
			parserError("void not allowed for variables");
			return null;
		}
		if ((mod & SScanner.M_ABSTR) != 0)
		{
			parserError("variables must not be abstract");
			return null;
		}
		do
		{
			if (!has(SScanner.S_ID))
			{
				parserError("missing identifier");
				return null;
			}
			if (first != null)
			{
				varType = varType.copy();
				varType.arrDim = origArrDim;
			}
			id = s.nxtSym.strBuf;
			accept();
			if (anno != null)
				now = new VrblAnno(id, mod, anno, curFID, s.nxtSym.syline, s.nxtSym.sycol);
			else
				now = new Vrbl(id, mod, curFID, s.nxtSym.syline, s.nxtSym.sycol);
			now.owner = owner;
			now.type = varType;
			now.type.arrDim += getArrDim();
			if (accept(SScanner.S_ASN, SScanner.RES))
			{
				if (accept(SScanner.S_ENC, SScanner.E_BO))
				{ //array init
					if ((ai = arrayInit()) == null)
						return null;
					if ((now.modifier & Modifier.M_STAT) == 0)
						now.init = new ExArrayCopy(ai, varType, true); //use copy of constant array
					else
						now.init = ai; //use constant array directly
					if (!accept(SScanner.S_ENC, SScanner.E_BC))
					{
						parserError(MISSING_BC_AFTER_ARRAY_INIT);
						return null;
					}
				}
				else
				{ //normal init
					if (curMthd == null)
					{
						resetCurMthd = true;
						curMthd = (now.modifier & Modifier.M_STAT) != 0 ? owner.initStat : owner.initDyna;
					}
					if ((now.init = expr()) == null)
						return null;
					if (resetCurMthd)
						curMthd = null;
				}
			}
			if (first == null)
				first = now;
			if (lf != null)
				lf.nextVrbl = now;
			lf = now;
		} while (accept(SScanner.S_DEL, SScanner.D_COM));
		return first;
	}
	
	private Intf intfDecl(QualID ip, QualIDList ii, int ia, int il, int ic)
	{
		Intf r;
		
		if (!has(SScanner.S_ID))
		{
			parserError("identifier expected after \"interface\"");
			return null;
		}
		(r = new Intf(ip, ii, ia, curFID, il, ic)).name = s.nxtSym.strBuf;
		accept();
		if (accept(SScanner.S_OKE, SScanner.O_EXTS) && (r.extsImplIDList = qualIDList()) == null)
			return null;
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("missing \"{\" after interface-identifier");
			return null;
		}
		if (!intfFields(r))
			return null;
		if (!accept(SScanner.S_ENC, SScanner.E_BC))
		{
			parserError("missing \"}\" after interface-fields");
			return null;
		}
		return r;
	}
	
	private Anno annoDecl(QualID ip, QualIDList ii, int ia, int il, int ic)
	{
		Anno a;
		int i;
		
		if (!has(SScanner.S_ID))
		{
			parserError("identifier expected after \"interface\"");
			return null;
		}
		(a = new Anno(ip, ii, ia, curFID, il, ic)).name = s.nxtSym.strBuf;
		accept();
		if (!accept(SScanner.S_ENC, SScanner.E_BO))
		{
			parserError("missing \"{\" after annotation-identifier");
			return null;
		}
		//TODO replace code for annotation-handling
		i = 1;
		while (i > 0)
		{
			if (accept(SScanner.S_ENC, SScanner.E_BO))
				i++;
			else if (accept(SScanner.S_ENC, SScanner.E_BC))
				i--;
			else
				accept();
		}
		return a;
	}
	
	private boolean intfFields(Intf c)
	{
		int mod, mark, syl, syc;
		TypeRef trf;
		JMthd nm, lm = null;
		Vrbl nv, lv = null;
		
		while (!has(SScanner.S_ENC, SScanner.E_BC))
		{
			while (accept(SScanner.S_DEL, SScanner.D_SEM)) /* discard empty fields */
				;
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if (!getModifier(true, true))
				return false; //modifiers
			mod = mod_modifier | SScanner.M_PUB; //all fields and methods are public
			mark = mod_marker;
			if (mod_anno != null)
				parserWarning("ignoring SJC-annotation for interface-field");
			if ((mod & SScanner.M_STAT) != 0 && has(SScanner.S_ENC, SScanner.E_BO))
			{
				parserError("static initialisation not allowed in interface");
				return false;
			}
			if ((mod & (SScanner.M_PUB | SScanner.M_STAT | SScanner.M_FIN)) != mod)
			{
				parserError("invalid modifier for interface field");
				return false;
			}
			if (lookAhead(SScanner.S_ENC, SScanner.E_RO))
			{ //constructor, not allowed
				parserError("missing return-type of method (constructor not not allowed in interface)");
				return false;
			}
			else
			{ //vardecl or methoddecl
				if ((trf = typeRef(true, true)) == null)
					return false;
				if (lookAhead(SScanner.S_ENC, SScanner.E_RO))
				{ //methoddecl
					if ((mod & SScanner.M_ABSTR) != 0)
					{
						parserError("methods can not be abstract in interface");
						return false;
					}
					if ((nm = methodDecl(mod, null, trf, true, syl, syc)) == null)
						return false;
					nm.marker = mark;
					if (lm != null)
						lm.nextMthd = nm; //not first methoddecl
					else
						c.mthds = nm; //store first methoddecl
					lm = nm;
				}
				else
				{ //vardecl
					mod |= SScanner.M_PUB | SScanner.M_STAT | SScanner.M_FIN | Modifier.MF_ISWRITTEN; //variables are implicitly public final static and are initialized
					if ((nv = varDecl(c, mod, mod_anno, trf)) == null)
						return false;
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError(MISSING_SEM_AFTER_VRBL);
						return false;
					}
					if (lv != null)
						lv.nextVrbl = nv; //not first vardecl
					else
						c.vars = nv; //store first vardecl
					lv = nv;
					while (lv.nextVrbl != null)
						lv = lv.nextVrbl; //search last vardecl
				}
			}
		}
		return true;
	}
	
	private StBlock stmtBlock(StBreakable outer, StringList labels, boolean addReturnMissingStatement)
	{
		StBlock b;
		Stmt ns, ls = null;
		
		b = new StBlock(outer, labels, curFID, s.nxtSym.syline, s.nxtSym.sycol);
		while (!has(SScanner.S_ENC, SScanner.E_BC))
		{
			if ((ns = stmt(b, false)) == null)
				return null;
			if (ls != null)
				ls.nextStmt = ns;
			else
				b.stmts = ns;
			ls = ns;
		}
		if (addReturnMissingStatement)
		{
			ns = new StRetMissing(curFID, s.nxtSym.syline, s.nxtSym.sycol);
			if (ls != null)
				ls.nextStmt = ns;
			else
				b.stmts = ns;
		}
		return b;
	}
	
	private Stmt stmt(StBreakable outer, boolean noUnreadyExpr)
	{
		int syl, syc, syp;
		StBlock sb;
		StReturn sr;
		StEndLoop sel;
		StIf si;
		StFor sf;
		StForEnh sfe;
		StLoop sl;
		StWhile sw;
		StSwitch ss;
		StTryCaFi st;
		StThrow sh;
		StSync sy;
		StAssert sa;
		Stmt init = null;
		CatchBlock lastCatch = null, thisCatch;
		TypeRef type;
		String singleLabel = null;
		StringList labels = null, tmpStrList;
		
		while (has(SScanner.S_ID) && lookAhead(SScanner.S_DEL, SScanner.D_COL))
		{
			tmpStrList = labels;
			labels = new StringList(s.nxtSym.strBuf);
			labels.next = tmpStrList;
			accept(); //consume label
			accept(); //consume colon
		}
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		syp = s.nxtSym.sypos;
		//empty statement
		if (accept(SScanner.S_DEL, SScanner.D_SEM))
		{
			if (labels != null)
				parserWarning(UNREACHABLE_LABEL);
			return new StEmpty(curFID, syl, syc);
		}
		//update method statement statistic
		mthdStmtCnt++; //do not count empty statement
		//block
		if (accept(SScanner.S_ENC, SScanner.E_BO))
		{
			sb = stmtBlock(outer, labels, false);
			if (!accept(SScanner.S_ENC, SScanner.E_BC))
			{
				parserError("missing \"}\" after block");
				return null;
			}
			return sb;
		}
		//check for flow control
		if (has(SScanner.S_FLC))
			switch (s.nxtSym.par)
			{
				case SScanner.F_IF: //if-else
					accept();
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					si = new StIf(curFID, syl, syc);
					si.srcStart = syp;
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("missing \"(\" in if-statement");
						return null;
					}
					if ((si.cond = expr()) == null)
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing \")\" in if-statement");
						return null;
					}
					if ((si.trStmt = stmt(outer, true)) == null)
						return null;
					if (accept(SScanner.S_FLC, SScanner.F_ELSE))
					{
						if ((si.faStmt = stmt(outer, true)) == null)
							return null;
					}
					si.srcLength = s.endOfLastSymbol - syp;
					return si;
				case SScanner.F_FOR: //for
					accept();
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("missing \"(\" in for-statement");
						return null;
					}
					if (!accept(SScanner.S_DEL, SScanner.D_SEM) && (init = exprVarDecl(true)) == null)
						return null;
					if (init != null && accept(SScanner.S_DEL, SScanner.D_COL))
					{ //enhanced for of jdk 1.5, declaration is checked in exprVarDecl already
						sl = sfe = new StForEnh(outer, labels, curFID, syl, syc);
						sfe.srcStart = syp;
						sfe.var = (StVrbl) init;
						if ((sfe.iter = expr()) == null)
							return null;
					}
					else
					{ //classic for
						sl = sf = new StFor(outer, labels, curFID, syl, syc);
						sf.srcStart = syp;
						sf.init = init;
						if (!has(SScanner.S_DEL, SScanner.D_SEM) && (sf.cond = expr()) == null)
							return null;
						if (!accept(SScanner.S_DEL, SScanner.D_SEM))
						{
							parserError("missing \";\" after condition in for-statement");
							return null;
						}
						if (!has(SScanner.S_ENC, SScanner.E_RC) && (sf.lupd = exprList()) == null)
							return null;
					}
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing \")\" in for-statement");
						return null;
					}
					loopLevel++;
					if ((sl.loStmt = stmt(sl, true)) == null)
						return null;
					loopLevel--;
					sl.srcLength = s.endOfLastSymbol - syp;
					return sl;
				case SScanner.F_WHILE: //while
					accept();
					sw = new StWhile(outer, labels, curFID, syl, syc); //default is exclusive while
					sw.srcStart = syp;
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("missing \"(\" in while-statement");
						return null;
					}
					if ((sw.cond = expr()) == null)
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing \")\" in while-statement");
						return null;
					}
					loopLevel++;
					if ((sw.loStmt = stmt(sw, true)) == null)
						return null;
					loopLevel--;
					sw.srcLength = s.endOfLastSymbol - syp;
					return sw;
				case SScanner.F_RET: //return
					accept();
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					sr = new StReturn(outer, curFID, syl, syc);
					sr.srcStart = syp;
					if (!has(SScanner.S_DEL, SScanner.D_SEM))
					{
						if ((sr.retVal = expr()) == null)
							return null;
					}
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after return");
						return null;
					}
					sr.srcLength = s.endOfLastSymbol - syp;
					return sr;
				case SScanner.F_BRK: //break
					accept();
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					if (has(SScanner.S_ID))
					{
						singleLabel = s.nxtSym.strBuf;
						accept();
					}
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after break");
						return null;
					}
					sel = new StEndLoop(outer, singleLabel, curFID, syl, syc); //already initialized: .contNotBreak=false;
					sel.srcStart = syp;
					sel.srcLength = s.endOfLastSymbol - syp;
					return sel;
				case SScanner.F_CNT: //continue
					accept();
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					if (has(SScanner.S_ID))
					{
						singleLabel = s.nxtSym.strBuf;
						accept();
					}
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after continue");
						return null;
					}
					(sel = new StEndLoop(outer, singleLabel, curFID, syl, syc)).contNotBreak = true;
					sel.srcStart = syp;
					sel.srcLength = s.endOfLastSymbol - syp;
					return sel;
				case SScanner.F_DO: //do-while
					accept();
					(sw = new StWhile(outer, labels, curFID, syl, syc)).inclusiveWhile = true;
					sw.srcStart = syp;
					loopLevel++;
					if ((sw.loStmt = stmt(sw, true)) == null)
						return null;
					loopLevel--;
					if (!accept(SScanner.S_FLC, SScanner.F_WHILE))
					{
						parserError("missing \"while\" after do-statement");
					}
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("missing \"(\" in do-while-statement");
						return null;
					}
					if ((sw.cond = expr()) == null)
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing \")\" in do-while-statement");
						return null;
					}
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after do-while-statement");
						return null;
					}
					sw.srcLength = s.nxtSym.sypos - sw.srcStart;
					return sw;
				case SScanner.F_SWTCH: //switch
					accept();
					ss = new StSwitch(outer, labels, curFID, syl, syc);
					ss.srcStart = syp;
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("missing \"(\" in switch-statement");
						return null;
					}
					if ((ss.cond = expr()) == null)
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing \")\" in switch-statement");
						return null;
					}
					if (!accept(SScanner.S_ENC, SScanner.E_BO))
					{
						parserError("missing \"{\" in switch-statement");
						return null;
					}
					if (!switchList(ss))
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_BC))
					{
						parserError("missing \"}\" in switch-statement");
						return null;
					}
					ss.srcLength = s.endOfLastSymbol - syp;
					return ss;
				case SScanner.F_TRY: //try-catch-finally
					accept();
					ctx.throwUsed = true;
					if (!accept(SScanner.S_ENC, SScanner.E_BO))
					{
						parserError("missing \"{\" in try-statement");
						return null;
					}
					st = new StTryCaFi(outer, labels, curFID, syl, syc);
					st.srcStart = syp;
					if ((st.tryBlock = stmtBlock(st, null, false)) == null)
						return null;
					if (!accept(SScanner.S_ENC, SScanner.E_BC))
					{
						parserError("missing \"}\" in try-statement");
						return null;
					}
					syl = s.nxtSym.syline;
					syc = s.nxtSym.sycol;
					while (accept(SScanner.S_FLC, SScanner.F_CATCH))
					{
						if (!accept(SScanner.S_ENC, SScanner.E_RO))
						{
							parserError("missing \"(\" in catch-statement");
							return null;
						}
						thisCatch = new CatchBlock(curFID, syl, syc);
						if ((type = typeRef(false, false)) == null)
							return null;
						if (!has(SScanner.S_ID))
						{
							parserError("missing identifier for exception in catch-block");
							return null;
						}
						thisCatch.catchVar = new Vrbl(s.nxtSym.strBuf, 0, curFID, s.nxtSym.syline, s.nxtSym.sycol);
						accept();
						thisCatch.catchVar.type = type;
						if (!accept(SScanner.S_ENC, SScanner.E_RC))
						{
							parserError("missing \")\" in catch-statement");
							return null;
						}
						if (!accept(SScanner.S_ENC, SScanner.E_BO))
						{
							parserError("missing \"{\" in catch-block");
							return null;
						}
						if ((thisCatch.stmts = stmtBlock(st, null, false)) == null)
							return null;
						if (!accept(SScanner.S_ENC, SScanner.E_BC))
						{
							parserError("missing \"}\" in catch-block");
							return null;
						}
						if (lastCatch == null)
							st.catchBlocks = thisCatch;
						else
							lastCatch.nextCatchDecl = thisCatch;
						lastCatch = thisCatch;
						syl = s.nxtSym.syline;
						syc = s.nxtSym.sycol;
					}
					if (accept(SScanner.S_FLC, SScanner.F_FIN))
					{
						if (!accept(SScanner.S_ENC, SScanner.E_BO))
						{
							parserError("missing \"{\" in finally-block");
							return null;
						}
						if ((st.finallyBlock = stmtBlock(outer, labels, false)) == null)
							return null;
						if (!accept(SScanner.S_ENC, SScanner.E_BC))
						{
							parserError("missing \"}\" in finally-block");
							return null;
						}
					}
					st.srcLength = s.endOfLastSymbol - syp;
					return st;
				case SScanner.F_THROW: //throw
					accept();
					ctx.throwUsed = true;
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					sh = new StThrow(curFID, syl, syc);
					sh.srcStart = syp;
					if ((sh.throwVal = expr()) == null)
						return null;
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after throw");
						return null;
					}
					sh.srcLength = s.endOfLastSymbol - syp;
					return sh;
				case SScanner.F_ASSRT:
					accept();
					if (labels != null)
						parserWarning(UNREACHABLE_LABEL);
					sa = new StAssert(curFID, syl, syc);
					sa.srcStart = syp;
					if ((sa.cond = expr()) == null)
						return null;
					if (accept(SScanner.S_DEL, SScanner.D_COL) && (sa.msg = expr()) == null)
						return null;
					if (!accept(SScanner.S_DEL, SScanner.D_SEM))
					{
						parserError("missing \";\" after assert");
						return null;
					}
					sa.srcLength = s.endOfLastSymbol - syp;
					return sa;
			}
		//synchronized
		if (accept(SScanner.S_MOD, SScanner.M_SYNC))
		{
			ctx.syncUsed = true;
			sy = new StSync(outer, StSync.SYNC_NORM, curFID, syl, syc);
			sy.srcStart = syp;
			if (!accept(SScanner.S_ENC, SScanner.E_RO))
			{
				parserError("missing \"(\" in synchronized-block");
				return null;
			}
			if ((sy.syncObj = expr()) == null)
				return null;
			if (!accept(SScanner.S_ENC, SScanner.E_RC))
			{
				parserError("missing \")\" in synchronized-block");
				return null;
			}
			if (!accept(SScanner.S_ENC, SScanner.E_BO))
			{
				parserError("missing \"{\" in synchronized-block");
				return null;
			}
			if ((sy.syncBlock = stmtBlock(sy, labels, false)) == null)
				return null;
			if (!accept(SScanner.S_ENC, SScanner.E_BC))
			{
				parserError("missing \"}\" after synchronized-block");
				return null;
			}
			sy.srcLength = s.endOfLastSymbol - syp;
			return sy;
		}
		//expression
		if (labels != null)
			parserWarning(UNREACHABLE_LABEL);
		return exprVarDecl(false);
	}
	
	private Stmt exprVarDecl(boolean allowSpecialsOfForLoop)
	{
		Expr ex;
		StExpr sse;
		TypeRef vt;
		Vrbl nv;
		StVrbl sv;
		int syl, syc, syp, mod, i;
		TypeRef type;
		
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		syp = s.nxtSym.sypos;
		//do some trivial checks to see if variable is declared
		if (has(SScanner.S_MOD, SScanner.M_ANNO) || has(SScanner.S_MOD, SScanner.M_FIN) || (has(SScanner.S_ID) || has(SScanner.S_TYP)) && (lookAhead(SScanner.S_ENC, SScanner.E_SOC) || lookAhead(SScanner.S_ID)))
		{
			//declaration of variable
			mod = 0;
			while (accept(SScanner.S_MOD, SScanner.M_ANNO))
			{ //read in annotation of local variables
				if ((type = typeRef(false, false)) == null)
				{
					parserError("annotation type expected");
					return null;
				}
				if (type.qid.name.str.equals("SJC"))
				{ //SJC-annotation
					if (type.qid.name.next != null && type.qid.name.next.str.equals("Flash"))
					{
						mod |= Modifier.MM_FLASH;
					}
					else if (type.qid.name.next != null && type.qid.name.next.str.equals("RefToFlash"))
					{
						mod |= Modifier.MM_REFTOFLASH;
					}
					else
					{
						parserError("only SJC.Flash-/RefToFlash-annotation or non-SJC-annotation allowed here");
						return null;
					}
				}
				else if (accept(SScanner.S_ENC, SScanner.E_RO))
				{ //skip over non-SJC-annotation
					i = 1;
					while (i > 0)
					{
						if (accept(SScanner.S_ENC, SScanner.E_RO))
							i++;
						else if (accept(SScanner.S_ENC, SScanner.E_RC))
							i--;
						else
							accept();
					}
				}
			}
			if (accept(SScanner.S_MOD, SScanner.M_FIN))
				mod |= SScanner.M_FIN;
			if ((vt = typeRef(false, true)) == null)
				return null;
			if ((nv = varDecl(null, mod, null, vt)) == null)
				return null;
			sv = new StVrbl(nv, loopLevel != 0);
			sv.srcStart = syp;
			sv.srcLength = s.endOfLastSymbol - syp;
			if (allowSpecialsOfForLoop && has(SScanner.S_DEL, SScanner.D_COL))
			{ //enhanced for loop
				if (nv.nextVrbl != null || nv.init != null)
				{
					parserError("invalid declaration in enhanced for loop");
					return null;
				}
			}
			else if (!accept(SScanner.S_DEL, SScanner.D_SEM))
			{ //normal variable declaration
				parserError(MISSING_SEM_AFTER_VRBL);
				return null;
			}
			return sv;
		}
		//no variable, normal expression
		if ((ex = expr()) == null)
			return null;
		if (!has(SScanner.S_DEL, SScanner.D_SEM) && (!allowSpecialsOfForLoop || !has(SScanner.S_DEL, SScanner.D_COM)))
		{ //varDecl not detected by variable-check above
			if ((vt = getTypeRefOfStEx(ex)) == null)
				return null;
			if ((nv = varDecl(null, 0, null, vt)) == null)
				return null;
			sv = new StVrbl(nv, loopLevel != 0);
			sv.srcStart = syp;
			if (!accept(SScanner.S_DEL, SScanner.D_SEM) && !(allowSpecialsOfForLoop && has(SScanner.S_DEL, SScanner.D_COL)))
			{
				parserError(MISSING_SEM_AFTER_VRBL);
				return null;
			}
			sv.srcLength = s.endOfLastSymbol - syp;
			return sv;
		}
		sse = new StExpr(curFID, syl, syc);
		sse.ex = ex;
		sse.srcStart = syp;
		if (allowSpecialsOfForLoop && accept(SScanner.S_DEL, SScanner.D_COM) && (sse.nextStmt = exprList()) == null)
			return null;
		if (!accept(SScanner.S_DEL, SScanner.D_SEM))
		{
			parserError("missing \";\" after expression-statement");
			return null;
		}
		sse.srcLength = s.endOfLastSymbol - syp;
		return sse;
	}
	
	private TypeRef getTypeRefOfStEx(Expr ex)
	{
		StringList qid = null, lqid = null;
		ExDeRef ed;
		ExVar ev;
		TypeRef tr;
		
		while (ex instanceof ExDeRef)
		{
			ed = (ExDeRef) ex;
			if (ed.ri instanceof ExVar)
			{
				ev = (ExVar) ed.ri;
				if (ev.id == null)
				{
					parserError("invalid typedecl or missing \";\" after expression-statement");
					return null;
				}
				if (qid == null)
				{
					qid = new StringList(ev.id);
					lqid = qid;
				}
				else
				{
					lqid = new StringList(ev.id);
					lqid.next = qid;
					qid = lqid;
				}
			}
			else
			{
				parserError("invalid typedecl or missing \";\" after expression-statement");
				return null;
			}
			ex = ed.le;
		}
		if (ex instanceof ExVar)
		{
			ev = (ExVar) ex;
			if (ev.id == null)
			{
				parserError("invalid typedecl or missing \";\" after expression-statement");
				return null;
			}
			if (qid == null)
			{
				qid = new StringList(ev.id);
				lqid = qid;
			}
			else
			{
				lqid = new StringList(ev.id);
				lqid.next = qid;
				qid = lqid;
			}
		}
		else
		{
			parserError("invalid typedecl or missing \";\" after expression-statement");
			return null;
		}
		(tr = new TypeRef(ex.fileID, ex.line, ex.col)).baseType = TypeRef.T_QID;
		tr.qid = new QualID(qid, QualID.Q_UNIT, ex.fileID, ex.line, ex.col);
		tr.arrDim = getArrDim();
		return tr;
	}
	
	private boolean switchList(StSwitch ss)
	{
		CondStmt ncs, lcs = null, lhcs = null;
		Stmt ns, ls = null;
		boolean defDone = false;
		int syl, syc;
		
		while (!has(SScanner.S_ENC, SScanner.E_BC))
		{
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if (accept(SScanner.S_FLC, SScanner.F_CASE))
			{
				ncs = new CondStmt(curFID, syl, syc);
				if ((ncs.cond = expr()) == null)
					return false;
				if (!accept(SScanner.S_DEL, SScanner.D_COL))
				{
					parserError("missing \":\" after case");
					return false;
				}
				if (lcs != null)
					lcs.nextCondStmt = ncs; //enter in list, if not first condition
				else
					ss.caseConds = ncs; //store first condition
				lcs = ncs;
				if (lhcs == null)
					lhcs = lcs;
				lcs.stmt = new StEmpty(curFID, syl, syc);
				if (ls != null)
					ls.nextStmt = lcs.stmt; //enter in list if not first statement
				else
					ss.stmts = lcs.stmt; //store first statement
				ls = lcs.stmt;
			}
			else if (accept(SScanner.S_FLC, SScanner.F_DFLT))
			{
				if (defDone)
				{
					parserError("more than one default-block");
					return false;
				}
				if (!accept(SScanner.S_DEL, SScanner.D_COL))
				{
					parserError("missing \":\" after default");
					return false;
				}
				defDone = true;
				ss.def = new StEmpty(curFID, syl, syc);
				if (ls != null)
					ls.nextStmt = ss.def; //enter in list if not first statement
				else
					ss.stmts = ss.def; //store first statement
				ls = ss.def;
			}
			else
			{
				if (lcs == null && !defDone)
				{
					parserError("case expected before statements in switch-list");
					return false;
				}
				if ((ns = stmt(ss, true)) == null)
				{
					parserError("statement expected in switch-list");
					return false;
				}
				if (ls != null)
					ls.nextStmt = ns; //enter in list if not first statement
				else
					ss.stmts = ns; //store first statement
				ls = ns;
			}
		}
		return true;
	}
	
	private StExpr exprList()
	{
		StExpr ret = null, se;
		
		ret = se = new StExpr(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		if ((ret.ex = expr()) == null)
			return null;
		se.srcStart = se.ex.srcStart;
		se.srcLength = se.ex.srcLength;
		while (accept(SScanner.S_DEL, SScanner.D_COM))
		{
			se.nextStmt = se = new StExpr(curFID, s.nxtSym.syline, s.nxtSym.sycol);
			if ((se.ex = expr()) == null)
				return null;
			se.srcStart = se.ex.srcStart;
			se.srcLength = se.ex.srcLength;
		}
		return ret;
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
	
	private boolean lookAhead(int t)
	{
		return s.lahSym.type == t;
	}
	
	private boolean lookAhead(int t, int p)
	{
		return s.lahSym.type == t && s.lahSym.par == p;
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
	
	private void parserWarning(String msg)
	{
		ctx.printPos(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		ctx.out.print(": parser-warning: ");
		ctx.out.println(msg);
	}
	
	//---###--- expressions ---###---
	
	private FilledParam getCallParam()
	{
		int syl, syc;
		Expr tmpEx;
		FilledParam ret, last;
		
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		if ((tmpEx = expr()) == null)
		{
			parserError("invalid expression in call");
			return null;
		}
		last = ret = new FilledParam(tmpEx, curFID, syl, syc);
		while (has(SScanner.S_DEL, SScanner.D_COM))
		{
			accept();
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			if ((tmpEx = expr()) == null)
			{
				parserError("parameter expected in call after \",\"");
				return null;
			}
			last.nextParam = new FilledParam(tmpEx, curFID, syl, syc);
			last = last.nextParam;
		}
		return ret;
	}
	
	private boolean getNewArrayParam(ExNew nob)
	{
		int syl, syc, syp, cnt;
		boolean needExpr = true, noExprAllowed = false;
		Expr tmpEx;
		FilledParam fields, last;
		
		if (!accept(SScanner.S_ENC, SScanner.E_SO))
		{
			parserError("expected \"[\" in array-expression");
			return false;
		}
		cnt = 1;
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		syp = s.nxtSym.sypos;
		if ((tmpEx = expr()) == null)
		{
			parserError("invalid expression in array-expression");
			return false;
		}
		if (!accept(SScanner.S_ENC, SScanner.E_SC))
		{
			parserError("expected \"]\" in array-expression");
			return false;
		}
		last = fields = new FilledParam(tmpEx, curFID, syl, syc);
		last.srcStart = syp;
		last.srcLength = s.endOfLastSymbol - syp;
		while (has(SScanner.S_ENC, SScanner.E_SO))
		{
			accept();
			cnt++;
			syl = s.nxtSym.syline;
			syc = s.nxtSym.sycol;
			syp = s.nxtSym.sypos;
			tmpEx = expr();
			if (needExpr)
			{
				if (tmpEx == null)
				{
					parserError("parameter expected in array-expression after \"[\"");
					return false;
				}
				needExpr = false;
			}
			else if (noExprAllowed)
			{
				if (tmpEx != null)
				{
					parserError("parameter not allowed in array-expression after null-expr");
					return false;
				}
			}
			if (tmpEx == null)
				noExprAllowed = true;
			if (!accept(SScanner.S_ENC, SScanner.E_SC))
			{
				parserError("expected \"]\" in array-expression");
				return false;
			}
			//expression ok, enter
			last.nextParam = new FilledParam(tmpEx, curFID, syl, syc);
			last = last.nextParam;
			last.srcStart = syp;
			last.srcLength = s.endOfLastSymbol - syp;
		}
		nob.par = fields; //enter parameters
		nob.obj.arrDim = cnt + getArrDim(); //perhaps the user wants to create arrays of empty arrays
		return true;
	}
	
	private String intToHex(int id)
	{
		int digits = 1, i, v;
		char[] buf;
		for (i = 1; i < 8; i++)
			if ((id & (0xF << (i << 2))) != 0)
				digits = i + 1;
		buf = new char[digits];
		for (i = 1; i <= digits; i++)
		{
			buf[digits - i] = ((v = id & 0xF) < 10) ? (char) (v + 48) : (char) (v + 55);
			id = id >>> 4;
		}
		return new String(buf);
	}
	
	private Expr getIDFragment()
	{
		int syl, syc, mod;
		ExCall call;
		ExVar var;
		ExNew nob;
		TypeRef nobType;
		ExArrayInit ai;
		String id;
		Clss anonymousInner;
		Unit tmpUnit;
		int counter = 0;
		
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		if (accept(SScanner.S_OKE, SScanner.O_NEW))
		{
			if ((nobType = typeRef(false, true)) == null)
			{
				parserError("error in type of new-expression");
				return null;
			}
			if (nobType.arrDim > 0)
			{ //explicitly initialized array => no "classic" new, instead use array-copy
				if (!accept(SScanner.S_ENC, SScanner.E_BO))
				{
					parserError("expected \"{\" before array init in new-expression");
					return null;
				}
				if ((ai = arrayInit()) == null)
					return null;
				if (!accept(SScanner.S_ENC, SScanner.E_BC))
				{
					parserError("expected \"}\" after array init in new-expression");
					return null;
				}
				return new ExArrayCopy(ai, nobType, false);
			}
			else
			{ //not explicitly initialized array
				nob = new ExNew(curFID, syl, syc);
				nob.obj = nobType;
				if (has(SScanner.S_ENC, SScanner.E_SO))
				{ //opening array-declaration
					nob.asArray = true;
					if (!getNewArrayParam(nob))
						return null;
				}
				else
				{ //no array, must be constructor
					nob.asArray = false;
					if (nob.obj.baseType != TypeRef.T_QID)
					{
						parserError("object of basis-type not allowed");
						return null;
					}
					if (!accept(SScanner.S_ENC, SScanner.E_RO))
					{
						parserError("expected \"(\" in new-expression");
						return null;
					}
					if (!has(SScanner.S_ENC, SScanner.E_RC) && (nob.par = getCallParam()) == null)
						return null; //get parameter
					if (!accept(SScanner.S_ENC, SScanner.E_RC))
					{
						parserError("missing closing \")\" in new-expresseion");
						return null;
					}
					if (accept(SScanner.S_ENC, SScanner.E_BO))
					{ //anonymous inner class
						mod = SScanner.M_FIN | SScanner.M_PRIV;
						if ((curMthd.modifier & Modifier.M_STAT) != 0)
							mod |= SScanner.M_STAT;
						anonymousInner = new Clss(curClass.pack, curClass.impt, mod, curClass.marker, curFID, syl, syc);
						if (!clssFields(anonymousInner))
							return null;
						if (!accept(SScanner.S_ENC, SScanner.E_BC))
						{
							parserError("missing \"}\" after anonymous inner class");
							return null;
						}
						//parsing of inner anonymous class ok, reorganize new-statement and insert inner class
						tmpUnit = curClass.innerUnits;
						while (tmpUnit != null)
						{
							counter++;
							tmpUnit = tmpUnit.nextUnit;
						}
						anonymousInner.extsID = nob.obj.qid; //depending on type this may be "implements" instead of "extents"
						nob.obj.qid = new QualID(new StringList(anonymousInner.name = "$anon".concat(intToHex(counter))), QualID.Q_UNIT, curFID, syl, syc);
						insertInnerUnit(curClass, anonymousInner);
					}
				}
				return nob;
			}
		}
		if ((id = ident()) == null)
		{
			parserError("identifier expected in expression (getIDFragement)");
			return null;
		}
		if (has(SScanner.S_ENC, SScanner.E_RO))
		{ //method-call
			accept();
			call = new ExCall(curFID, syl, syc);
			call.id = id;
			if (!has(SScanner.S_ENC, SScanner.E_RC))
			{
				call.par = getCallParam();
				if (call.par == null)
					return null;
			}
			if (!accept(SScanner.S_ENC, SScanner.E_RC))
			{
				parserError("expected \")\" after method-call");
				return null;
			}
			return call;
		}
		//variable
		var = new ExVar(id, curFID, syl, syc);
		return var;
	}
	
	private TypeRef getTypeRefOfExpr(Expr ex)
	{
		StringList list = null, last = null;
		Expr checkVar = null;
		ExDeRef deref;
		ExVar var;
		TypeRef ret;
		boolean goOn = true;
		
		while (goOn)
		{ //if goOn==false, we have a valid TypeRef-identifier in list
			if (ex instanceof ExDeRef)
			{
				deref = (ExDeRef) ex;
				ex = deref.le;
				checkVar = deref.ri;
			}
			else
			{
				checkVar = ex;
				goOn = false;
			}
			if (checkVar instanceof ExVar)
			{
				var = (ExVar) checkVar;
				if (list == null)
				{
					last = new StringList(var.id);
					list = last;
				}
				else
				{
					last = new StringList(var.id);
					last.next = list;
					list = last;
				}
			}
			else
			{
				parserError("invalid type");
				return null;
			}
		}
		//we have a valid StringList, convert it to a TypeRef with JQualID
		(ret = new TypeRef(curFID, checkVar.line, checkVar.col)).baseType = TypeRef.T_QID; //init with position of first token
		ret.qid = new QualID(list, QualID.Q_UNIT, curFID, checkVar.line, checkVar.col);
		return ret;
	}
	
	private Expr getOperandFragment(boolean acceptFurtherFragments)
	{
		int syl, syc;
		ExEnc enc;
		ExVal num;
		Expr tmpEx;
		
		if (has(SScanner.S_OKE, SScanner.O_CLSS) || has(SScanner.S_OKE, SScanner.O_INTF) || has(SScanner.S_OKE, SScanner.O_ANDC))
		{
			parserError("named local units inside methods not supported");
			return null;
		}
		
		syl = s.nxtSym.syline;
		syc = s.nxtSym.sycol;
		if (accept(SScanner.S_ENC, SScanner.E_RO))
		{
			enc = new ExEnc(curFID, syl, syc);
			if (has(SScanner.S_TYP))
			{ //check if next token is standard-type
				if (!acceptFurtherFragments)
				{
					parserError("standard-type-conversion in right enclosure");
					return null;
				}
				enc.convertTo = new TypeRef(curFID, syl, syc);
				enc.convertTo.baseType = s.nxtSym.par;
				accept();
				enc.convertTo.arrDim = getArrDim();
				if (!accept(SScanner.S_ENC, SScanner.E_RC))
				{
					parserError("missing \")\" in conversion");
					return null;
				}
				if ((enc.ex = getOperand()) == null)
				{
					parserError("expecteded operand after conversion");
					return null;
				}
				return enc;
			}
			if ((enc.ex = expr()) == null)
				return null;
			if (has(SScanner.S_ENC, SScanner.E_SOC))
			{
				//check if enclosed expression is realy just a type and an expr follows
				if ((enc.convertTo = getTypeRefOfExpr(enc.ex)) == null)
					return null;
				enc.convertTo.arrDim = getArrDim();
				if (!accept(SScanner.S_ENC, SScanner.E_RC))
				{
					parserError("missing \")\" after conversion to array");
					return null;
				}
				if ((enc.ex = getOperandFragment(false)) == null)
				{
					parserError("expecteded expression after conversion to array");
					return null;
				}
			}
			else
			{
				if (!accept(SScanner.S_ENC, SScanner.E_RC))
				{
					parserError("missing \")\" in enclosed expression");
					return null;
				}
				if (has(SScanner.S_DEL, SScanner.D_DOT) || has(SScanner.S_ENC, SScanner.E_SO)) //not a conversion but encapsulated dereferenzation
					return appendDeRefArray(enc);
				if (acceptFurtherFragments && (tmpEx = getOperandFragment(false)) != null)
				{ //conversion
					//check if enclosed expression is realy just a type
					enc.convertTo = getTypeRefOfExpr(enc.ex);
					if (enc.convertTo == null)
						return null;
					enc.ex = tmpEx;
				}
			}
			return enc;
		}
		if (has(SScanner.S_NUM))
		{
			num = new ExVal(curFID, syl, syc);
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
		if (has(SScanner.S_SCT))
		{
			tmpEx = new ExStr(s.nxtSym.strBuf, curFID, syl, syc);
			accept();
			if (has(SScanner.S_DEL, SScanner.D_DOT)) //constant string may be dereferenced directly
				return appendDeRefArray(tmpEx);
			return tmpEx;
		}
		if (has(SScanner.S_ID) || has(SScanner.S_OKE, SScanner.O_NEW))
		{
			tmpEx = getIDFragment();
			if (has(SScanner.S_DEL, SScanner.D_DOT) || has(SScanner.S_ENC, SScanner.E_SO))
				return appendDeRefArray(tmpEx);
			return tmpEx;
		}
		if (acceptFurtherFragments)
			parserError("operand expected in expression");
		return null;
	}
	
	private Expr appendDeRefArray(Expr res)
	{
		ExDeRef deref;
		ExDeArray array;
		boolean ref;
		
		while ((ref = has(SScanner.S_DEL, SScanner.D_DOT)) || has(SScanner.S_ENC, SScanner.E_SO))
		{
			if (ref)
			{ //DeRef
				deref = new ExDeRef(curFID, s.nxtSym.syline, s.nxtSym.sycol);
				deref.srcStart = s.nxtSym.sypos;
				accept();
				deref.le = res;
				if ((deref.ri = getIDFragment()) == null)
					return null;
				res = deref;
				deref.srcLength = s.nxtSym.sypos - deref.srcStart;
			}
			else
			{ //DeArray
				array = new ExDeArray(curFID, s.nxtSym.syline, s.nxtSym.sycol);
				array.srcStart = s.nxtSym.sypos;
				accept();
				array.le = res;
				if ((array.ind = expr()) == null)
					return null;
				if (!accept(SScanner.S_ENC, SScanner.E_SC))
				{
					parserError("missing \"]\" in array expression");
					return null;
				}
				res = array;
				array.srcLength = s.nxtSym.sypos - array.srcStart;
			}
		}
		return res;
	}
	
	private Expr getOperand()
	{
		int op;
		Expr ret;
		ExUna nun;
		
		//unary operator
		if ((op = getUnaOperator()) != 0)
		{
			nun = new ExUna(op, curFID, s.nxtSym.syline, s.nxtSym.sycol);
			nun.srcStart = s.nxtSym.sypos;
			if ((nun.ex = getOperand()) == null)
				return null;
			nun.srcLength = s.nxtSym.sypos - nun.srcStart;
			return nun;
		}
		//get operand
		op = getPrePstOperator();
		ret = getOperandFragment(true);
		if (ret == null)
			return null;
		if (op != 0)
		{ //there is a pre-operator
			ret = new ExPrePst(ret, op, true, ret.fileID, ret.line, ret.col);
			if (getPrePstOperator() != 0)
			{
				parserError("prefix and postfix operator can not be combined");
				return null;
			}
		}
		else
		{ //no pre-operator, check if post-operator
			op = getPrePstOperator();
			if (op != 0)
				ret = new ExPrePst(ret, op, false, ret.fileID, ret.line, ret.col);
		}
		return ret;
	}
	
	private Expr getOperandClass()
	{
		ExClssMthdName ncl;
		
		ncl = new ExClssMthdName(curFID, s.nxtSym.syline, s.nxtSym.sycol);
		ncl.srcStart = s.nxtSym.sypos;
		if ((ncl.destType = typeRef(false, true)) == null)
		{
			parserError("error in type of class name for expression");
			return null;
		}
		ncl.srcLength = ncl.srcStart - s.nxtSym.sypos;
		return ncl;
	}
	
	private int getBinOperator()
	{
		int i;
		
		if (has(SScanner.S_ASN) || has(SScanner.S_ASK) || has(SScanner.S_CMP) || has(SScanner.S_BSH) || has(SScanner.S_ASNARI) || has(SScanner.S_ASNBSH) || (has(SScanner.S_LOG) && !has(SScanner.S_LOG, SScanner.L_NOT)) || (has(SScanner.S_ARI) && !has(SScanner.S_ARI, SScanner.A_CPL)))
		{
			i = (s.nxtSym.type << 16) | (s.nxtSym.par);
			accept();
			return i;
		}
		return 0;
	}
	
	private int getUnaOperator()
	{
		int i;
		
		if (has(SScanner.S_LOG, SScanner.L_NOT) || has(SScanner.S_ARI, SScanner.A_CPL) || has(SScanner.S_ARI, SScanner.A_PLUS) || has(SScanner.S_ARI, SScanner.A_MINUS))
		{
			i = (s.nxtSym.type << 16) | (s.nxtSym.par);
			accept();
			return i;
		}
		return 0;
	}
	
	private int getPrePstOperator()
	{
		int i;
		
		if (has(SScanner.S_PFX))
		{
			i = (s.nxtSym.type << 16) | (s.nxtSym.par);
			accept();
			return i;
		}
		return 0;
	}
	
	private int getRank(int type, int par)
	{
		switch (type)
		{
			case SScanner.S_ASN:
			case SScanner.S_ASNARI:
			case SScanner.S_ASNBSH:
				return 1;
			case SScanner.S_ASK:
				return 2;
			case SScanner.S_ARI:
				switch (par)
				{
					case SScanner.A_OR:
						return 5;
					case SScanner.A_XOR:
						return 6;
					case SScanner.A_AND:
						return 7;
					case SScanner.A_PLUS:
					case SScanner.A_MINUS:
						return 11;
					case SScanner.A_MUL:
					case SScanner.A_DIV:
					case SScanner.A_MOD:
						return 12;
				}
				return 0;
			case SScanner.S_CMP:
				switch (par)
				{
					case SScanner.C_EQ:
					case SScanner.C_NE:
						return 8;
					case SScanner.C_LW:
					case SScanner.C_LE:
					case SScanner.C_GE:
					case SScanner.C_GT:
						return 9;
					case SScanner.C_INOF:
						return 10;
				}
				return 0;
			case SScanner.S_LOG:
				switch (par)
				{
					case SScanner.L_OR:
						return 3;
					case SScanner.L_AND:
						return 4;
				}
				return 0;
			case SScanner.S_BSH:
				return 10;
		}
		//invalid operator
		return 0;
	}
	
	private boolean lowerPrio(int ra1, int ra2)
	{
		if (ra1 == 2)
			return true; //?: is right associative on right side
		if (ra1 != 1 || ra2 != 1)
			return ra1 < ra2;
		return true; //assign is right-associative
	}
	
	private Expr expr()
	{
		int op, opType, opPar, rank, syp;
		Expr ret;
		ExBin bin, tmpBn;
		ExChoose chs;
		
		syp = s.nxtSym.sypos;
		if ((ret = getOperand()) == null)
			return null;
		while ((op = getBinOperator()) != 0)
		{
			if (op != 0)
			{
				opType = op >>> 16;
				opPar = op & 0xFFFF;
				rank = getRank(opType, opPar);
				if (opType == SScanner.S_ASK)
				{
					chs = new ExChoose(op, rank, curFID, s.nxtSym.syline, s.nxtSym.sycol);
					if ((chs.ce = expr()) == null)
						return null;
					if (!accept(SScanner.S_DEL, SScanner.D_COL))
					{
						parserError("missing colon in choose expression");
						return null;
					}
					bin = chs;
				}
				else
					bin = new ExBin(op, rank, curFID, s.nxtSym.syline, s.nxtSym.sycol);
				if (opType == SScanner.S_CMP && opPar == SScanner.C_INOF)
					bin.ri = getOperandClass();
				else
					bin.ri = getOperand();
				if (bin.ri == null)
				{
					parserError("missing operand after operator in expression");
					return null;
				}
				//pay attention to ranking
				if (ret instanceof ExBin)
				{
					tmpBn = (ExBin) ret;
					if (lowerPrio(tmpBn.rank, rank))
					{
						while ((tmpBn.ri instanceof ExBin) && lowerPrio(((ExBin) tmpBn.ri).rank, rank))
							tmpBn = (ExBin) tmpBn.ri;
						bin.le = tmpBn.ri;
						tmpBn.ri = bin;
					}
					else
					{ //lowest priority so far
						bin.le = ret;
						ret = bin;
					}
				}
				else
				{ //single operand so far
					bin.le = ret;
					ret = bin;
				}
			}
		}
		//expression complete
		ret.srcStart = syp;
		ret.srcLength = s.nxtSym.sypos - syp;
		return ret;
	}
}
