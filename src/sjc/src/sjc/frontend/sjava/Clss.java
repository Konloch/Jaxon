/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2015, 2019 Stefan Frenz
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
import sjc.debug.DebugWriter;

/**
 * Clss: class-dependent part of java-units
 *
 * @author S. Frenz
 * @version 190417 added support for explicit null-initialization for non-final static struct
 * version 190322 added const struct scalar handling to support non-final static struct initialization
 * version 151108 added allocation debug hint
 * version 121020 added owner to $outer-variable of inner class
 * version 120923 moved position of writing variables to debug writer to be done before methods
 * version 120522 added check for invalid "final abstract" combination
 * version 120501 moved call to checkAnnotations to JUnit
 * version 120228 added support for validateModifier to ensure fixed (non-changing) modifier during resolve
 * version 120227 added support for constant fields in structs
 * version 110705 added check for annotated struct array variable length (count parameter), added check for inner static class extending non-static classes
 * version 110624 adopted changed Context
 * version 110616 fixed ownership of implicitly inserted abstract method
 * version 110615 added implicit abstract method declaration on interface extension
 * version 110122 fixed alignment if needsAlignedVrbls is set
 * version 101231 added access level hint for implemented interface methods
 * version 101222 added check for newly inserted needsAlignedVrbls and K_FOCD
 * version 101015 adopted changed Expr
 * version 100924 checking for explicit null initialization
 * version 100902 clarified mthdPtrCnt
 * version 100818 added indir-flag-check for implements
 * version 100512 adopted changed Unit
 * version 100510 excluded private variables from naming check
 * version 100504 removed unneccessary conversion, reduced compErr-messages
 * version 100411 beautified warning message (annotation- instead of DEFINE-hint)
 * version 100409 adopted changed TypeRef
 * version 100408 using resolveType for var.init (semantics as before 100401)
 * version 100401 removed MARKER- and DEFINE-specials (replaced by annotations)
 * version 100331 updated MARKER.flash support
 * version 100312 adopted changed TypeRef, added support for flash objects
 * version 100115 removed references to arch.err as it is no longer existing
 * version 100114 reorganized constant object handling
 * version 091215 adopted changed Unit
 * version 091210 fixed dynaMem-mode in checkDefines
 * version 091116 adopted simplified Mthd-signature
 * version 091112 added support for explicit conversion option
 * version 091026 set accessed-flag for $outer variable
 * version 091022 adopted changes in RelationManager
 * version 091021 adopted changed modifier declarations and extended relation tracking
 * version 091014 made variables implemented and referenced public (formerly: private) to allow recompilation in cluster
 * version 091012 fixed visibility level checking for method overloading
 * version 091009 added support for relation tracking
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090916 added visibility check for interface implementing methods
 * version 090724 adopted changed Expr
 * version 090718 added support for non-static final variables, adopted changed Expr
 * version 090623 fixed check of langRoot in resolveIntfExtsIpls
 * version 090616 adopted changed ExVar and ExCall
 * version 090508 removed doFlowAnalysis after integration into resolve
 * version 090507 added checks for struct variables minSize to support correct offset calculation of struct-arrays
 * version 090505 fixed check for static overwriting methods, added support for doFlowAnalysis
 * version 090207 added copyright notice
 * version 081231 fixed typo in error message
 * version 081227 better error output for variable names already in use
 * version 080708 fixed chaining of interface maps if first interface is not used
 * version 080706 adopted changed symInfo-debug-interface, removed printExts
 * version 080629 added supoprt for new noInlineMthdObj option
 * version 080622 better error output for invalidly initialized static variables, fixed static new for dynaMem-mode
 * version 080616 made use of JUnit.fixDynaAddresses
 * version 080614 moved strings and arrays to JUnit as it may be used in Intf, adopted changed Unit.searchVariable, allowed var-overriding
 * version 080414 moved instInitVars to JUnit as it is required for method resolving now
 * version 080402 added new DEFINE-statement "ref" for STRUCT-variables
 * version 080202 added support for alternate newInstance
 * version 080122 added checks for inner STRUCTs, got integration of $outer from JUnit
 * version 080118 added support for anonymous inner classes implementing interfaces
 * version 080109 fixed insertInheritableReferences in dynaMem-mode
 * version 080102 added names of outer units in writeDebug
 * version 071215 replaced call to ctx.mem.copy by semantically correct re-filling, adopted change in Mthd
 * version 071214 fixed handling of multiple interface implementations
 * version 071001 better native method offsets and added DEFINE-special for windows dll calls
 * version 070922 fixed non-final static initialized reference-variables
 * version 070920 fixed too optimistic optimization in dynamic mode
 * version 070918 added support for multidimensional arrays
 * version 070913 removed DEFINE-check for variables (now integrated in statements)
 * version 070909 optimized signature of Expr.resolve, StBlock is breakable
 * version 070823 better resolving of DEFINE-blocks, added support for initialized static float/double vars
 * version 070813 beautified output of writeDebug
 * version 070801 removed methods no longer needed after Unit-redesign
 * version 070731 adopted change of QualID[] to QualIDList
 * version 070730 removed all parent-references to Intf and Clss
 * version 070729 fixed writeDebug to print by option generated descriptor
 * version 070727 adopted change of Mth.id from PureID to String, moved extsID and implID to Unit
 * version 070722 added check of instance variable initialization
 * version 070714 added support to write extsID for symbol information, checking genAllUnitDesc
 * version 070705 fixed address calculation in embConstRAM mode
 * version 070703 added support for embConstRAM
 * version 070703 adopted change in memory statistic method
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070615 adopted removal of Architecture.getRef
 * version 070528 added string statistics
 * version 070527 added support for instance inline arrays
 * version 070523 fixed filling of not generated descriptor, fixed location of initialized class-rels
 * version 070522 optimized method entry allocation, fixed setting of String.count
 * version 070519 made String.count optional, moved setting of variable owner to JParser
 * version 070513 fixed location of constant objects in dynaMem mode
 * version 070511 made genConstObj public
 * version 070509 added support to remove not accessed method fields from descriptor
 * version 070506 added support for runtime environment without objects
 * version 070505 removed not needed owner of method field in descriptor
 * version 070504 added genDescriptor
 * version 070501 renamed assignOffset to match real function
 * version 070331 changed DEFINE-pseudo-calls, stronger checks in DEFINE, added support for additional indirect scalars in SArray
 * version 070327 added codeSize in debug-out
 * version 070303 added support for indirect movable scalars
 * version 070128 optimized explConstr-check in checkInstVarInit
 * version 070127 added checkInstVarInit, added support for initialization of instance variables
 * version 070114 fixed setting M_CALLED of interface-accessed methods, fixed access level of init*, reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 changed struct array detection
 * version 061228 added debug output for statement count
 * version 061211 setting flag for overloaded methods
 * version 061203 optimized calls to printPos and compErr
 * version 061202 support for stat*Table*
 * version 061128 added support for embedded mode
 * version 061111 added support to identify struct-arrays
 * version 061109 added DEFINE-handling for structs
 * version 061030 changed detection of indirectCall
 * version 061027 invented special class STRUCT
 * version 060807 added support for byte-strings
 * version 060803 added ownership of variables
 * version 060723 added support for extension of explicit standard constructor in parent
 * version 060720 added check to not write debug of abstract methods
 * version 060628 adapted new OutputObject functionality and moved code debugging
 * version 060621 added check for implementation of abstract methods in parent
 * version 060607 initial version
 */

public class Clss extends JUnit
{
	//required fields for resolving
	public IndirUnitMapList implemented;
	public UnitList referenced;
	
	protected Clss(QualID ip, QualIDList ii, int imod, int imark, int fid, int il, int ic)
	{
		super(fid, il, ic);
		pack = ip;
		impt = ii;
		modifier = imod;
		marker = imark;
		initStat = new JMthd(null, Modifier.M_STAT | Modifier.M_PUB, fid, il, ic);
		initDyna = new JMthd(null, Modifier.M_PUB, fid, il, ic);
		initStat.owner = initDyna.owner = this;
	}
	
	public boolean validateModifierAfterImportResolve(Context ctx)
	{
		if (this == ctx.langRoot)
		{ //language-root must not extend anything
			if (extsID != null)
			{
				printPos(ctx, "lang-root must extend not extend anything");
				ctx.out.println();
				return false;
			}
		}
		else
		{
			if (extsID == null && this != ctx.structClass)
				extsID = ctx.langRoot.getQIDTo(); //objects must extend language-root
			if (extsID != null)
			{ //this class extends another class
				if (!resolveExtsIplsQID(extsID, ctx))
					return false;
				modifier |= extsID.unitDest.modifier & (Modifier.M_STRUCT | Modifier.MM_FLASH); //copy struct- and flash-flag from parent
			}
		}
		if ((modifier & (Modifier.M_ABSTR | Modifier.M_FIN)) == (Modifier.M_ABSTR | Modifier.M_FIN))
		{
			printPos(ctx, "class can not be both final and abstract");
			ctx.out.println();
			return false;
		}
		return true;
	}
	
	protected boolean resolveIntfExtsIpls(Context ctx)
	{
		Vrbl checkVrbl;
		QualIDList list;
		
		if (this != ctx.langRoot)
		{ //language-root already checked in validateModifier
			if (extsID != null)
			{ //this class extends another class, extsID unitDest is already resolved (but not its interface!)
				if (!extsID.unitDest.resolveInterface(ctx))
					return false;
				if ((extsID.unitDest.modifier & Modifier.M_INDIR) != 0)
				{
					if (name.charAt(0) == '$')
					{ //anonymous inner class implements interface instead of extending class
						extsImplIDList = new QualIDList(); //current extsImplIDList is empty
						extsImplIDList.qid = extsID; //move already resolved qid
						extsID = ctx.langRoot.getQIDTo(); //reset "extends"-field
					}
					else
					{
						printPos(ctx, "interface can not be extended by class ");
						ctx.out.println(name);
						return false;
					}
				}
				if ((extsID.unitDest.modifier & Modifier.M_FIN) != 0)
				{
					printPos(ctx, "can not extend final class ");
					ctx.out.print(extsID.unitDest.name);
					ctx.out.print(" in class ");
					ctx.out.println(name);
					return false;
				}
				if ((modifier & Modifier.M_STAT) != 0 && (extsID.unitDest.modifier & Modifier.M_STAT) == 0)
				{
					printPos(ctx, "static class can not extend non-static class in class ");
					ctx.out.println(name);
					return false;
				}
			}
			if (this == ctx.rteSClassDesc && extsID.unitDest != ctx.langRoot)
			{
				printPos(ctx, "SClassDesc must not extend anything but lang-root");
				ctx.out.println();
				return false;
			}
		}
		list = extsImplIDList;
		while (list != null)
		{
			if (!resolveExtsIplsQID(list.qid, ctx) || !list.qid.unitDest.resolveInterface(ctx))
				return false;
			if ((list.qid.unitDest.modifier & Modifier.M_INDIR) == 0)
			{
				printPos(ctx, "class can not be implemented by class ");
				ctx.out.println(name);
				return false;
			}
			if (ctx.relations != null)
				ctx.relations.addRelation(list.qid.unitDest, this);
			list = list.nextQualID;
		}
		//apply specials for STRUCT
		if ((modifier & Modifier.M_STRUCT) != 0)
		{
			//check implements
			if (extsImplIDList != null)
			{
				printPos(ctx, "can not implement anything in struct ");
				ctx.out.println(name);
				return false;
			}
			//check variables and assign offsets
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				//mark variable
				if ((checkVrbl.modifier & Modifier.M_FIN) == 0)
					checkVrbl.modifier |= Modifier.M_STRUCT;
				if (checkVrbl.type.arrDim > 0)
					checkVrbl.type.typeSpecial = TypeRef.S_STRUCTARRNOTSPEC;
				//check next variable
				checkVrbl = checkVrbl.nextVrbl;
			}
		}
		//everything done
		return true;
	}
	
	protected boolean checkAnnotations(Vrbl v, FilledAnno a, Context ctx)
	{
		StringList keys;
		FilledParam values;
		int iValue;
		
		while (a != null)
		{
			if (a.name.equals(""))
			{
				if (a.keys == null)
				{
					a.printPos(ctx, "need flag or parameter for SJC-annotation");
					return false;
				}
				if ((modifier & Modifier.M_STRUCT) == 0)
				{
					a.printPos(ctx, "parameter annotations only allowed for STRUCTs");
					return false;
				}
				keys = a.keys;
				values = a.values;
				while (keys != null)
				{
					if (!values.expr.resolve(this, initDyna, Expr.RF_CHECKREAD, null, ctx))
						return false;
					if (values.expr.calcConstantType(ctx) != StdTypes.T_INT)
					{
						a.printPos(ctx, "need constant integer value for parameter ");
						ctx.out.print(keys.str);
						return false;
					}
					iValue = values.expr.getConstIntValue(ctx);
					if (keys.str.equals("offset"))
						v.relOff = iValue;
					else if (keys.str.equals("count"))
					{
						if (v.type.arrDim != 1)
						{
							a.printPos(ctx, "parameter count only allowed for one-dimensional arrays");
							return false;
						}
						if (iValue == 0)
							iValue = TypeRef.S_STRUCTARRDONTCHECK;
						else if (iValue < 0)
						{
							a.printPos(ctx, "invalid count (only positive values allowed, use 0 for unchecked arrays)");
							return false;
						}
						v.type.typeSpecial = iValue;
					}
					else
					{
						a.printPos(ctx, "unknown parameter ");
						ctx.out.print(keys.str);
						return false;
					}
					keys = keys.next;
					values = values.nextParam;
				}
			}
			else if (a.name.equals("Ref"))
			{
				if (a.keys != null)
				{
					a.printPos(ctx, "Ref does not need any parameter");
					return false;
				}
				if ((modifier & Modifier.M_STRUCT) == 0)
				{
					a.printPos(ctx, "Ref only allowed for STRUCTs");
					return false;
				}
				v.location = AccVar.L_STRUCTREF;
			}
			else if (a.name.equals("InlineArrayVar"))
			{
				if ((modifier & Modifier.M_STRUCT) != 0 || ctx.indirScalars)
				{
					a.printPos(ctx, "inline arrays not allowed for STRUCTs nor in indir scalar mode");
					return false;
				}
				if (inlArr != null)
				{
					v.printPos(ctx, "only one inline array allowed");
					return false;
				}
				if ((v.modifier & Modifier.M_STAT) != 0)
				{
					v.printPos(ctx, "inline array allowed only for non-static arrays");
					return false;
				}
				inlArr = v;
				v.location = AccVar.L_INLARR;
				v.type.typeSpecial = TypeRef.S_INSTINLARR;
			}
			else if (a.name.equals("InlineArrayCount"))
			{
				if ((v.modifier & (Modifier.M_STAT | Modifier.M_STRUCT)) != 0)
				{
					v.printPos(ctx, "static or struct variable not allowed as count for inline array");
					return false;
				}
				if (isInlArrCountDefined() || !v.type.isIntType())
				{
					v.printPos(ctx, "only one inline array count allowed, it has to be of type int");
					return false;
				}
				v.modifier |= Modifier.M_ARRLEN;
			}
			else if (a.name.equals("Head"))
			{
				if (a.keys != null)
				{
					a.printPos(ctx, "Head does not need any parameter");
					return false;
				}
				if (!ctx.indirScalars)
				{
					a.printPos(ctx, "Head only allowed in indirect scalar mode");
					return false;
				}
				v.location = AccVar.L_INSTSCL;
			}
			else
			{
				a.printPos(ctx, "unknown SJC-Clss-Annotation");
				return false;
			}
			a = a.nextAnno;
		}
		return true;
	}
	
	private boolean isInlArrCountDefined()
	{
		Vrbl v = vars;
		while (v != null)
		{
			if ((v.modifier & Modifier.M_ARRLEN) != 0)
				return true;
			v = v.nextVrbl;
		}
		return false;
	}
	
	protected boolean checkInstVarInit(Vrbl var, Context ctx)
	{
		JMthd addedConstr;
		VrblList lastInit;
		
		//check validity
		if ((modifier & Modifier.M_STRUCT) != 0)
		{
			var.printPos(ctx, "initialization of struct variables not supported");
			return false;
		}
		//check if a constructor already exists
		if (!explConstr)
		{ //insert standard constructor for initialization
			addedConstr = new JMthd(name, Modifier.M_PUB, fileID, line, col);
			addedConstr.nextMthd = mthds;
			(mthds = addedConstr).isConstructor = true;
			addedConstr.block = new StBlock(null, null, fileID, line, col);
			if (ctx.verbose)
			{
				ctx.out.print("needed to insert standard constructor for initialization of instance variables in unit ");
				ctx.out.println(name);
			}
			if (!addedConstr.checkNameAndType(this, ctx))
				return false;
		}
		//check variable's init
		if (var.init == null || !var.init.resolveType(this, ctx))
		{
			var.printPos(ctx, "invalid instance variable initialization");
			return false;
		}
		//check assignment of null (not needed as instance block must be zero after allocation)
		if (var.init.baseType == StdTypes.T_NULL)
			return true; //null, do not insert into instInitVars
		//insert variable into list
		if (instInitVars == null)
			instInitVars = new VrblList(var);
		else
		{
			lastInit = instInitVars;
			while (lastInit.next != null)
				lastInit = lastInit.next;
			lastInit.next = new VrblList(var);
		}
		//everything ok
		return true;
	}
	
	protected boolean resolveMthdExtsIpls(Context ctx)
	{
		if (extsID != null)
		{ //this class extends another class, interface is resolved successfully
			return extsID.unitDest.resolveMethodBlocks(ctx);
		}
		return true;
	}
	
	protected boolean checkDeclarations(Context ctx)
	{
		QualIDList chkIntfParentList;
		Unit checkClss;
		Mthd checkMthd, destMthd;
		Vrbl checkVrbl, tmpVrbl;
		boolean fulfilled, sigOK = true;
		QualIDList chkIntf;
		int i;
		
		//check variable specials
		if (inlArr != null ^ isInlArrCountDefined())
		{ //either both active or both inactive
			ctx.out.print("missing count for inline variable in class ");
			ctx.out.println(name);
			return false;
		}
		//apply specials for FLASH
		if ((modifier & Modifier.MM_FLASH) != 0)
		{
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				switch (checkVrbl.type.typeSpecial)
				{
					case TypeRef.S_NOSPECIAL:
						checkVrbl.type.typeSpecial = TypeRef.S_FLASHREF;
						break;
					case TypeRef.S_INSTINLARR:
						checkVrbl.type.typeSpecial = TypeRef.S_FLASHINLARR;
						break;
					default:
						checkVrbl.printPos(ctx, "flash tag not available for special variable ");
						ctx.out.print(checkVrbl.name);
						ctx.out.print(" in unit ");
						ctx.out.println(name);
						return false;
				}
				checkVrbl = checkVrbl.nextVrbl;
			}
		}
		//check state of flags for explicit constructors
		if (!explConstr && extsID != null)
		{ //if we had no explicit constructor, copy state of parent
			explConstr = extsID.unitDest.explConstr;
			explStdConstr = extsID.unitDest.explStdConstr;
		}
		//check extension of methods
		checkMthd = mthds;
		while (checkMthd != null)
		{
			if (((checkMthd.modifier & Modifier.M_STAT) == 0) && (extsID != null))
			{ //check extend if not static
				destMthd = extsID.unitDest.searchMethod(checkMthd, ctx);
			}
			else
				destMthd = null;
			if (destMthd != null && (destMthd.modifier & Modifier.M_PRIV) == 0)
			{ //this will overwrite another method
				if (checkMthd.retType.compareType(destMthd.retType, false, ctx) != TypeRef.C_EQ)
				{
					checkMthd.printPos(ctx, "method ");
					ctx.out.print(checkMthd.name);
					ctx.out.print(" differs to parent in return-type only in class ");
					ctx.out.println(name);
					ctx.out.println();
					sigOK = false;
				}
				else
				{
					if ((checkMthd.modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP | Modifier.M_PRIV)) > (destMthd.modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP | Modifier.M_PRIV)))
					{
						checkMthd.printPos(ctx, "method ");
						ctx.out.print(checkMthd.name);
						ctx.out.print(" may not reduce visability level in class ");
						ctx.out.println(name);
						sigOK = false;
					}
					else
						switch (destMthd.modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP))
						{
							case Modifier.M_PUB:
								checkMthd.modifier |= Modifier.MA_PUB;
								break;
							case Modifier.M_PROT:
								checkMthd.modifier |= Modifier.MA_PROT;
								break;
							case Modifier.M_PACP:
								checkMthd.modifier |= Modifier.MA_PACP;
								break;
						}
					(checkMthd.ovldMthd = destMthd).modifier |= Modifier.M_OVERLD | Modifier.M_NDDESC;
				}
			}
			//else checkMthd.ovldMthd=null; //this is a new method, already initialized to null
			checkMthd = checkMthd.nextMthd;
		}
		fulfilled = true;
		//if not abstract, check implementation of abstract methods of abstract parent
		if (extsID != null && (extsID.unitDest.modifier & Modifier.M_ABSTR) != 0 && (modifier & Modifier.M_ABSTR) == 0)
		{
			checkClss = extsID.unitDest;
			while (checkClss != null && (checkClss.modifier & Modifier.M_ABSTR) != 0)
			{
				checkMthd = checkClss.mthds;
				while (checkMthd != null)
				{
					if ((checkMthd.modifier & Modifier.M_ABSTR) != 0)
					{
						if ((destMthd = searchMethod(checkMthd, ctx)) == checkMthd)
						{
							printPos(ctx, "abstract method ");
							checkMthd.printNamePar(ctx.out);
							ctx.out.print(" has to be implemented in class ");
							ctx.out.println(name);
							fulfilled = false;
						}
						else if ((destMthd.modifier & (Modifier.M_ABSTR | Modifier.M_STAT)) != 0)
						{
							destMthd.printPos(ctx, "method overwriting abstract method has an invalid modifier");
							fulfilled = false;
						}
					}
					checkMthd = checkMthd.nextMthd;
				}
				if (checkClss.extsID != null)
					checkClss = checkClss.extsID.unitDest;
				else
					checkClss = null;
			}
		}
		//check implementation of interface methods
		chkIntf = extsImplIDList;
		while (chkIntf != null)
		{
			if (!checkImplementation(chkIntf.qid.unitDest, ctx))
				fulfilled = false;
			else
			{
				chkIntfParentList = chkIntf.qid.unitDest.extsImplIDList;
				while (chkIntfParentList != null)
				{ //check all parents - implementation is done already, but we need the pointers and indices
					fulfilled &= checkImplementation(chkIntfParentList.qid.unitDest, ctx);
					chkIntfParentList = chkIntfParentList.nextQualID;
				}
			}
			chkIntf = chkIntf.nextQualID;
		}
		//check existence of outer class
		if (outerUnit != null && (modifier & Modifier.M_STAT) == 0)
		{ //check extends and on demand add internal variable "$outer"
			if ((modifier & Modifier.M_STRUCT) != 0)
			{
				printPos(ctx, "inner struct ");
				ctx.out.print(name);
				ctx.out.println(" has to be static");
				fulfilled = false;
			}
			else if (extsID != null && extsID.unitDest.outerUnit != null)
			{ //check if outer is compatible
				if (extsID.unitDest.outerUnit != outerUnit && !extsID.unitDest.outerUnit.isParent(outerUnit, ctx))
				{
					printPos(ctx, "outer class of type ");
					ctx.out.print(extsID.unitDest.outerUnit.name);
					ctx.out.print(" required in extending inner class ");
					ctx.out.println(name);
					fulfilled = false;
				}
			}
			else
			{ //no outer unit so far, add "$outer" variable
				checkVrbl = new Vrbl(OUTERVARNAME, 0, fileID, line, col);
				checkVrbl.owner = this;
				checkVrbl.type = ctx.objectType;
				checkVrbl.modifier |= Modifier.MA_ACCSSD | Modifier.MA_PRIV;
				if (!checkVrbl.type.resolveType(this, ctx) || !checkVrbl.enterSize(Vrbl.L_UNIT, ctx))
				{
					compErr(ctx, "generated outer variable invalid");
					fulfilled = false;
				}
				checkVrbl.nextVrbl = vars;
				vars = checkVrbl;
			}
		}
		//check extension of variables
		if (extsID != null)
		{
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				if ((tmpVrbl = extsID.unitDest.searchVariable(checkVrbl.name, ctx)) != null && (tmpVrbl.modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP | Modifier.M_PRIV)) != Modifier.M_PRIV)
				{
					checkVrbl.printPos(ctx, "warning: name of variable ");
					ctx.out.print(checkVrbl.name);
					ctx.out.print(" already in use in parent of ");
					ctx.out.println(name);
				}
				checkVrbl = checkVrbl.nextVrbl;
			}
		}
		//assign offsets if this is a struct
		if ((modifier & Modifier.M_STRUCT) != 0)
		{
			if (extsID == null)
				instScalarTableSize = 0; //this==ctx.structClass, no variable inside
			else
			{
				instScalarTableSize = extsID.unitDest.instScalarTableSize; //get size of parent
				checkVrbl = vars;
				while (checkVrbl != null)
				{
					//get size
					if (checkVrbl.type.arrDim != 0)
					{
						if (checkVrbl.type.typeSpecial > 0)
							i = checkVrbl.minSize * checkVrbl.type.typeSpecial;
						else
							i = 0;
					}
					else
						i = checkVrbl.minSize;
					//assign offset if variable is not final static
					switch (checkVrbl.modifier & (Modifier.M_FIN | Modifier.M_STAT))
					{
						case Modifier.M_FIN:
						case Modifier.M_STAT:
							printPos(ctx, "variable ");
							ctx.out.print(checkVrbl.name);
							ctx.out.print(" needs to be final static or non-final dynamic in struct ");
							ctx.out.print(name);
							fulfilled = false;
							break;
						case Modifier.M_FIN | Modifier.M_STAT: //constant field, no offset required
							break;
						case 0:
							if (checkVrbl.relOff == AccVar.INV_RELOFF)
							{ //not explicitly assigned offset
								checkVrbl.relOff = instScalarTableSize;
								instScalarTableSize += i;
							}
							else
							{ //variable had offset-annotation, check if end is behind current end
								if ((i = checkVrbl.relOff + i) > instScalarTableSize)
									instScalarTableSize = i;
							}
							break;
					}
					//check array special
					if (checkVrbl.type.arrDim != 0 && checkVrbl.type.typeSpecial == TypeRef.S_STRUCTARRNOTSPEC)
					{ //no array size specified
						//print a warning
						checkVrbl.printPos(ctx, "warning: unchecked array size (use @SJC(count=X) for ");
						ctx.out.print(checkVrbl.name);
						ctx.out.print(" in struct ");
						ctx.out.println(name);
					}
					//ok, next variable
					checkVrbl = checkVrbl.nextVrbl;
				}
			}
			if (fulfilled && sigOK)
				offsetsAssigned = true;
			else
				offsetError = true;
		}
		//check if there is an explicit constructor if there are not initialized non-static final variables
		if (writeCheckFinalVars != null && !explConstr && !explStdConstr)
		{
			printPos(ctx, "not initialized non-static final variables need initialization (at declation or in constructor) in unit ");
			ctx.out.println(name);
			offsetError = true;
			sigOK = false;
		}
		//return true if everything was OK
		return (fulfilled && sigOK);
	}
	
	public UnitList getRefUnit(Unit refUnit, boolean insert)
	{
		UnitList check, last;
		
		//check if imported by parent
		if (extsID != null && (check = extsID.unitDest.getRefUnit(refUnit, false)) != null)
			return check; //found
		//check if imported by us
		check = referenced;
		last = null;
		//search if already imported
		while (check != null)
		{
			if (check.unit == refUnit)
				return check;
			last = check;
			check = check.next;
		}
		//not found
		if (!insert)
			return null; //we shall not insert
		//insert in list and allocate space in relocs
		refUnit.modifier |= Modifier.MA_ACCSSD;
		if (last == null)
		{
			referenced = new UnitList(refUnit);
			last = referenced;
		}
		else
		{
			last.next = new UnitList(refUnit);
			last = last.next;
		}
		return last;
	}
	
	public boolean assignOffsets(boolean doClssOff, Context ctx)
	{
		Unit parent = null;
		boolean doInstOff = true;
		Vrbl checkVrbl, inlArrVrbl = null;
		Mthd checkMthd, omthd;
		UnitList imported;
		int offTmp;
		int mthdPtrCnt = ctx.dynaMem ? 2 : 1; //additionally need to remember owner of method in dynamic environment
		
		if (offsetsAssigned)
			return true; //not called for struct, already set in checkStructSpecials
		if (offsetError)
			return false;
		if (extsID == null)
		{
			if (this == ctx.langRoot)
			{ //java.lang.Object
				if (doClssOff)
				{ //second step
					clssScalarTableSize = ctx.rteSClassDesc.instScalarTableSize;
					clssRelocTableEntries = ctx.rteSClassDesc.instRelocTableEntries;
					doInstOff = false; //do not change already entered inst-offs
				}
				else
				{ //this is the first call, just enter required inst-offs
					instScalarTableSize = instRelocTableEntries = 0;
				}
			}
			//else: special internal class, everything set already
		}
		else
		{
			parent = extsID.unitDest;
			if (this == ctx.rteSClassDesc)
			{ //java.rte.SClassDesc
				if (ctx.leanRTE)
				{
					if (doClssOff)
					{ //second step
						clssScalarTableSize = clssRelocTableEntries = 0;
						doInstOff = false; //do not change already entered inst-offs
					}
					else
					{ //this is the first call
						instScalarTableSize = instRelocTableEntries = instIndirScalarTableSize = 0;
					}
				}
				else
				{
					if (doClssOff)
					{ //second step
						clssScalarTableSize = parent.clssScalarTableSize;
						clssRelocTableEntries = parent.clssRelocTableEntries;
						doInstOff = false; //do not change already entered inst-offs
					}
					else
					{ //this is the first call, get inst-values of java.lang.Object
						instScalarTableSize = parent.instScalarTableSize;
						instRelocTableEntries = parent.instRelocTableEntries;
						instIndirScalarTableSize = parent.instIndirScalarTableSize;
					}
				}
			}
			else
			{ //all other objects
				if (!doClssOff)
				{ //all other must enter everything at once
					ctx.out.println("invalid call to Clss.assignOffsets for normal class");
					offsetError = true;
					return false;
				}
				//check parent
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
			}
		}
		//enter methods
		if (doClssOff)
		{
			checkMthd = mthds;
			while (checkMthd != null)
			{
				if ((checkMthd.modifier & Modifier.M_NAT) == 0)
				{
					if ((checkMthd.modifier & Modifier.M_ABSTR) == 0)
					{
						omthd = checkMthd;
						if (ctx.genAllMthds)
							checkMthd.modifier |= Modifier.M_NDCODE;
						else
							while (omthd != null)
							{
								if ((omthd.modifier & Modifier.MA_ACCSSD) != 0)
								{
									checkMthd.modifier |= Modifier.M_NDCODE;
									break;
								}
								omthd = omthd.ovldMthd;
							}
						if ((checkMthd.modifier & Modifier.M_NDCODE) == 0)
						{
							if (ctx.debugCode)
								checkMthd.modifier |= Modifier.M_NDCODE;
							if (ctx.verbose)
							{
								checkMthd.printPos(ctx, ctx.debugCode ? "would skip" : "skipping");
								ctx.out.print(" code for method ");
								checkMthd.printSig(ctx.out);
								ctx.out.print(" in ");
								if (pack != null)
								{
									pack.printFullQID(ctx.out);
									ctx.out.print('.');
								}
								ctx.out.println(name);
							}
						}
					}
					if (checkMthd.ovldMthd == null || checkMthd.ovldMthd.relOff == 0)
					{
						if (ctx.dynaMem || (checkMthd.modifier & Modifier.M_NDDESC) != 0)
						{
							modifier |= Modifier.MA_ACCSSD;
							checkMthd.relOff = -(clssRelocTableEntries += mthdPtrCnt) * ctx.arch.relocBytes; //new method, two references required to remember owner of class
						}
						else
							checkMthd.relOff = 0; //no class descriptor entry needed
					}
					else
					{
						modifier |= Modifier.MA_ACCSSD;
						checkMthd.relOff = checkMthd.ovldMthd.relOff; //overloaded method, copy table index
					}
				}
				checkMthd = checkMthd.nextMthd;
			}
		}
		//handle variables
		checkVrbl = vars;
		while (checkVrbl != null)
		{
			//enter variable offset
			switch (checkVrbl.location)
			{
				case Vrbl.L_CLSSSCL:
					if (doClssOff)
					{
						modifier |= Modifier.MA_ACCSSD;
						if (ctx.embedded)
						{
							if (ctx.arch.needsAlignedVrbls)
							{ //align current ramSize to a multiple of the variable size
								offTmp = checkVrbl.minSize - 1;
								ctx.ramSize = (ctx.ramSize + offTmp) & ~offTmp;
							}
							//get space
							checkVrbl.relOff = ctx.ramSize;
							ctx.ramSize += checkVrbl.minSize;
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
					}
					break;
				case Vrbl.L_CLSSREL:
					if (doClssOff)
					{
						modifier |= Modifier.MA_ACCSSD;
						if (ctx.embedded)
						{
							checkVrbl.relOff = ctx.ramSize;
							ctx.ramSize -= checkVrbl.minSize * ctx.arch.relocBytes; //no special alignment neccessary, relocBytes are aligned per definitionem
						}
						else
							checkVrbl.relOff = -(statRelocTableEntries -= checkVrbl.minSize) * ctx.arch.relocBytes;
					}
					break;
				case Vrbl.L_INSTSCL:
					if (doInstOff)
					{
						//align
						offTmp = checkVrbl.minSize - 1;
						instScalarTableSize = (instScalarTableSize + offTmp) & ~offTmp;
						//get space
						checkVrbl.relOff = instScalarTableSize;
						instScalarTableSize += checkVrbl.minSize;
					}
					break;
				case Vrbl.L_INSTIDS:
					if (doInstOff)
					{
						//align
						offTmp = checkVrbl.minSize - 1;
						instIndirScalarTableSize = (instIndirScalarTableSize + offTmp) & ~offTmp;
						//get space
						checkVrbl.relOff = instIndirScalarTableSize;
						instIndirScalarTableSize += checkVrbl.minSize;
					}
					break;
				case Vrbl.L_INSTREL:
					if (doInstOff)
					{
						if (checkVrbl.minSize == -1)
							instRelocTableEntries++;
						else if (checkVrbl.minSize == -2)
							instRelocTableEntries += 2;
						else
						{
							checkVrbl.compErr(ctx, "has minSize!=-1 and !=-2 for INSTREL");
							ctx.out.println();
							offsetError = true;
							return false;
						}
						checkVrbl.relOff = -instRelocTableEntries * ctx.arch.relocBytes;
					}
					break;
				case Vrbl.L_CONSTDC: //constants do not need space
				case Vrbl.L_CONST:
					break;
				case Vrbl.L_STRUCT:
					compErr(ctx, "struct variable in assignOffsets");
					offsetError = true;
					return false;
				case Vrbl.L_INLARR:
					if (doInstOff)
						inlArrVrbl = checkVrbl; //remember for second run
					break;
				default:
					compErr(ctx, "unknown location of variable");
					offsetError = true;
					return false;
			}
			checkVrbl = checkVrbl.nextVrbl;
		}
		if (inlArrVrbl != null)
		{
			//search corresponding length field and enter position
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				if ((checkVrbl.modifier & Modifier.M_ARRLEN) != 0)
				{
					inlArrVrbl.relOff = checkVrbl.relOff;
					break;
				}
				checkVrbl = checkVrbl.nextVrbl;
			}
		}
		if (doClssOff)
		{
			//if there were statics, align class-scalars to stack-alignment
			if (statScalarTableSize != 0)
				clssScalarTableSize = (clssScalarTableSize + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits;
			if (ctx.dynaMem)
			{
				if ((imported = referenced) != null)
					modifier |= Modifier.MA_ACCSSD;
				//enter imported units
				while (imported != null)
				{
					imported.relOff = -(++clssRelocTableEntries) * ctx.arch.relocBytes;
					imported = imported.next;
				}
				//fix dynamic addresses
				fixDynaAddresses(ctx);
			}
			//everything done, set flag
			offsetsAssigned = true;
		}
		return true;
	}
	
	public boolean genDescriptor(Context ctx)
	{
		Vrbl checkVrbl;
		
		if (outputLocation != null || (modifier & Modifier.M_STRUCT) != 0)
			return true;
		if (outputError)
			return false;
		
		if (ctx.relations != null && extsID != null)
			ctx.relations.addRelation(extsID.unitDest, this);
		
		if (extsID != null && this != ctx.rteSClassDesc && !extsID.unitDest.genDescriptor(ctx))
		{
			outputError = true;
			return false;
		}
		if (ctx.genAllUnitDesc || (marker & Marks.K_FOCD) != 0)
			modifier |= Modifier.MA_ACCSSD;
		if (!ctx.dynaMem && (modifier & Modifier.MA_ACCSSD) == 0)
		{
			if (ctx.verbose)
			{
				printPos(ctx, "skipping descriptor for class ");
				if (pack != null)
				{
					pack.printFullQID(ctx.out);
					ctx.out.print('.');
				}
				ctx.out.println(name);
			}
			if (extsID != null)
				outputLocation = extsID.unitDest.outputLocation;
			return true;
		}
		
		if (!allocateDescriptor(clssScalarTableSize + statScalarTableSize, clssRelocTableEntries + statRelocTableEntries, ctx.rteSClassDesc.outputLocation, ctx))
		{
			outputError = true;
			return false;
		}
		//clss* will not be changed anymore, fix stat*
		if (!ctx.embedded)
		{
			checkVrbl = vars;
			while (checkVrbl != null)
			{
				switch (checkVrbl.location)
				{ //L_CLSS* are in fact allocated in stat*, so move them by clss*
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
		//everything ok
		return true;
	}
	
	public boolean genOutput(Context ctx)
	{
		int tmpOff;
		Object tmp, addr;
		Mthd mthd;
		IndirUnitMapList chkIntf, lastIntf = null, firstIntf = null;
		Vrbl var;
		
		//check if we are not yet done and parents OK
		if (outputError)
			return false;
		if (outputGenerated)
			return true;
		//check parents
		if (extsID != null && !extsID.unitDest.genOutput(ctx))
		{
			outputError = true;
			return false;
		}
		//check if not everything has to be done
		if ((modifier & Modifier.M_STRUCT) != 0)
			return outputGenerated = true; //break if struct
		if (extsID != null && outputLocation == extsID.unitDest.outputLocation)
		{ //there is no extra class descriptor, just generate methods
			mthd = mthds;
			while (mthd != null)
			{
				if ((mthd.modifier & Modifier.M_NDCODE) != 0)
				{
					mthd.genOutput(ctx);
					if (ctx.err)
					{
						outputError = true;
						return false;
					}
				}
				mthd = mthd.nextMthd;
			}
			return outputGenerated = true; //do not write to (not existing) class descriptor
		}
		//handle interface implementations
		chkIntf = implemented;
		while (chkIntf != null)
		{
			//generate output for this particular interface-map
			if ((chkIntf.intf.modifier & Modifier.MA_ACCSSD) != 0)
			{
				if (!genIntfOutput(chkIntf, ctx))
				{
					outputError = true;
					return false;
				}
				else
				{
					if (lastIntf != null)
					{ //set next pointer of last interface map to current interface map
						ctx.arch.putRef(lastIntf.outputLocation, ctx.rteSIMnext, chkIntf.outputLocation, 0);
					}
					else
						firstIntf = chkIntf; //remember this interface as first interface
					lastIntf = chkIntf; //remember this interface as done
				}
			}
			//check next one
			chkIntf = chkIntf.next;
		}
		//enter parent pointer
		if (extsID != null && extsID.unitDest.outputLocation != null)
		{
			ctx.arch.putRef(outputLocation, ctx.rteSClassParent, extsID.unitDest.outputLocation, 0);
		}
		//enter size if required
		if (ctx.alternateObjNew)
		{
			ctx.mem.putInt(outputLocation, ctx.rteSClassInstScalarSize, instScalarTableSize);
			ctx.mem.putInt(outputLocation, ctx.rteSClassInstRelocTableEntries, instRelocTableEntries);
			if (ctx.indirScalars)
				ctx.mem.putInt(outputLocation, ctx.rteSClassInstIndirScalarSize, instIndirScalarTableSize);
		}
		//create method-objects
		mthd = mthds;
		while (mthd != null)
		{
			if (ctx.noInlineMthdObj && (mthd.marker & Marks.K_FINL) != 0)
			{
				if ((mthd.modifier & Modifier.MA_INTFMD) != 0)
				{
					mthd.printPos(ctx, "method is used in interface, skipping output is not possible");
					ctx.out.println();
					outputError = true;
					return false;
				}
				mthd.modifier &= ~Modifier.M_NDCODE;
			}
			if ((mthd.modifier & Modifier.M_NDCODE) != 0)
			{
				mthd.genOutput(ctx);
				if (ctx.err)
				{
					outputError = true;
					return false;
				}
				//enter pointers of static methods (others are done by enterInheritableReferences)
				if (mthd.relOff != 0 && (mthd.modifier & Modifier.M_STAT) != 0)
				{
					ctx.arch.putRef(outputLocation, mthd.relOff, mthd.outputLocation, 0); //put address of method
					if (ctx.dynaMem)
						ctx.arch.putRef(outputLocation, mthd.relOff + ctx.arch.relocBytes, outputLocation, 0); //put ourselve as owner
				}
			}
			mthd = mthd.nextMthd;
		}
		//enter referenced units, inheritable method pointers, pointer to first interface map and chain (if existing) interfaces of parent
		chkIntf = enterInheritableReferences(outputLocation, lastIntf, ctx);
		if (firstIntf != null)
			ctx.arch.putRef(outputLocation, ctx.rteSClassImpl, firstIntf.outputLocation, 0);
		else if (chkIntf != null)
			ctx.arch.putRef(outputLocation, ctx.rteSClassImpl, chkIntf.outputLocation, 0);
		//initialize static variables
		var = vars;
		while (var != null)
		{
			if ((var.modifier & Modifier.M_STAT) != 0 && var.init != null)
			{
				switch (var.location)
				{
					case Vrbl.L_CLSSREL:
						if (var.init.isNullType())
							break;
						if (!var.init.isCompInitConstObject(ctx))
						{
							var.init.printPos(ctx, "value of static reference variable is not constant");
							ctx.out.println();
							outputError = true;
							return false;
						}
						if ((tmp = var.init.getConstInitObj(ctx).outputLocation) == null)
						{
							var.init.compErr(ctx, "static initialization with invalid destination");
							outputError = true;
							return false;
						}
						if (ctx.embedded)
						{
							addr = ctx.ramInitLoc;
							tmpOff = ctx.embConstRAM ? ctx.ramOffset : 0;
						}
						else
						{
							addr = outputLocation;
							tmpOff = 0;
						}
						ctx.arch.putRef(addr, var.relOff, tmp, tmpOff);
						break;
					case Vrbl.L_CLSSSCL:
						if (var.init.isNullType() && var.type.isStructType())
							break; //null==0 is default but needs special care for STRUCTs
						int calcConst = var.init.calcConstantType(ctx);
						if (calcConst == 0)
						{
							var.init.printPos(ctx, "value of static scalar variable ");
							ctx.out.print(var.name);
							ctx.out.println(" is not constant");
							outputError = true;
							return false;
						}
						if (ctx.embedded)
							addr = ctx.ramInitLoc;
						else
							addr = outputLocation;
						int type = var.type.baseType;
						if (var.init.isStructType())
							type = calcConst;
						switch (type)
						{
							case StdTypes.T_BOOL:
							case StdTypes.T_BYTE:
								ctx.mem.putByte(addr, var.relOff, (byte) var.init.getConstIntValue(ctx));
								break;
							case StdTypes.T_CHAR:
							case StdTypes.T_SHRT:
								ctx.mem.putShort(addr, var.relOff, (short) var.init.getConstIntValue(ctx));
								break;
							case StdTypes.T_INT:
							case StdTypes.T_FLT:
								ctx.mem.putInt(addr, var.relOff, var.init.getConstIntValue(ctx));
								break;
							case StdTypes.T_LONG:
							case StdTypes.T_DBL:
								ctx.mem.putLong(addr, var.relOff, var.init.getConstLongValue(ctx));
								break;
							default:
								ctx.out.print("invalid type during assign of static variable ");
								ctx.out.println(var.name);
								outputError = true;
								return false;
						}
						break;
					case Vrbl.L_CONST:
					case Vrbl.L_CONSTDC:
						//variable is constant (used or only declared), do not assign anything
						break;
					default:
						ctx.out.print("invalid location during assign of static variable ");
						ctx.out.println(var.name);
						outputError = true;
						return false;
				}
			}
			var = var.nextVrbl;
		}
		//if there were errors, return with signal
		if (ctx.err)
			return false;
		//everything done
		return outputGenerated = true;
	}
	
	public IndirUnitMapList enterInheritableReferences(Object objLoc, IndirUnitMapList lastIntf, Context ctx)
	{ //returns pointer to first interface map
		UnitList refc;
		IndirUnitMapList inheritedIntfMaps = null;
		Mthd mthd;
		
		//handle parent and enter parent's interface map pointer into lastIntf if not null
		if (extsID != null && (inheritedIntfMaps = extsID.unitDest.enterInheritableReferences(objLoc, null, ctx)) != null && lastIntf != null)
		{
			ctx.arch.putRef(lastIntf.outputLocation, ctx.rteSIMnext, inheritedIntfMaps.outputLocation, 0);
		}
		//enter pointers to dynamic methods
		mthd = mthds;
		if (ctx.dynaMem)
			while (mthd != null)
			{
				if (mthd.relOff != 0)
				{ //always put method and owner
					ctx.arch.putRef(objLoc, mthd.relOff, mthd.outputLocation, 0); //put address of method
					ctx.arch.putRef(objLoc, mthd.relOff + ctx.arch.relocBytes, outputLocation, 0); //put ourselve as owner
				}
				mthd = mthd.nextMthd;
			}
		else
			while (mthd != null)
			{
				if (mthd.relOff != 0 && (mthd.modifier & Modifier.M_STAT) == 0)
				{ //only if dynamic call, only address needed
					ctx.arch.putRef(objLoc, mthd.relOff, mthd.outputLocation, 0); //put address of method
				}
				mthd = mthd.nextMthd;
			}
		//handle references to other units
		if ((refc = referenced) != null)
		{
			if (!ctx.dynaMem)
			{ //static compilation must not have filled referenced
				ctx.out.println("### Clss.referenced!=null with dynaMem==false");
				ctx.err = true;
				return null;
			}
			while (refc != null)
			{
				ctx.arch.putRef(objLoc, refc.relOff, refc.unit.outputLocation, 0);
				refc = refc.next;
			}
		}
		return implemented != null ? implemented : inheritedIntfMaps;
	}
	
	public void writeDebug(Context ctx, DebugWriter dbw)
	{
		Mthd mthd;
		UnitList clst;
		IndirUnitMapList intf;
		
		dbw.startUnit((modifier & Modifier.M_STRUCT) == 0 ? "class" : "struct", this);
		if ((modifier & Modifier.M_STRUCT) == 0)
			dbw.hasUnitOutputLocation((!ctx.dynaMem && (modifier & Modifier.MA_ACCSSD) == 0) ? null : outputLocation);
		else
			dbw.hasUnitOutputLocation(null);
		dbw.hasUnitFields(clssRelocTableEntries, clssScalarTableSize, statRelocTableEntries, statScalarTableSize, instRelocTableEntries, instScalarTableSize, instIndirScalarTableSize);
		writeVarsAndConstObjDebug(ctx, dbw);
		dbw.startMethodList();
		mthd = mthds;
		while (mthd != null)
		{
			dbw.hasMethod(mthd, false);
			mthd = mthd.nextMthd;
		}
		dbw.endMethodList();
		dbw.startImportedUnitList();
		clst = referenced;
		while (clst != null)
		{
			dbw.hasImportedUnit(clst);
			clst = clst.next;
		}
		dbw.endImportedUnitList();
		dbw.startInterfaceMapList();
		intf = implemented;
		while (intf != null)
		{
			dbw.hasInterfaceMap(intf);
			intf = intf.next;
		}
		dbw.endInterfaceMapList();
		dbw.endUnit();
	}
	
	private boolean checkImplementation(Unit checkIntf, Context ctx)
	{
		Mthd checkMthd, destMthd;
		boolean fulfilled = true;
		IndirUnitMapList me;
		
		//check if asked interface is already inserted into implemented list
		me = implemented;
		while (me != null)
		{
			if (me.intf == checkIntf)
				return true; //this interface is not new to us
			me = me.next;
		}
		//interface needs to be added in implemented list
		me = new IndirUnitMapList(checkIntf, implemented);
		me.map = new JMthd[checkIntf.indirMthdTableEntries + 1];
		//reserved for address: me.map[0]
		checkMthd = checkIntf.mthds;
		while (checkMthd != null)
		{
			destMthd = searchMethod(checkMthd, ctx);
			if (destMthd != null)
			{
				if ((destMthd.modifier & Modifier.M_STAT) != 0)
				{
					destMthd.printPos(ctx, "invalid modifier \"static\" for implementing method ");
					ctx.out.print(destMthd.name);
					ctx.out.print(" in unit ");
					ctx.out.println(name);
					fulfilled = false;
				}
				else if (destMthd.retType.compareType(checkMthd.retType, false, ctx) != TypeRef.C_EQ)
				{
					destMthd.printPos(ctx, "method ");
					ctx.out.print(destMthd.name);
					ctx.out.print(" differs in return-type compared to interface ");
					ctx.out.print(checkIntf.name);
					ctx.out.print(" in unit ");
					ctx.out.println(name);
					fulfilled = false;
				}
				else if ((destMthd.modifier & Modifier.M_PUB) == 0)
				{
					destMthd.printPos(ctx, "method ");
					ctx.out.print(destMthd.name);
					ctx.out.print(" must be public to implement interface ");
					ctx.out.print(checkIntf.name);
					ctx.out.print(" in unit ");
					ctx.out.println(name);
					fulfilled = false;
				}
				else
				{
					me.map[checkMthd.relOff] = destMthd;
					destMthd.modifier |= Modifier.MA_ACCSSD | Modifier.M_NDDESC | Modifier.MA_INTFMD;
					switch (checkMthd.modifier & (Modifier.M_PUB | Modifier.M_PROT | Modifier.M_PACP))
					{
						case Modifier.M_PUB:
							destMthd.modifier |= Modifier.MA_PUB;
							break;
						case Modifier.M_PROT:
							destMthd.modifier |= Modifier.MA_PROT;
							break;
						case Modifier.M_PACP:
							destMthd.modifier |= Modifier.MA_PACP;
							break;
					}
				}
				checkMthd = checkMthd.nextMthd;
			}
			else if ((modifier & Modifier.M_ABSTR) != 0)
			{ //if class is abstract, silently insert method to fulfill interface
				destMthd = checkMthd.copy();
				destMthd.owner = this;
				destMthd.nextMthd = mthds;
				mthds = destMthd;
				//do not step on to next method but check inserted method again (required to check modifier and insert relOff in map
			}
			else
			{
				printPos(ctx, "no method ");
				checkMthd.printNamePar(ctx.out);
				ctx.out.print(" to fulfill interface ");
				ctx.out.print(checkIntf.name);
				ctx.out.print(" in unit ");
				ctx.out.println(name);
				fulfilled = false;
				checkMthd = checkMthd.nextMthd;
			}
		}
		//check state
		if (fulfilled)
		{
			implemented = me; //already checked ones are chained (see constructor of JIntfList)
			return true;
		}
		return false;
	}
	
	private boolean genIntfOutput(IndirUnitMapList current, Context ctx)
	{
		int i, cnt, base;
		Object intfLoc;
		
		//check if owner is generated
		if (!current.intf.genOutput(ctx))
			return false;
		//generate map
		cnt = current.map.length;
		base = ctx.rteSIntfMap.instScalarTableSize;
		if ((intfLoc = ctx.mem.allocate(base + cnt * 4, 0, ctx.rteSIntfMap.instRelocTableEntries, ctx.rteSIntfMap.outputLocation)) == null)
		{
			ctx.out.println("error in allocating memory while creating interface-maps");
			outputError = true;
			return false;
		}
		ctx.mem.allocationDebugHint(current);
		//put the information in the scalar-section
		ctx.mem.putInt(intfLoc, base, ctx.mem.getAddrAsInt(current.intf.outputLocation, 0)); //TODO check this out, and check why not allocateArray is used?!?
		for (i = 1; i < cnt; i++)
			ctx.mem.putInt(intfLoc, base + i * 4, current.map[i].relOff);
		//set the owner
		ctx.arch.putRef(intfLoc, ctx.rteSIMowner, current.intf.outputLocation, 0);
		//everything ok
		current.outputLocation = intfLoc;
		current.outputGenerated = true;
		return true;
	}
}
