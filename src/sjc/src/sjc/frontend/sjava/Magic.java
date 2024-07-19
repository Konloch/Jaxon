/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2015, 2016 Stefan Frenz
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

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;
import sjc.frontend.ExVal;

/**
 * Magic: all the magic functionality used for system programming
 *
 * @author S. Frenz
 * @version 160818 added support for assembler text inling
 * version 150922 added support for inlineCodeAddress
 * version 121029 added error message for too many parameters in mthdOff-call
 * version 120925 added support for expression printer
 * version 120228 cleaned up "import sjc." typo
 * version 120227 cleaned up "package sjc." typo
 * version 110626 ordering static inits regarding type hierarchy
 * version 101218 adopted changed Pack/Unit
 * version 101103 respecting write once semantic in genOutputBtsMem
 * version 101015 adopted changed Expr
 * version 100923 enabled access to compressed image state variables even if no compressed image existing (then showing not-existing-values)
 * version 100906 made resolveClsDest re-resolvable
 * version 100623 enabled re-resolving
 * version 100605 fixed check in toXArray
 * version 100602 fixed comprImage check in access to comprRelocation
 * version 100429 split clssDesc into clssDesc and intfDesc
 * version 100428 added support for interface parameter in cast2Ref, fixed MA_ACCSD and interface-check in clssDesc
 * version 100401 got ignore and stopBlockCoding from Marker, added assign
 * version 100115 adopted codeStart-movement
 * version 100114 reorganized constant object handling
 * version 100113 added support for constant references
 * version 091209 added import of moved ExVal
 * version 091102 adopted changed storage of codeblocks
 * version 091021 adopted changed modifier declarations
 * version 091013 adopted changed method signature of genStore*
 * version 091005 adopted changed Expr
 * version 091004 replaced specialValue by relocation, fixed inlineVarOffset after memory interface changes
 * version 091003 replaced ramStart by ramLoc
 * version 091001 adopted changed memory interface, added support for embConstRAM-flag
 * version 090724 adopted changed Expr
 * version 090718 added support for non-static final variables, adopted changed Expr
 * version 090625 beautified error message for genInlineBlock
 * version 090619 adopted changed Architecture
 * version 090616 adopted changed ExVar and ExCall
 * version 090409 added archSpecialValue and comprArchSpecialValue
 * version 090303 adopted changed osio package structure
 * version 090228 added support for getNamedString
 * version 090207 added copyright notice and optimized assign structure to make use of changed Architecture
 * version 081021 added support for different memory locations in IOs
 * version 080622 added support for static code initialization, changed semantics of MAGIC.inlineVarOffset
 * version 080614 removed data-searching from inlineBlock-resolving (enough during genOutput)
 * version 080613 adopted hasEffect->effectType, added inlineBlock
 * version 080405 removed setStruct (with cast2Struct it is not needed anymore)
 * version 080402 moved final type check for 32 bit cast2Obj/Struct to genOutputCast2ObjStruct
 * version 080401 added cast2Struct as special case of cast2Obj
 * version 080331 moved final type check for 32 bit setStruct to genOutputSetStruct
 * version 080119 adopted changed signature of Expr.canGenAddr
 * version 070929 allowing cast2Ref for STRUCTs
 * version 070912 added inlineOffset
 * version 070909 optimized signature of Expr.resolve
 * version 070829 cleaned up magicType for variables, added toByte/CharArray
 * version 070816 added checks for constant parameter in MAGIC.addr
 * version 070727 adopted changed type of id from PureID to String
 * version 070703 added getConstMemorySize
 * version 070628 adopted changes for "Expr extends TypeRef", fixed arrDim-checking
 * version 070617 fixed multiple instruction inlines, added inline16
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070527 added support for inline array allocation
 * version 070505 adopted change in Architecture
 * version 070331 added special variable indirScalars and support for getInstIndirScalarSize
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061231 prevention of automatic method inlining if inline code is used
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061129 static TypeRef object moved dynamically to Context
 * version 061128 added support for embedded mode
 * version 061107 fixed setStruct to work with SSA
 * version 061102 added setStruct
 * version 060818 added compressedImageBase
 * version 060707 added inline32
 * version 060629 added several information-variables
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public class Magic extends CtxBasedConfig
{
	private final static String ERR_MONSTRP = "MAGIC.mthdOff needs string containing class as first parameter";
	private final static String ERR_XDNSTRP = "MAGIC.XDesc needs only one string containing name of class/interface";
	private final static String ERR_STRCLSS = "used MAGIC needs only one string containing name of class";
	
	private final static int M_INVALID = -1;
	//private final static int M_NONE    =  0; //this is implicitly set by expr-constructor
	//used for calls
	private final static int M_INLINE8 = 1;
	private final static int M_INLINE16 = 2;
	private final static int M_INLINE32 = 3;
	private final static int M_INLINEBK = 4;
	private final static int M_WMEM64 = 5;
	private final static int M_RMEM64 = 6;
	private final static int M_WMEM32 = 7;
	private final static int M_RMEM32 = 8;
	private final static int M_WMEM16 = 9;
	private final static int M_RMEM16 = 10;
	private final static int M_WMEM8 = 11;
	private final static int M_RMEM8 = 12;
	private final static int M_WIOS64 = 13;
	private final static int M_RIOS64 = 14;
	private final static int M_WIOS32 = 15;
	private final static int M_RIOS32 = 16;
	private final static int M_WIOS16 = 17;
	private final static int M_RIOS16 = 18;
	private final static int M_WIOS8 = 19;
	private final static int M_RIOS8 = 20;
	private final static int M_CST2REF = 21;
	private final static int M_CST2OBJ = 22;
	private final static int M_ADDR = 23;
	private final static int M_CLINDESC = 24;
	private final static int M_MTHDOFF = 25;
	private final static int M_GETCDOF = 26;
	private final static int M_I_SCLSZ = 27;
	private final static int M_I_ISCSZ = 28;
	private final static int M_I_RLCEN = 29;
	private final static int M_BITMEM8 = 30;
	private final static int M_BITMEM16 = 31;
	private final static int M_BITMEM32 = 32;
	private final static int M_BITMEM64 = 33;
	private final static int M_BITIOS8 = 34;
	private final static int M_BITIOS16 = 35;
	private final static int M_BITIOS32 = 36;
	private final static int M_BITIOS64 = 37;
	private final static int M_GETRAMAD = 38;
	private final static int M_GETRAMSZ = 39;
	private final static int M_GETRINAD = 40;
	private final static int M_USEASTHS = 41;
	private final static int M_GETCNSTM = 42;
	private final static int M_INLOFFST = 43;
	private final static int M_INLCODAD = 44;
	private final static int M_STATINIT = 45;
	private final static int M_IGNORE = 46;
	private final static int M_STOPCODE = 47;
	private final static int M_ASSIGN = 48;
	
	private final static String ID_ASSIGNCALL = "assignCall";
	private final static String ID_ASSIGNHEAPCALL = "assignHeapCall";
	private final static String ID_COMPRESSEDIMAGEBASE = "compressedImageBase";
	private final static String ID_COMPRRELOCATION = "comprRelocation";
	private final static String ID_EMBCONSTRAM = "embConstRAM";
	private final static String ID_EMBEDDED = "embedded";
	private final static String ID_IMAGEBASE = "imageBase";
	private final static String ID_INDIRSCALARS = "indirScalars";
	private final static String ID_MOVABLE = "movable";
	private final static String ID_PTRSIZE = "ptrSize";
	private final static String ID_RELOCATION = "relocation";
	private final static String ID_RUNTIMEBOUNDEXCEPTION = "runtimeBoundException";
	private final static String ID_RUNTIMENULLEXCEPTION = "runtimeNullException";
	private final static String ID_STREAMLINE = "streamline";
	
	private final int relocBytes;
	private final Vrbl ptrSizeVrbl;
	private final ExVal ptrSizeVal;
	private final Vrbl movableVrbl;
	private final ExVal movableVal;
	private final Vrbl indirScalarsVrbl;
	private final ExVal indirScalarsVal;
	private final Vrbl streamVrbl;
	private final ExVal streamVal;
	private final Vrbl assignVrbl;
	private final ExVal assignVal;
	private final Vrbl assignHeapVrbl;
	private final ExVal assignHeapVal;
	private final Vrbl rtBoundExcVrbl;
	private final ExVal rtBoundExcVal;
	private final Vrbl rtNullExcVrbl;
	private final ExVal rtNullExcVal;
	private final Vrbl imgBaseVrbl;
	private final ExVal imgBaseVal;
	private final Vrbl comprImgBaseVrbl;
	private final ExVal comprImgBaseVal;
	private final Vrbl embeddedVrbl;
	private final ExVal embeddedVal;
	private final Vrbl embConstRAMVrbl;
	private final ExVal embConstRAMVal;
	private final Vrbl relocationVrbl;
	private final ExVal relocationVal;
	private final Vrbl comprRelocationVrbl;
	private final ExVal comprRelocationVal;
	
	protected Magic(Context ctx)
	{
		relocBytes = ctx.arch.relocBytes;
		//create dummy entry for ptrSize
		ptrSizeVal = new ExVal(-1, -1, -1);
		ptrSizeVal.intValue = relocBytes;
		ptrSizeVrbl = new Vrbl(ID_PTRSIZE, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		ptrSizeVrbl.init = ptrSizeVal;
		ptrSizeVrbl.location = AccVar.L_CONST;
		//create dummy entry for movable
		movableVal = new ExVal(-1, -1, -1);
		if (ctx.dynaMem)
			movableVal.intValue = 1; //else: already initialized to 0
		movableVrbl = new Vrbl(ID_MOVABLE, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		movableVrbl.init = movableVal;
		movableVrbl.location = AccVar.L_CONST;
		//create dummy entry for indirScalars
		indirScalarsVal = new ExVal(-1, -1, -1);
		if (ctx.indirScalars)
			indirScalarsVal.intValue = 1; //else: already initialized to 0
		indirScalarsVrbl = new Vrbl(ID_INDIRSCALARS, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		indirScalarsVrbl.init = indirScalarsVal;
		indirScalarsVrbl.location = AccVar.L_CONST;
		//create dummy entry for streamline
		streamVal = new ExVal(-1, -1, -1);
		if (ctx.mem.streamObjects)
			streamVal.intValue = 1; //else: already initialized to 0
		streamVrbl = new Vrbl(ID_STREAMLINE, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		streamVrbl.init = streamVal;
		streamVrbl.location = AccVar.L_CONST;
		//create dummy entry for assignCall
		assignVal = new ExVal(-1, -1, -1);
		if (ctx.assignCall)
			assignVal.intValue = 1; //else: already initialized to 0
		assignVrbl = new Vrbl(ID_ASSIGNCALL, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		assignVrbl.init = assignVal;
		assignVrbl.location = AccVar.L_CONST;
		//create dummy entry for assignHeapCall
		assignHeapVal = new ExVal(-1, -1, -1);
		if (ctx.assignHeapCall)
			assignHeapVal.intValue = 1; //else: already initialized to 0
		assignHeapVrbl = new Vrbl(ID_ASSIGNHEAPCALL, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		assignHeapVrbl.init = assignHeapVal;
		assignHeapVrbl.location = AccVar.L_CONST;
		//create dummy entry for runtimeBoundException
		rtBoundExcVal = new ExVal(-1, -1, -1);
		if (ctx.runtimeBound)
			rtBoundExcVal.intValue = 1; //else: already initialized to 0
		rtBoundExcVrbl = new Vrbl(ID_RUNTIMEBOUNDEXCEPTION, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		rtBoundExcVrbl.init = rtBoundExcVal;
		rtBoundExcVrbl.location = AccVar.L_CONST;
		//create dummy entry for runtimeNullException
		rtNullExcVal = new ExVal(-1, -1, -1);
		if (ctx.runtimeNull)
			rtNullExcVal.intValue = 1; //else: already initialized to 0
		rtNullExcVrbl = new Vrbl(ID_RUNTIMENULLEXCEPTION, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		rtNullExcVrbl.init = rtNullExcVal;
		rtNullExcVrbl.location = AccVar.L_CONST;
		//create dummy entry for imageBase
		imgBaseVal = new ExVal(-1, -1, -1);
		imgBaseVal.intValue = ctx.mem.getBaseAddress();
		imgBaseVrbl = new Vrbl(ID_IMAGEBASE, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		imgBaseVrbl.init = imgBaseVal;
		imgBaseVrbl.location = AccVar.L_CONST;
		//if there is a compressed image, create dummy entry for compressedImageBase
		comprImgBaseVal = new ExVal(-1, -1, -1);
		comprImgBaseVal.intValue = ctx.compressedImage != null ? ctx.compressedImage.baseAddress : -1; //address -1 is reserved for "not existing"
		comprImgBaseVrbl = new Vrbl(ID_COMPRESSEDIMAGEBASE, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		comprImgBaseVrbl.init = comprImgBaseVal;
		comprImgBaseVrbl.location = AccVar.L_CONST;
		//create dummy entry for embedded
		embeddedVal = new ExVal(-1, -1, -1);
		if (ctx.embedded)
			embeddedVal.intValue = 1; //else: already initialized to 0
		embeddedVrbl = new Vrbl(ID_EMBEDDED, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		embeddedVrbl.init = embeddedVal;
		embeddedVrbl.location = AccVar.L_CONST;
		//create dummy entry for embConstRAM
		embConstRAMVal = new ExVal(-1, -1, -1);
		if (ctx.embConstRAM)
			embConstRAMVal.intValue = 1; //else: already initialized to 0
		embConstRAMVrbl = new Vrbl(ID_EMBCONSTRAM, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		embConstRAMVrbl.init = embConstRAMVal;
		embConstRAMVrbl.location = AccVar.L_CONST;
		//create dummy entry for relocation
		relocationVal = new ExVal(-1, -1, -1);
		relocationVal.intValue = ctx.relocateOption;
		relocationVrbl = new Vrbl(ID_RELOCATION, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		relocationVrbl.init = relocationVal;
		relocationVrbl.location = AccVar.L_CONST;
		//if there is a compressed image, create dummy entry for comprRelocation
		comprRelocationVal = new ExVal(-1, -1, -1);
		comprRelocationVal.intValue = ctx.compressedRelocateOption; //defaults to 0 if not used
		comprRelocationVrbl = new Vrbl(ID_COMPRRELOCATION, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT, -1, -1, -1);
		comprRelocationVrbl.init = comprRelocationVal;
		comprRelocationVrbl.location = AccVar.L_CONST;
	}
	
	public void printExpression(Expr ex, CodePrinter prnt)
	{
		ExCall call;
		if (ex instanceof ExVar)
			prnt.magcVar(((ExVar) ex).dest);
		else if (ex instanceof ExCall)
		{
			call = (ExCall) ex;
			prnt.magcCall(call.id, call.par);
		}
		else if (ex instanceof ExConstStruct)
			prnt.exprConstStruct((ExConstStruct) ex);
		else if (ex instanceof ExArrayInit)
			prnt.exprArrayInit(ex, ((ExArrayInit) ex).par);
		else if (ex instanceof ExStr)
			prnt.exprString(((ExStr) ex).value);
		else
			prnt.reportError(ex, "not yet supported MAGIC");
	}
	
	protected Expr resolve(Expr ex, Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		ExCall call;
		ExVar var;
		Expr ret;
		String name;
		
		if (ex instanceof ExVar)
		{
			var = (ExVar) ex;
			name = var.id;
			if (name.equals(ID_PTRSIZE))
			{
				var.baseType = StdTypes.T_INT;
				var.dest = ptrSizeVrbl;
			}
			else if (name.equals(ID_MOVABLE))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = movableVrbl;
			}
			else if (name.equals(ID_INDIRSCALARS))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = indirScalarsVrbl;
			}
			else if (name.equals(ID_STREAMLINE))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = streamVrbl;
			}
			else if (name.equals(ID_ASSIGNCALL))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = assignVrbl;
			}
			else if (name.equals(ID_ASSIGNHEAPCALL))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = assignHeapVrbl;
			}
			else if (name.equals(ID_RUNTIMEBOUNDEXCEPTION))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = rtBoundExcVrbl;
			}
			else if (name.equals(ID_RUNTIMENULLEXCEPTION))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = rtNullExcVrbl;
			}
			else if (name.equals(ID_IMAGEBASE))
			{
				var.baseType = StdTypes.T_INT;
				var.dest = imgBaseVrbl;
			}
			else if (name.equals(ID_COMPRESSEDIMAGEBASE))
			{
				var.baseType = StdTypes.T_INT;
				var.dest = comprImgBaseVrbl;
			}
			else if (name.equals(ID_EMBEDDED))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = embeddedVrbl;
			}
			else if (name.equals(ID_EMBCONSTRAM))
			{
				var.baseType = StdTypes.T_BOOL;
				var.dest = embConstRAMVrbl;
			}
			else if (name.equals(ID_RELOCATION))
			{
				var.baseType = StdTypes.T_INT;
				var.dest = relocationVrbl;
			}
			else if (name.equals(ID_COMPRRELOCATION))
			{
				var.baseType = StdTypes.T_INT;
				var.dest = comprRelocationVrbl;
			}
			else
			{
				ex.printPos(ctx, "unknown MAGIC-variable ");
				ctx.out.print(name);
				return null;
			}
			var.constType = StdTypes.T_INT; //all variables are constant
		}
		else if (ex instanceof ExCall)
		{
			(call = (ExCall) ex).magicType = M_INVALID; //will be set anew if all parameters are sane
			name = call.id;
			if (name.equals("inline"))
			{
				if (!resolveInline(StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLINE8;
				mthdContext.marker |= Marks.K_NINL; //avoid automatic inlining
			}
			else if (name.equals("inline16"))
			{
				if (!resolveInline(StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLINE16;
				mthdContext.marker |= Marks.K_NINL; //avoid automatic inlining
			}
			else if (name.equals("inline32"))
			{
				if (!resolveInline(StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLINE32;
				mthdContext.marker |= Marks.K_NINL; //avoid automatic inlining
			}
			else if (name.equals("inlineBlock"))
			{
				if (!resolveInlineBlock(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLINEBK;
				mthdContext.marker |= Marks.K_NINL;
			}
			else if (name.equals("wMem64"))
			{
				if (!resolveWMem(false, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WMEM64;
			}
			else if (name.equals("rMem64"))
			{
				if (!resolveRMem(false, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RMEM64;
			}
			else if (name.equals("wMem32"))
			{
				if (!resolveWMem(false, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WMEM32;
			}
			else if (name.equals("rMem32"))
			{
				if (!resolveRMem(false, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RMEM32;
			}
			else if (name.equals("wMem16"))
			{
				if (!resolveWMem(false, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WMEM16;
			}
			else if (name.equals("rMem16"))
			{
				if (!resolveRMem(false, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RMEM16;
			}
			else if (name.equals("wMem8"))
			{
				if (!resolveWMem(false, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WMEM8;
			}
			else if (name.equals("rMem8"))
			{
				if (!resolveRMem(false, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RMEM8;
			}
			else if (name.equals("wIOs64"))
			{
				if (!resolveWMem(true, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WIOS64;
			}
			else if (name.equals("rIOs64"))
			{
				if (!resolveRMem(true, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RIOS64;
			}
			else if (name.equals("wIOs32"))
			{
				if (!resolveWMem(true, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WIOS32;
			}
			else if (name.equals("rIOs32"))
			{
				if (!resolveRMem(true, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RIOS32;
			}
			else if (name.equals("wIOs16"))
			{
				if (!resolveWMem(true, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WIOS16;
			}
			else if (name.equals("rIOs16"))
			{
				if (!resolveRMem(true, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RIOS16;
			}
			else if (name.equals("wIOs8"))
			{
				if (!resolveWMem(true, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_WIOS8;
			}
			else if (name.equals("rIOs8"))
			{
				if (!resolveRMem(true, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_RIOS8;
			}
			else if (name.equals("cast2Ref"))
			{
				if (!resolveCast2Ref(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_CST2REF;
			}
			else if (name.equals("cast2Obj"))
			{
				if (resolveCast2ObjStruct(true, call, unitContext, mthdContext, resolveFlags, preferredType, ctx) == null)
					return null;
				call.magicType = M_CST2OBJ;
			}
			else if (name.equals("addr"))
			{
				if (!resolveAddr(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_ADDR;
			}
			else if (name.equals("clssDesc"))
			{
				if (!resolveClssIntfDesc(false, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_CLINDESC;
			}
			else if (name.equals("intfDesc"))
			{
				if (!resolveClssIntfDesc(true, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_CLINDESC;
			}
			else if (name.equals("mthdOff"))
			{
				if (!resolveMthdOff(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_MTHDOFF;
			}
			else if (name.equals("getCodeOff"))
			{
				if (!resolveNoParam("getCodeOff", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_GETCDOF;
			}
			else if (name.equals("getInstScalarSize"))
			{
				if (!resolveClsDest(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_I_SCLSZ;
			}
			else if (name.equals("getInstIndirScalarSize"))
			{
				if (!resolveClsDest(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_I_ISCSZ;
			}
			else if (name.equals("getInstRelocEntries"))
			{
				if (!resolveClsDest(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_I_RLCEN;
			}
			else if (name.equals("bitMem8"))
			{
				if (!resolveBtsMem(false, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITMEM8;
			}
			else if (name.equals("bitMem16"))
			{
				if (!resolveBtsMem(false, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITMEM16;
			}
			else if (name.equals("bitMem32"))
			{
				if (!resolveBtsMem(false, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITMEM32;
			}
			else if (name.equals("bitMem64"))
			{
				if (!resolveBtsMem(false, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITMEM64;
			}
			else if (name.equals("bitIOs8"))
			{
				if (!resolveBtsMem(true, StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITIOS8;
			}
			else if (name.equals("bitIOs16"))
			{
				if (!resolveBtsMem(true, StdTypes.T_SHRT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITIOS16;
			}
			else if (name.equals("bitIOs32"))
			{
				if (!resolveBtsMem(true, StdTypes.T_INT, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITIOS32;
			}
			else if (name.equals("bitIOs64"))
			{
				if (!resolveBtsMem(true, StdTypes.T_LONG, call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_BITIOS64;
			}
			else if (name.equals("cast2Struct"))
			{
				if ((ret = resolveCast2ObjStruct(false, call, unitContext, mthdContext, resolveFlags, preferredType, ctx)) == null)
					return null;
				if (ret != call)
					return ret;
				call.magicType = M_CST2OBJ;
			}
			else if (name.equals("getRamAddr"))
			{
				if (!resolveNoParam("getRamAddr", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_GETRAMAD;
			}
			else if (name.equals("getRamSize"))
			{
				if (!resolveNoParam("getRamSize", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_GETRAMSZ;
			}
			else if (name.equals("getRamInitAddr"))
			{
				if (!resolveNoParam("getRamInitAddr", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_GETRINAD;
			}
			else if (name.equals("useAsThis"))
			{
				if (!resolveUseAsThis(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_USEASTHS;
			}
			else if (name.equals("getConstMemorySize"))
			{
				if (!resolveNoParam("getConstMemorySize", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_GETCNSTM;
			}
			else if (name.equals("inlineOffset"))
			{
				if (!resolveInlineOffset(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLOFFST;
			}
			else if (name.equals("inlineCodeAddress"))
			{
				if (!resolveInlineCodeAddress(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_INLCODAD;
			}
			else if (name.equals("doStaticInit"))
			{
				if (!resolveDoStaticInit(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_STATINIT;
			}
			else if (name.equals("toByteArray"))
			{ //replaces right side of MAGIC
				return resolveStringToArray(StdTypes.T_BYTE, call, unitContext, mthdContext, resolveFlags, ctx);
			}
			else if (name.equals("toCharArray"))
			{ //replaces right side of MAGIC
				return resolveStringToArray(StdTypes.T_CHAR, call, unitContext, mthdContext, resolveFlags, ctx);
			}
			else if (name.equals("getNamedString"))
			{ //replaces right side of MAGIC
				return resolveGetNamedString(call, unitContext, mthdContext, resolveFlags, ctx);
			}
			else if (name.equals("ignore"))
			{
				call.magicType = M_IGNORE; //just ignore
				call.baseType = StdTypes.T_VOID;
				call.effectType = Expr.EF_NORM;
			}
			else if (name.equals("stopBlockCoding"))
			{
				if (!resolveNoParam("stopBlockCoding", call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_STOPCODE;
				call.effectType = StExpr.EF_STOP;
			}
			else if (name.equals("assign"))
			{
				if (!resolveAssign(call, unitContext, mthdContext, resolveFlags, ctx))
					return null;
				call.magicType = M_ASSIGN;
			}
			else
			{
				ex.printPos(ctx, "unknown MAGIC-method ");
				ctx.out.print(name);
				return null;
			}
		}
		else
		{
			ex.printPos(ctx, "unknown MAGIC-deref");
			return null;
		}
		return ex; //default: unmodified expression
	}
	
	protected void genOutput(int reg, Expr ex, Context ctx)
	{
		ExCall call;
		
		if (ex instanceof ExCall)
			switch ((call = (ExCall) ex).magicType)
			{
				case M_INLINE8:
					genOutputInline(StdTypes.T_BYTE, call, ctx);
					return;
				case M_INLINE16:
					genOutputInline(StdTypes.T_SHRT, call, ctx);
					return;
				case M_INLINE32:
					genOutputInline(StdTypes.T_INT, call, ctx);
					return;
				case M_INLINEBK:
					genOutputInlineBlock(call, ctx);
					return;
				case M_WMEM64:
					genOutputWMem(false, StdTypes.T_LONG, call, ctx);
					return;
				case M_RMEM64:
					genOutputRMem(false, reg, StdTypes.T_LONG, call, ctx);
					return;
				case M_WMEM32:
					genOutputWMem(false, StdTypes.T_INT, call, ctx);
					return;
				case M_RMEM32:
					genOutputRMem(false, reg, StdTypes.T_INT, call, ctx);
					return;
				case M_WMEM16:
					genOutputWMem(false, StdTypes.T_SHRT, call, ctx);
					return;
				case M_RMEM16:
					genOutputRMem(false, reg, StdTypes.T_SHRT, call, ctx);
					return;
				case M_WMEM8:
					genOutputWMem(false, StdTypes.T_BYTE, call, ctx);
					return;
				case M_RMEM8:
					genOutputRMem(false, reg, StdTypes.T_BYTE, call, ctx);
					return;
				case M_WIOS64:
					genOutputWMem(true, StdTypes.T_LONG, call, ctx);
					return;
				case M_RIOS64:
					genOutputRMem(true, reg, StdTypes.T_LONG, call, ctx);
					return;
				case M_WIOS32:
					genOutputWMem(true, StdTypes.T_INT, call, ctx);
					return;
				case M_RIOS32:
					genOutputRMem(true, reg, StdTypes.T_INT, call, ctx);
					return;
				case M_WIOS16:
					genOutputWMem(true, StdTypes.T_SHRT, call, ctx);
					return;
				case M_RIOS16:
					genOutputRMem(true, reg, StdTypes.T_SHRT, call, ctx);
					return;
				case M_WIOS8:
					genOutputWMem(true, StdTypes.T_BYTE, call, ctx);
					return;
				case M_RIOS8:
					genOutputRMem(true, reg, StdTypes.T_BYTE, call, ctx);
					return;
				case M_CST2REF:
					genOutputCast2Ref(reg, call, ctx);
					return;
				case M_CST2OBJ:
					genOutputCast2ObjStruct(reg, call, ctx);
					return;
				case M_ADDR:
					genOutputAddr(reg, call, ctx);
					return;
				case M_CLINDESC:
					genOutputClssDesc(reg, call, ctx);
					return;
				case M_MTHDOFF:
					genOutputMthdOff(reg, call, ctx);
					return;
				case M_GETCDOF:
					genOutputGetCodeOff(reg, ctx);
					return;
				case M_I_SCLSZ:  //do the same as M_I_RLCEN
				case M_I_ISCSZ:  //do the same as M_I_RLCEN
				case M_I_RLCEN:
					genOutputUnitProp(call.magicType, reg, call, ctx);
					return;
				case M_BITMEM8:
					genOutputBtsMem(false, StdTypes.T_BYTE, call, ctx);
					return;
				case M_BITMEM16:
					genOutputBtsMem(false, StdTypes.T_SHRT, call, ctx);
					return;
				case M_BITMEM32:
					genOutputBtsMem(false, StdTypes.T_INT, call, ctx);
					return;
				case M_BITMEM64:
					genOutputBtsMem(false, StdTypes.T_LONG, call, ctx);
					return;
				case M_BITIOS8:
					genOutputBtsMem(true, StdTypes.T_BYTE, call, ctx);
					return;
				case M_BITIOS16:
					genOutputBtsMem(true, StdTypes.T_SHRT, call, ctx);
					return;
				case M_BITIOS32:
					genOutputBtsMem(true, StdTypes.T_INT, call, ctx);
					return;
				case M_BITIOS64:
					genOutputBtsMem(true, StdTypes.T_LONG, call, ctx);
					return;
				case M_GETRAMAD:
					genOutputGetRamAddr(reg, ctx);
					return;
				case M_GETRAMSZ:
					genOutputGetRamSize(reg, ctx);
					return;
				case M_GETRINAD:
					genOutputGetRamInitAddr(reg, ctx);
					return;
				case M_USEASTHS:
					genOutputUseAsThis(call, ctx);
					return;
				case M_GETCNSTM:
					genOutputGetConstMemorySize(reg, ctx);
					return;
				case M_INLOFFST:
					genOutputInlineOffset(call, ctx);
					return;
				case M_INLCODAD:
					genOutputInlineCodeAddress(call, ctx);
					return;
				case M_STATINIT:
					genOutputDoStaticInit(call, ctx);
					return;
				case M_IGNORE:
					return; //nothing to code
				case M_STOPCODE:
					return; //nothing to code
				case M_ASSIGN:
					genAssign(call, ctx);
					return;
			}
		//not a special call to MAGIC (was replaced in resolveX)
		ex.genOutputVal(reg, ctx);
	}
	
	private boolean resolveInline(int type, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve all parameters, check if all parameters are constant integers (optionally: first parameter is string)
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.inlineX without parameter");
			return false;
		}
		if (pa.expr instanceof ExStr)
			pa = pa.nextParam; //skip over first parameter if it is a constant string
		while (pa != null)
		{
			paEx = pa.expr;
			if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
				return false;
			if (!paEx.isIntType() || paEx.calcConstantType(ctx) != StdTypes.T_INT)
			{
				paEx.printPos(ctx, "MAGIC.inlineX needs constant integer values");
				return false;
			}
			if (type == StdTypes.T_BYTE)
			{
				if ((paEx.getConstIntValue(ctx) & 0xFFFFFF00) != 0)
				{
					paEx.printPos(ctx, "MAGIC.inline with constant too large for byte (use inline32 or decrease)");
					return false;
				}
			}
			else if (type == StdTypes.T_SHRT)
			{
				if ((paEx.getConstIntValue(ctx) & 0xFFFF0000) != 0)
				{
					paEx.printPos(ctx, "MAGIC.inline with constant too large for short (use inline32 or decrease)");
					return false;
				}
			}
			pa = pa.nextParam;
		}
		//everything ok, set type
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveInlineBlock(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		
		//resolve all parameters, check if all parameters are constant integers
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.inlineBlock needs name of imported data");
			return false;
		}
		if (!(pa.expr instanceof ExStr) || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.inlineBlock needs one constant parameter with name of imported data");
			return false;
		}
		//block is searched during genOutput to avoid error message on never accessed datablocks
		//everything ok, set type
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveWMem(boolean isIO, int type, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve all parameters, check if all parameters are constant integers
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.wMemX needs address");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (relocBytes == 2)
		{
			if (!paEx.isIntType() && !paEx.isShortType())
			{
				call.printPos(ctx, "MAGIC.wMemX needs int or short value as destination");
				return false;
			}
		}
		else if (relocBytes == 4)
		{
			if (!paEx.isIntType())
			{
				call.printPos(ctx, "MAGIC.wMemX needs int value as destination");
				return false;
			}
		}
		else if (relocBytes == 8)
		{
			if (!paEx.isIntType() && !paEx.isLongType())
			{
				call.printPos(ctx, "MAGIC.wMemX needs int or long value as destination");
				return false;
			}
		}
		else
		{
			call.printPos(ctx, "MAGIC.wMemX could not determine size of pointer");
			return false;
		}
		pa = pa.nextParam;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.wMemX needs value");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (paEx.baseType != type || paEx.arrDim > 0)
		{
			paEx.printPos(ctx, "incompatible type in MAGIC.wMemX (");
			ctx.out.print(paEx.baseType);
			ctx.out.print("/");
			ctx.out.print(type);
			ctx.out.print(")");
			return false;
		}
		if (pa.nextParam != null)
		{
			if (!isIO)
			{
				call.printPos(ctx, "MAGIC.wMemX needs only two parameters");
				return false;
			}
			pa = pa.nextParam;
			paEx = pa.expr;
			if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
				return false;
			if (!paEx.isIntType() || paEx.calcConstantType(ctx) != StdTypes.T_INT)
			{
				call.printPos(ctx, "MAGIC.wMemX allows only constant third parameter");
				return false;
			}
			if (pa.nextParam != null)
			{
				call.printPos(ctx, "MAGIC.wMemX needs not more than three parameters");
				return false;
			}
		}
		//everything ok, set type
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveRMem(boolean isIO, int type, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve all parameters, check if all parameters are constant integers
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.rMemX needs address");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (relocBytes == 2)
		{
			if (!paEx.isIntType() && !paEx.isShortType())
			{
				call.printPos(ctx, "MAGIC.rMemX needs int or short value as destination");
				return false;
			}
		}
		else if (relocBytes == 4)
		{
			if (!paEx.isIntType())
			{
				call.printPos(ctx, "MAGIC.rMemX needs int value as destination");
				return false;
			}
		}
		else if (relocBytes == 8)
		{
			if (!paEx.isIntType() && !paEx.isLongType())
			{
				call.printPos(ctx, "MAGIC.rMemX needs int or long value as destination");
				return false;
			}
		}
		else
		{
			call.printPos(ctx, "MAGIC.rMemX could not determine size of pointer");
			return false;
		}
		if (pa.nextParam != null)
		{
			if (!isIO)
			{
				call.printPos(ctx, "MAGIC.rMemX needs only one parameter");
				return false;
			}
			pa = pa.nextParam;
			paEx = pa.expr;
			if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
				return false;
			if (!paEx.isIntType() || paEx.calcConstantType(ctx) != StdTypes.T_INT)
			{
				call.printPos(ctx, "MAGIC.rMemX allows only constant second parameter");
				return false;
			}
			if (pa.nextParam != null)
			{
				call.printPos(ctx, "MAGIC.rMemX needs not more than two parameters");
				return false;
			}
		}
		//everything ok, set type
		call.baseType = type;
		return true;
	}
	
	private boolean resolveCast2Ref(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa == null || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.cast2Ref needs object-parameter");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!paEx.isObjType() && !paEx.isIntfType() && !paEx.isStructType())
		{
			call.printPos(ctx, "MAGIC.cast2Ref needs object/struct parameter");
			return false;
		}
		//everthing ok, set type corresponding to architecture
		if (relocBytes == 2)
			call.baseType = StdTypes.T_SHRT;
		else if (relocBytes == 4)
			call.baseType = StdTypes.T_INT;
		else if (relocBytes == 8)
			call.baseType = StdTypes.T_LONG;
		else
		{
			call.printPos(ctx, "MAGIC.cast2Ref could not determine size of pointer");
			return false;
		}
		return true;
	}
	
	private Expr resolveCast2ObjStruct(boolean obj, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa == null || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.cast2Obj/Struct needs address");
			return null;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return null;
		if (relocBytes == 2)
		{
			if (!paEx.isIntType() && !paEx.isShortType())
			{
				call.printPos(ctx, "MAGIC.cast2Obj/Struct needs int or short value as destination");
				return null;
			}
		}
		else if (relocBytes == 4)
		{
			if (!paEx.isIntType() && !paEx.isLongType())
			{ //long type will be checked in genOutputCast2ObjStruct
				call.printPos(ctx, "MAGIC.cast2Obj/Struct needs int pointer for this architecture");
				return null;
			}
		}
		else if (relocBytes == 8)
		{
			if (!paEx.isIntType() && !paEx.isLongType())
			{
				call.printPos(ctx, "MAGIC.cast2Obj/Struct needs int or long pointer for this architecture");
				return null;
			}
		}
		else
		{
			call.printPos(ctx, "MAGIC.cast2Obj/Struct could not determine size of pointer");
			return null;
		}
		//everthing ok
		//if struct, check if address is constant integer so expression can be handled as constant object
		if (!obj && paEx.calcConstantType(ctx) == StdTypes.T_INT)
		{
			paEx = new ExConstStruct(paEx.getConstIntValue(ctx), call.fileID, call.line, call.col);
			if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, preferredType, ctx))
				return null;
			return paEx;
		}
		//not constant, set type and return original call object
		call.baseType = TypeRef.T_QID;
		call.qid = obj ? ctx.objectType.qid : ctx.structClass.getQIDTo();
		return call;
	}
	
	private boolean resolveAddr(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa == null || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.addr needs object-parameter");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags & ~Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!paEx.canGenAddr(unitContext, true, resolveFlags, ctx) || paEx.isCompInitConstObject(ctx))
		{
			call.printPos(ctx, "MAGIC.addr not allowed for constant parameter");
			return false;
		}
		//everthing ok, set type corresponding to architecture
		if (relocBytes == 2)
			call.baseType = StdTypes.T_SHRT;
		else if (relocBytes == 4)
			call.baseType = StdTypes.T_INT;
		else if (relocBytes == 8)
			call.baseType = StdTypes.T_LONG;
		else
		{
			call.printPos(ctx, "MAGIC.addr could not determine size of pointer");
			return false;
		}
		return true;
	}
	
	private boolean resolveClssIntfDesc(boolean isIntf, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		ExClssMthdName clss;
		String name;
		
		//resolve parameter;
		pa = call.par;
		if (pa == null || pa.nextParam != null || (paEx = pa.expr) == null)
		{
			call.printPos(ctx, ERR_XDNSTRP);
			return false;
		}
		if (paEx instanceof ExStr)
		{ //first resolve
			name = ((ExStr) paEx).value;
			clss = new ExClssMthdName(name, null, true, paEx.fileID, paEx.line, paEx.col);
		}
		else if (paEx instanceof ExClssMthdName)
			clss = (ExClssMthdName) paEx; //re-resolve
		else
		{
			call.printPos(ctx, ERR_XDNSTRP);
			return false;
		}
		if (!clss.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		call.par.expr = clss;
		//everthing ok, set type
		call.baseType = TypeRef.T_QID;
		if (isIntf)
		{
			if ((clss.destUnit.modifier & Modifier.M_INDIR) == 0)
			{
				call.printPos(ctx, "MAGIC.intfDesc only allowed for interfaces");
				return false;
			}
			call.qid = ctx.intfType.qid;
		}
		else
		{
			if ((clss.destUnit.modifier & Modifier.M_INDIR) != 0)
			{
				call.printPos(ctx, "MAGIC.clssDesc only allowed for classes");
				return false;
			}
			call.qid = ctx.clssType.qid;
		}
		clss.destUnit.modifier |= Modifier.MA_ACCSSD;
		return true;
	}
	
	private boolean resolveMthdOff(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		String unit, mthd;
		
		//resolve parameter;
		pa = call.par;
		if (pa == null || (paEx = pa.expr) == null)
		{
			call.printPos(ctx, ERR_MONSTRP);
			return false;
		}
		if (paEx instanceof ExStr)
		{ //first resolve
			unit = ((ExStr) paEx).value;
			pa = pa.nextParam;
			if (pa == null || (paEx = pa.expr) == null || !(paEx instanceof ExStr))
			{
				call.printPos(ctx, "MAGIC.mthdOff needs second string containing method as last parameter");
				return false;
			}
			mthd = ((ExStr) paEx).value;
			call.par.expr = paEx = new ExClssMthdName(unit, mthd, false, paEx.fileID, paEx.line, paEx.col);
		}
		else if (paEx instanceof ExClssMthdName)
			((ExClssMthdName) paEx).destMthd = null; //re-resolve: reset
		else
		{
			call.printPos(ctx, ERR_MONSTRP);
			return false;
		}
		if (pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.mthdOff call with too many parameters");
			return false;
		}
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		//everthing ok, type is always int, discard no more needed parameter
		call.par.nextParam = null;
		call.baseType = StdTypes.T_INT;
		return true;
	}
	
	private boolean resolveNoParam(String cmd, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		//check
		if (call.par != null)
		{
			call.printPos(ctx, "MAGIC.");
			ctx.out.print(cmd);
			ctx.out.print(" needs no parameters");
			return false;
		}
		//everthing ok, set type
		call.baseType = StdTypes.T_INT;
		return true;
	}
	
	private boolean resolveClsDest(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		String name;
		
		pa = call.par;
		if (pa == null || pa.nextParam != null || (paEx = pa.expr) == null)
		{
			call.printPos(ctx, ERR_STRCLSS);
			return false;
		}
		if (paEx instanceof ExStr)
		{ //first resolve otherwise already instance of ExClssMthdName
			name = ((ExStr) paEx).value;
			paEx = new ExClssMthdName(name, null, false, paEx.fileID, paEx.line, paEx.col);
		}
		else if (!(paEx instanceof ExClssMthdName))
		{
			call.printPos(ctx, ERR_STRCLSS);
			return false;
		}
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		call.par.expr = paEx;
		//everthing ok, set type
		call.baseType = StdTypes.T_INT;
		return true;
	}
	
	private boolean resolveBtsMem(boolean isIO, int type, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve all parameters, check if all parameters are constant integers
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.btsMemX needs address");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (relocBytes == 2)
		{
			if (!paEx.isIntType() && !paEx.isShortType())
			{
				call.printPos(ctx, "MAGIC.btsMemX needs int or short value as destination");
				return false;
			}
		}
		else if (relocBytes == 4)
		{
			if (!paEx.isIntType())
			{
				call.printPos(ctx, "MAGIC.btsMemX needs int value as destination");
				return false;
			}
		}
		else if (relocBytes == 8)
		{
			if (!paEx.isIntType() && !paEx.isLongType())
			{
				call.printPos(ctx, "MAGIC.btsMemX needs int or long value as destination");
				return false;
			}
		}
		else
		{
			call.printPos(ctx, "MAGIC.btsMemX could not determine size of pointer");
			return false;
		}
		pa = pa.nextParam;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.btsMemX needs value");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (paEx.baseType != type || paEx.arrDim > 0)
		{
			paEx.printPos(ctx, "incompatible type in MAGIC.btsMemX (");
			ctx.out.print(paEx.baseType);
			ctx.out.print("/");
			ctx.out.print(type);
			ctx.out.print(")");
			return false;
		}
		pa = pa.nextParam;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.btsMemX needs set/clear");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!paEx.isBoolType())
		{
			call.printPos(ctx, "MAGIC.btsMemX needs boolean set/clear");
			return false;
		}
		if (pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.btsMemX needs only three parameters");
			return false;
		}
		//everything ok, set type
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveUseAsThis(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa == null || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.useAsThis needs object-parameter");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!paEx.isObjType())
		{
			call.printPos(ctx, "MAGIC.useAsThis needs parameter of type Object");
			return false;
		}
		if (pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.useAsThis needs only two parameters");
			return false;
		}
		//check mthdContext
		if (!mthdContext.isConstructor || (mthdContext.modifier & Modifier.M_EXINIT) != 0)
		{
			call.printPos(ctx, "MAGIC.inlineArrayInit only once allowed in constructor");
			return false;
		}
		mthdContext.modifier |= Modifier.M_EXINIT;
		//everything ok, set type and mark
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveInlineOffset(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.inlineOffset needs byte count");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (paEx.calcConstantType(ctx) != StdTypes.T_INT)
		{
			call.printPos(ctx, "MAGIC.inlineOffset needs constant inlineMode");
			return false;
		}
		if ((pa = pa.nextParam) == null)
		{
			call.printPos(ctx, "MAGIC.inlineOffset needs variable");
			return false;
		}
		paEx = pa.expr;
		if (!paEx.resolve(unitContext, mthdContext, resolveFlags & ~Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!(paEx instanceof ExVar) || !paEx.canGenAddr(unitContext, true, resolveFlags, ctx))
		{
			call.printPos(ctx, "MAGIC.inlineOffset needs non-constant variable");
			return false;
		}
		if ((pa = pa.nextParam) != null && pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.inlineOffset needs two or three parameters");
			return false;
		}
		if (pa != null)
		{ //resolve optional offset
			if (!(paEx = pa.expr).resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
				return false;
			if (paEx.calcConstantType(ctx) != StdTypes.T_INT)
			{
				call.printPos(ctx, "optional offset for MAGIC.inlineOffset has to be constant");
				return false;
			}
		}
		//everything ok, set type and mark
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveInlineCodeAddress(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx;
		
		//resolve parameter
		pa = call.par;
		if (pa != null)
		{
			paEx = pa.expr;
			if (!paEx.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
				return false;
			if (paEx.calcConstantType(ctx) != StdTypes.T_INT)
			{
				call.printPos(ctx, "MAGIC.inlineCodeAddress needs constant offset");
				return false;
			}
			if (pa.nextParam != null)
			{
				call.printPos(ctx, "MAGIC.inlineCodeAddress needs none or one parameter");
				return false;
			}
		}
		//everything ok, set type and mark
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	private boolean resolveDoStaticInit(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		Mthd m, lm, tm, tmp;
		boolean changedOrder;
		
		//check parameter
		if (call.par != null)
		{
			call.printPos(ctx, "MAGIC.doStaticInit needs no parameters");
			return false;
		}
		//adjust statistics and import references if required
		if (ctx.dynaMem)
			call.dest = unitContext.initStat; //remember unitContext for unit references
		//insert references to all units with static initialization and check type hierarchy sorted order
		lm = null;
		m = ctx.staticInitMthds;
		while (m != null)
		{
			if (ctx.dynaMem)
				unitContext.getRefUnit(m.owner, true); //just prepare, don't remember referenced unit
			mthdContext.stmtCnt += m.stmtCnt; //update statistics
			if (ctx.verbose)
			{ //give verbose information if requested
				ctx.out.print("inserting class initialization of ");
				m.owner.printNameWithOuter(ctx.out);
				ctx.out.print(" in ");
				unitContext.printNameWithOuter(ctx.out);
				ctx.out.print('.');
				mthdContext.printNamePar(ctx.out);
				ctx.out.println();
			}
			//check order and set next method
			tm = m;
			changedOrder = false;
			while (tm.nextMthd != null)
			{
				if (tm.nextMthd.owner.isParent(m.owner, ctx))
				{ //tm.nextMthd must be before m, but is after => re-order
					tmp = tm.nextMthd; //get method of parent
					tm.nextMthd = tmp.nextMthd; //snip method of parent out of list
					tmp.nextMthd = m; //next method of parent is owner of current method
					if (lm == null)
						ctx.staticInitMthds = tmp; //no previous method, insert at start of list
					else
						lm.nextMthd = tmp; //set as next method of last method right before current method
					m = tmp; //continue with method of parent
					changedOrder = true; //do not step onto next method
				}
				else
					tm = tm.nextMthd; //checked method is not parent, check next one
			}
			if (!changedOrder)
			{
				lm = m;
				m = m.nextMthd;
			}
		}
		//everything ok, set type and mark several targets
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		mthdContext.marker |= Marks.K_NINL;
		ctx.staticInitDone = true;
		return true;
	}
	
	private Expr resolveStringToArray(int type, ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa, newPa;
		String source;
		boolean trailingZero;
		ExVar replacement;
		ExVal val;
		ExArrayInit init;
		int i, fid, line, col;
		
		pa = call.par;
		if (pa == null || !(pa.expr instanceof ExStr))
		{
			call.printPos(ctx, "MAGIC.toXArray needs constant String as first parameter");
			return null;
		}
		source = ((ExStr) pa.expr).value;
		pa = pa.nextParam;
		if (pa == null || !pa.expr.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, ctx.stringType, ctx) || pa.expr.calcConstantType(ctx) != StdTypes.T_INT || pa.expr.baseType != StdTypes.T_BOOL || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.toXArray needs constant boolean as second (i.e. last) parameter");
			return null;
		}
		trailingZero = pa.expr.getConstIntValue(ctx) != 0;
		//everything ok, insert new variable in unit and return replacement variable
		fid = call.fileID;
		line = call.line;
		col = call.col;
		replacement = new ExVar("$MAGIC", fid, line, col);
		init = new ExArrayInit(fid, line, col);
		replacement.arrDim = 1;
		replacement.baseType = type;
		pa = null;
		for (i = 0; i < source.length(); i++)
		{
			newPa = new FilledParam(val = new ExVal(fid, line, col), fid, line, col);
			val.baseType = type;
			val.intValue = source.charAt(i);
			if (pa == null)
				init.par = newPa;
			else
				pa.nextParam = newPa;
			pa = newPa;
		}
		if (trailingZero)
		{
			newPa = new FilledParam(val = new ExVal(fid, line, col), fid, line, col);
			val.baseType = type;
			val.intValue = 0;
			if (pa == null)
				init.par = newPa;
			else
				pa.nextParam = newPa;
		}
		if (!init.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
		{
			ctx.out.print(" in magically created initArray");
			return null;
		}
		replacement.dest = init.dest;
		replacement.constObject = true;
		return replacement;
	}
	
	private Expr resolveGetNamedString(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		byte[] data;
		String source;
		ExVal nullVal;
		Expr replacement;
		int fid, line, col;
		
		if ((pa = call.par) == null || !(pa.expr instanceof ExStr) || pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.getNamedString needs constant String as first and only parameter");
			return null;
		}
		source = ((ExStr) pa.expr).value;
		//everything ok, insert constant string return replacement variable
		fid = call.fileID;
		line = call.line;
		col = call.col;
		if ((data = ctx.osio.readFile(source)) == null)
		{ //named object not found
			nullVal = new ExVal(fid, line, col);
			nullVal.baseType = StdTypes.T_NNPT;
			replacement = nullVal;
		}
		else
			(replacement = new ExStr(new String(data), fid, line, col)) //build string from named object
					.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx); //and resolve it
		return replacement;
	}
	
	private boolean resolveAssign(ExCall call, Unit unitContext, Mthd mthdContext, int resolveFlags, Context ctx)
	{
		FilledParam pa;
		Expr paEx1, paEx2;
		
		//resolve all parameters, check if all parameters are constant integers
		pa = call.par;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.assign needs destination");
			return false;
		}
		paEx1 = pa.expr;
		if (!paEx1.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (!paEx1.canGenAddr(unitContext, true, resolveFlags, ctx) || paEx1.isCompInitConstObject(ctx))
		{
			call.printPos(ctx, "MAGIC.assign not allowed for constant first parameter");
			return false;
		}
		pa = pa.nextParam;
		if (pa == null)
		{
			call.printPos(ctx, "MAGIC.assign needs value");
			return false;
		}
		paEx2 = pa.expr;
		if (!paEx2.resolve(unitContext, mthdContext, resolveFlags | Expr.RF_CHECKREAD, null, ctx))
			return false;
		if (pa.nextParam != null)
		{
			call.printPos(ctx, "MAGIC.assign needs not more than two parameters");
			return false;
		}
		if (paEx1.compareType(paEx2, false, ctx) != TypeRef.C_EQ)
		{
			call.printPos(ctx, "need identical types for MAGIC.assign");
			return false;
		}
		//everything ok, set type
		call.baseType = TypeRef.T_VOID;
		call.effectType = Expr.EF_NORM;
		return true;
	}
	
	//internal genOutput-methods
	private void genOutputInline(int type, ExCall call, Context ctx)
	{
		Architecture arch = ctx.arch;
		FilledParam pa;
		Instruction instr = null;
		int cnt = 0, value, size = 4;
		
		switch (type)
		{
			case StdTypes.T_BYTE:
				size = 1;
				break;
			case StdTypes.T_SHRT:
				size = 2;
				break;
			//already initialized: INT: size=4;
		}
		pa = call.par;
		if ((pa.expr instanceof ExStr))
		{
			if (arch.supportsAsmTextInline)
			{ //architecture support asmText, insert text instead of opcodes
				instr = arch.getUnlinkedInstruction();
				arch.appendInstruction(instr);
				instr.type = Architecture.I_MAGC;
				instr.asmText = ((ExStr) pa.expr).value;
				pa = pa.nextParam;
				if (pa == null)
					instr.size = size;
				else
				{
					instr.size = 0;
					while (pa != null)
					{
						instr.size += size;
						pa = pa.nextParam;
					}
				}
				return;
			}
			pa = pa.nextParam; //architecture does not support asmText, skip string parameter
		}
		while (pa != null)
		{
			if (cnt + size >= arch.maxInstrCodeSize)
			{
				instr.size = cnt;
				instr = null;
			}
			if (instr == null)
			{
				instr = arch.getUnlinkedInstruction();
				arch.appendInstruction(instr);
				instr.type = Architecture.I_MAGC;
				cnt = 0;
			}
			value = pa.expr.getConstIntValue(ctx);
			instr.code[cnt++] = (byte) value;
			if (size > 1)
			{
				instr.code[cnt++] = (byte) (value >>> 8);
				if (size > 2)
				{
					instr.code[cnt++] = (byte) (value >>> 16);
					instr.code[cnt++] = (byte) (value >>> 24);
				}
			}
			pa = pa.nextParam;
		}
		if (instr != null)
			instr.size = cnt; //fix size of last instruction
	}
	
	private void genOutputInlineBlock(ExCall call, Context ctx)
	{
		Architecture arch = ctx.arch;
		FilledParam pa;
		Instruction instr = null;
		int cnt = 0, ind = 0, len;
		String name;
		DataBlockList list;
		byte[] code;
		
		//search block and insert its bytes
		pa = call.par;
		name = ((ExStr) pa.expr).value;
		list = ctx.codeBlocks;
		while (list != null)
		{
			if (name.equals(list.name))
				break;
			list = list.nextDataBlock;
		}
		if (list == null)
		{
			call.printPos(ctx, "could not find code-block named ");
			ctx.out.println(name);
			ctx.err = true;
			return;
		}
		code = list.data;
		len = code.length;
		while (ind < len)
		{
			if (cnt + 1 >= arch.maxInstrCodeSize)
			{
				instr.size = cnt;
				instr = null;
			}
			if (instr == null)
			{
				instr = arch.getUnlinkedInstruction();
				arch.appendInstruction(instr);
				instr.type = Architecture.I_MAGC;
				cnt = 0;
			}
			instr.code[cnt++] = code[ind++];
		}
		if (instr != null)
			instr.size = cnt; //fix size of last instruction
	}
	
	private void genOutputWMem(boolean isIO, int type, ExCall call, Context ctx)
	{
		FilledParam pa;
		int addr, addrValType, addrVal, val, restoreAddr, restoreAddrVal, restoreVal, ioMemLoc = 0;
		int addrConstType, valConstType;
		Expr addrEx, valEx;
		
		//prepare access to expressions, pa points to optional third parameter
		addrEx = (pa = call.par).expr;
		addrValType = addrEx.getRegType(ctx);
		valEx = (pa = pa.nextParam).expr;
		//try to optimize access to memory if address/value is constant
		if (!isIO && ((addrConstType = addrEx.calcConstantType(ctx)) != 0 | (valConstType = valEx.calcConstantType(ctx)) != 0))
		{
			if (addrConstType == StdTypes.T_INT)
			{
				if (valConstType == StdTypes.T_INT)
				{ //both addr and val are constant, value is 8/16/32 bit
					ctx.arch.genStoreVarConstVal(0, null, addrEx.getConstIntValue(ctx), valEx.getConstIntValue(ctx), type);
					return;
				}
				if (valConstType == StdTypes.T_LONG)
				{ //both addr and val are constant, value is 64 bit
					ctx.arch.genStoreVarConstDoubleOrLongVal(0, null, addrEx.getConstIntValue(ctx), valEx.getConstLongValue(ctx), type == StdTypes.T_DBL);
					return;
				}
				//addr is constant, but value is not constant
				restoreVal = ctx.arch.prepareFreeReg(0, 0, 0, type);
				val = ctx.arch.allocReg();
				valEx.genOutputVal(val, ctx);
				ctx.arch.genStoreVarVal(0, null, addrEx.getConstIntValue(ctx), val, type);
				ctx.arch.deallocRestoreReg(val, 0, restoreVal);
				return;
			}
			//addr is not constant or not a 32 bit value
			if (valConstType == StdTypes.T_INT || valConstType == StdTypes.T_LONG)
			{ //addr is not constant, value is constant
				restoreAddr = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
				addr = ctx.arch.allocReg();
				restoreAddrVal = ctx.arch.prepareFreeReg(0, 0, addr, addrValType);
				addrVal = ctx.arch.allocReg();
				addrEx.genOutputVal(addrVal, ctx);
				ctx.arch.genConvertVal(addr, addrVal, StdTypes.T_PTR, addrValType);
				ctx.arch.deallocRestoreReg(addrVal, addr, restoreAddrVal);
				if (valConstType == StdTypes.T_INT)
					ctx.arch.genStoreVarConstVal(addr, null, 0, valEx.getConstIntValue(ctx), type);
				else
					ctx.arch.genStoreVarConstDoubleOrLongVal(addr, null, 0, valEx.getConstLongValue(ctx), type == StdTypes.T_DBL);
				ctx.arch.deallocRestoreReg(addr, 0, restoreAddr);
				return;
			}
			//unsupported combination of constant values
		}
		//normal code generation with registers for address and value
		restoreAddr = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		restoreAddrVal = ctx.arch.prepareFreeReg(0, 0, addr, addrValType);
		addrVal = ctx.arch.allocReg();
		addrEx.genOutputVal(addrVal, ctx);
		ctx.arch.genConvertVal(addr, addrVal, StdTypes.T_PTR, addrValType);
		ctx.arch.deallocRestoreReg(addrVal, addr, restoreAddrVal);
		restoreVal = ctx.arch.prepareFreeReg(addr, 0, 0, type);
		val = ctx.arch.allocReg();
		valEx.genOutputVal(val, ctx);
		if (isIO)
		{
			if (pa.nextParam != null)
				ioMemLoc = pa.nextParam.expr.getConstIntValue(ctx);
			ctx.arch.genWriteIO(addr, val, type, ioMemLoc);
		}
		else
			ctx.arch.genAssign(addr, val, type);
		ctx.arch.deallocRestoreReg(val, 0, restoreVal);
		ctx.arch.deallocRestoreReg(addr, 0, restoreAddr);
	}
	
	private void genOutputRMem(boolean isIO, int reg, int type, ExCall call, Context ctx)
	{
		FilledParam pa;
		int addr, addrType, addrEx, restoreEx, restore, ioMemLoc = 0;
		
		restore = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		pa = call.par;
		addrType = pa.expr.getRegType(ctx);
		restoreEx = ctx.arch.prepareFreeReg(0, 0, addr, addrType);
		addrEx = ctx.arch.allocReg();
		pa.expr.genOutputVal(addrEx, ctx);
		ctx.arch.genConvertVal(addr, addrEx, StdTypes.T_PTR, addrType);
		ctx.arch.deallocRestoreReg(addrEx, addr, restoreEx);
		if (isIO)
		{
			if (pa.nextParam != null)
				ioMemLoc = pa.nextParam.expr.getConstIntValue(ctx);
			ctx.arch.genReadIO(reg, addr, type, ioMemLoc);
		}
		else
			ctx.arch.genLoadVarVal(reg, addr, null, 0, type);
		ctx.arch.deallocRestoreReg(addr, reg, restore);
	}
	
	private void genOutputCast2Ref(int reg, ExCall call, Context ctx)
	{
		FilledParam pa;
		int addrEx, restore;
		
		pa = call.par;
		restore = ctx.arch.prepareFreeReg(0, 0, reg, pa.expr.isIntfType() ? StdTypes.T_DPTR : StdTypes.T_PTR);
		addrEx = ctx.arch.allocReg();
		pa.expr.genOutputVal(addrEx, ctx); //will generate an int or long depending on architecture, nothing else needed
		if (relocBytes == 2)
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_SHRT, StdTypes.T_PTR);
		else if (relocBytes == 4)
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_INT, StdTypes.T_PTR);
		else
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_LONG, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(addrEx, reg, restore);
	}
	
	private void genOutputCast2ObjStruct(int reg, ExCall call, Context ctx)
	{
		FilledParam pa;
		int valType, valEx, restore;
		
		pa = call.par;
		if ((valType = pa.expr.getRegType(ctx)) != StdTypes.T_INT && relocBytes == 4)
		{ //post-check if invalid type is used
			pa.expr.printPos(ctx, "MAGIC.cast2Struct needs int value as location (can not generate output)");
			ctx.err = true;
		}
		else
		{
			restore = ctx.arch.prepareFreeReg(0, 0, reg, valType);
			valEx = ctx.arch.allocReg();
			pa.expr.genOutputVal(valEx, ctx); //will generate short/int/long depending on architecture, nothing else needed
			ctx.arch.genConvertVal(reg, valEx, StdTypes.T_PTR, valType);
			ctx.arch.deallocRestoreReg(valEx, reg, restore);
		}
	}
	
	private void genOutputAddr(int reg, ExCall call, Context ctx)
	{
		FilledParam pa;
		int addrEx, restore;
		
		pa = call.par;
		restore = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
		addrEx = ctx.arch.allocReg();
		pa.expr.genOutputAddr(addrEx, ctx); //will generate an int or long depending on architecture, nothing else needed
		if (relocBytes == 2)
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_SHRT, StdTypes.T_PTR);
		else if (relocBytes == 4)
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_INT, StdTypes.T_PTR);
		else
			ctx.arch.genConvertVal(reg, addrEx, StdTypes.T_LONG, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(addrEx, reg, restore);
	}
	
	private void genOutputClssDesc(int reg, ExCall call, Context ctx)
	{
		ExClssMthdName exc;
		exc = (ExClssMthdName) call.par.expr;
		if (ctx.dynaMem)
			ctx.arch.genLoadUnitContext(reg, exc.importedClass.relOff);
		else
			ctx.arch.genLoadConstUnitContext(reg, exc.destUnit.outputLocation);
	}
	
	private void genOutputMthdOff(int reg, ExCall call, Context ctx)
	{
		ExClssMthdName exc;
		exc = (ExClssMthdName) call.par.expr;
		ctx.arch.genLoadConstVal(reg, exc.destMthd.relOff, StdTypes.T_INT);
	}
	
	private void genOutputGetCodeOff(int reg, Context ctx)
	{
		ctx.arch.genLoadConstVal(reg, ctx.codeStart, StdTypes.T_INT);
	}
	
	private void genOutputUnitProp(int dest, int reg, ExCall call, Context ctx)
	{
		Unit unit;
		int val;
		
		unit = ((ExClssMthdName) call.par.expr).destUnit;
		switch (dest)
		{
			case M_I_SCLSZ:
				val = unit.instScalarTableSize;
				break;
			case M_I_ISCSZ:
				val = unit.instIndirScalarTableSize;
				break;
			case M_I_RLCEN:
				val = unit.instRelocTableEntries;
				break;
			default:
				call.compErr(ctx, "### unknown dest in genOutputUnitProp");
				return;
		}
		ctx.arch.genLoadConstVal(reg, val, StdTypes.T_INT);
	}
	
	private void genOutputBtsMem(boolean isIO, int type, ExCall call, Context ctx)
	{
		FilledParam pa;
		Instruction doAnd, doOr, ariDone;
		int addr, addrEx, addrType, mask, preVal, val, notMask, restoreEx, restore1, restore2, restore3, restore4, restore5, maskConst;
		boolean doNot = true;
		
		restore1 = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		pa = call.par;
		addrType = pa.expr.getRegType(ctx);
		restoreEx = ctx.arch.prepareFreeReg(0, 0, addr, addrType);
		addrEx = ctx.arch.allocReg();
		pa.expr.genOutputVal(addrEx, ctx);
		ctx.arch.genConvertVal(addr, addrEx, StdTypes.T_PTR, addrType);
		ctx.arch.deallocRestoreReg(addrEx, addr, restoreEx);
		pa = pa.nextParam;
		restore2 = ctx.arch.prepareFreeReg(addr, 0, 0, type);
		mask = ctx.arch.allocReg();
		if ((maskConst = pa.expr.calcConstantType(ctx)) != 0 && pa.nextParam.expr.calcConstantType(ctx) == StdTypes.T_INT && pa.nextParam.expr.getConstIntValue(ctx) == 0)
		{
			switch (maskConst)
			{
				case StdTypes.T_INT:
					ctx.arch.genLoadConstVal(mask, ~pa.expr.getConstIntValue(ctx), type);
					break;
				case StdTypes.T_LONG:
					ctx.arch.genLoadConstDoubleOrLongVal(mask, ~pa.expr.getConstLongValue(ctx), false);
					break;
				default:
					call.compErr(ctx, "### unknown const type in Magic.genOutputBtsMem");
					return;
			}
			doNot = false; //"cpl"-ari for mask already done without code
		}
		else
			pa.expr.genOutputVal(mask, ctx);
		restore3 = ctx.arch.prepareFreeReg(addr, mask, 0, type);
		preVal = ctx.arch.allocReg();
		if (isIO)
			ctx.arch.genReadIO(preVal, addr, type, 0);
		else
			ctx.arch.genLoadVarVal(preVal, addr, null, 0, type);
		pa = pa.nextParam;
		restore4 = ctx.arch.prepareFreeReg(addr, mask, preVal, type);
		val = ctx.arch.allocReg();
		if (pa.expr.calcConstantType(ctx) == StdTypes.T_INT)
		{ //constant clear/set
			if (pa.expr.getConstIntValue(ctx) == 0)
			{ //clear -> not+and
				if (doNot)
				{
					restore5 = ctx.arch.prepareFreeReg(val, preVal, mask, type);
					notMask = ctx.arch.allocReg();
					ctx.arch.genUnaOp(notMask, mask, (Ops.S_ARI << 16) | Ops.A_CPL, type);
					ctx.arch.genBinOp(val, preVal, notMask, (Ops.S_ARI << 16) | Ops.A_AND, type);
					ctx.arch.deallocRestoreReg(notMask, mask, restore5);
				}
				else
					ctx.arch.genBinOp(val, preVal, mask, (Ops.S_ARI << 16) | Ops.A_AND, type);
			}
			else
			{ //set -> or
				ctx.arch.genBinOp(val, preVal, mask, (Ops.S_ARI << 16) | Ops.A_OR, type);
			}
		}
		else
		{ //mini-if to check or/and
			doAnd = ctx.arch.getUnlinkedInstruction();
			doOr = ctx.arch.getUnlinkedInstruction();
			ariDone = ctx.arch.getUnlinkedInstruction();
			pa.expr.genOutputCondJmp(doAnd, false, doOr, ctx); //jump if false -> clear -> not+and
			ctx.arch.appendInstruction(doOr); //not jumped -> true -> set -> or
			ctx.arch.genBinOp(val, preVal, mask, (Ops.S_ARI << 16) | Ops.A_OR, type);
			ctx.arch.genJmp(ariDone);
			ctx.arch.appendInstruction(doAnd); //jump destination for not+and
			restore5 = ctx.arch.prepareFreeReg(val, preVal, mask, type);
			notMask = ctx.arch.allocReg();
			ctx.arch.genUnaOp(notMask, mask, (Ops.S_ARI << 16) | Ops.A_CPL, type);
			ctx.arch.genBinOp(val, preVal, notMask, (Ops.S_ARI << 16) | Ops.A_AND, type);
			ctx.arch.deallocRestoreReg(notMask, mask, restore5);
			ctx.arch.appendInstruction(ariDone); //value adjusted
		}
		if (isIO)
			ctx.arch.genWriteIO(addr, val, type, 0);
		else
			ctx.arch.genAssign(addr, val, type);
		ctx.arch.deallocRestoreReg(val, preVal, restore4);
		ctx.arch.deallocRestoreReg(preVal, 0, restore3);
		ctx.arch.deallocRestoreReg(mask, 0, restore2);
		ctx.arch.deallocRestoreReg(addr, 0, restore1);
	}
	
	private void genOutputGetRamAddr(int reg, Context ctx)
	{
		ctx.arch.genLoadConstVal(reg, ctx.mem.getAddrAsInt(ctx.ramLoc, 0), StdTypes.T_INT);
	}
	
	private void genOutputGetRamSize(int reg, Context ctx)
	{
		ctx.arch.genLoadConstVal(reg, ctx.ramSize, StdTypes.T_INT);
	}
	
	private void genOutputGetRamInitAddr(int reg, Context ctx)
	{
		ctx.arch.genLoadConstVal(reg, ctx.mem.getAddrAsInt(ctx.ramInitLoc, 0), StdTypes.T_INT);
	}
	
	private void genOutputUseAsThis(ExCall call, Context ctx)
	{
		int reg, restore;
		
		restore = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		reg = ctx.arch.allocReg();
		call.par.expr.genOutputVal(reg, ctx);
		ctx.arch.genLoadInstContext(reg);
		ctx.arch.deallocRestoreReg(reg, 0, restore);
	}
	
	private void genOutputGetConstMemorySize(int reg, Context ctx)
	{
		ctx.arch.genLoadConstVal(reg, ctx.constMemorySize, StdTypes.T_INT);
	}
	
	private void genOutputInlineOffset(ExCall call, Context ctx)
	{
		int inlineMode, objReg, baseValue;
		FilledParam pa;
		AccVar var;
		Object obj = null;
		
		inlineMode = (pa = call.par).expr.getConstIntValue(ctx);
		var = ((ExVar) pa.nextParam.expr).dest;
		switch (var.location)
		{
			case AccVar.L_LOCAL:
			case AccVar.L_PARAM:
				objReg = ctx.arch.regBase;
				break;
			case AccVar.L_CLSSSCL:
			case AccVar.L_CLSSREL:
				if (ctx.dynaMem)
					objReg = ctx.arch.regClss;
				else
				{
					obj = var.owner.outputLocation;
					objReg = 0;
				}
				break;
			case AccVar.L_INSTSCL:
			case AccVar.L_INSTIDS:
			case AccVar.L_INSTREL:
				objReg = ctx.arch.regInst;
				break;
			default:
				call.printPos(ctx, "invalid variable type in MAGIC.inlineOffset");
				return;
		}
		baseValue = (pa = pa.nextParam.nextParam) != null ? pa.expr.getConstIntValue(ctx) : 0; //optional additional offset
		ctx.arch.inlineVarOffset(inlineMode, objReg, obj, var.relOff, baseValue);
	}
	
	private void genOutputInlineCodeAddress(ExCall call, Context ctx)
	{
		FilledParam pa;
		
		if ((pa = call.par) == null)
			ctx.arch.inlineCodeAddress(true, 0);
		else
			ctx.arch.inlineCodeAddress(false, pa.expr.getConstIntValue(ctx));
	}
	
	private void genOutputDoStaticInit(ExCall call, Context ctx)
	{
		Mthd m;
		Unit caller = null;
		
		m = ctx.staticInitMthds;
		if (ctx.dynaMem)
			caller = call.dest.owner; //prepared in resolveDoStaticInit to remember unitContext
		while (m != null)
		{
			if (ctx.dynaMem)
			{
				ctx.arch.genSaveUnitContext();
				ctx.arch.genLoadUnitContext(ctx.arch.regClss, caller.getRefUnit(m.owner, false).relOff);
			}
			if (!ctx.arch.mayInline())
			{
				ctx.out.print("could not inline static code initialization for ");
				ctx.out.println(m.owner.name);
				ctx.err = true;
			}
			else
				m.genInlineOutput(ctx);
			if (ctx.dynaMem)
			{
				ctx.arch.genRestUnitContext();
			}
			m = m.nextMthd;
		}
	}
	
	private void genAssign(ExCall call, Context ctx)
	{
		call.par.expr.genOutputAssignTo(0, call.par.nextParam.expr, ctx);
	}
}
