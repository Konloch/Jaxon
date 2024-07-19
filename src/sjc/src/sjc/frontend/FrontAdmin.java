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

package sjc.frontend;

import sjc.compbase.*;
import sjc.frontend.binimp.BinImp;
import sjc.frontend.clist.CList;
import sjc.frontend.prepobj.PrepObj;
import sjc.frontend.sjava.SJava;
import sjc.osio.TextPrinter;

/**
 * FrontAdmin: handling of language dependend frontend
 *
 * @author S. Frenz
 * @version 160818 moved enterStartupInfo to Context and therefore also moved startUnit and startMthd there
 * version 150907 added precheckLangEnvironment
 * version 120228 cleaned up "import sjc." typo
 * version 120227 cleaned up "package sjc." typo
 * version 110624 adopted changed Context
 * version 110607 moved buildStringList to StringList
 * version 101220 added support for noSyncCalls
 * version 101115 changed "smf" to "sjc" in supported file list
 * version 101021 added support for assert
 * version 100813 removed internal check
 * version 100428 added support for Context.rteSIntfParents
 * version 100426 renamed _r_clssType of SArray into _r_unitType
 * version 100424 removed support for "return missing"
 * version 100411 beautified warning message (annotation- instead of DEFINE-hint)
 * version 100127 adopted renaming of *ariCall into *binAriCall and adding of unaAriCall
 * version 100115 adopted codeStart-movement
 * version 091209 added support for PrepObj
 * version 091208 added support for arrayDeepCopy
 * version 091112 added check of null-name in checkMthd
 * version 091021 adopted changed modifier declarations
 * version 091005 added support for prepared TypeRef for boolean and int
 * version 091001 adopted changed memory interface, fixed signature check for newInstance in alternateObjNew-mode
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090626 added support for stack extreme check error method
 * version 090625 beautified error message for possibly missing DEFINE-head in indirect scalar mode
 * version 090619 fixed check of newInstance signature in indirect scalar mode
 * version 090506 added support for "list compiled files", adopted change in Context
 * version 090430 added support for "return missing" method
 * version 090306 added support for user settable start method
 * version 090303 adopted changed osio package structure
 * version 090226 added check for signature of runtime environment methods
 * version 090224 added check for ambiguous runtime environment methods
 * version 090218 added support for synchronized-environment
 * version 090207 added copyright notice
 * version 080622 added check insertion of code for static initialization
 * version 080616 added flow hint in compiled method rte.DynamicRuntime.doThrow to support SSAEmul
 * version 080614 adopted changed Unit.searchVariable
 * version 080613 added bim in printKnownLanguage
 * version 080604 added support for basic exception environment
 * version 080520 fixed references to different ariCall-methods for different basic types
 * version 080518 added additional types for ariCall-request
 * version 080517 adopted changes of Architecture.ariLongCall to ariCall
 * version 080202 added support for alternate newInstance and SClassDesc
 * version 080119 added setting of ctx.rteDRCheckArrayStoreMd if not disabled
 * version 080104 added setting of ctx.rteDRProfilerMd if required
 * version 071214 added setting of ctx.rteSIMnext to support chained interfaces
 * version 070816 added type-checks for java.lang.String.value
 * version 070731 adopted renaming of id to name
 * version 070730 no-overwrite of already initialized ctx.root
 * version 070727 adopted change of Mth.id from PureID to String
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070526 adopted removal of Context.rteSArray* short-paths
 * version 070523 removed invalid error output for String.count
 * version 070522 adopted additional flag Mthd.M_NDCODE, setting flags for accessed unit descriptors
 * version 070519 made String.count optional
 * version 070509 more helpful message on non-existing files
 * version 070504 changed parameter of finalizeImage
 * version 070331 added checks for header-variables if movable indirect scalars
 * version 070303 added support for movable indirect scalars
 * version 070113 adopted change in SArray-structure
 * version 070111 adapted change in printPos and compErr
 * version 070101 removed filling of method-idx
 * version 061211 integrated checkEnvironment
 * version 061203 optimized calls to printPos and compErr
 * version 061129 static TypeRef object moved dynamically to Context
 * version 061102 claryfied error message on wrong variables
 * version 060818 added support to import an internal byte array
 * version 060629 user-friendly updates
 * version 060607 initial version
 */

public class FrontAdmin
{
	private final Context ctx;
	private final Language[] langs;
	private TextPrinter filelister;
	
	public static void printKnownLanguages(TextPrinter v)
	{
		//this should correspond with the entries in langs (see init(.))
		v.println(" *.bim  - binary imported machine-code");
		v.println(" *.bib  - binary imported byte-array");
		v.println(" *.clt  - compilation list");
		v.println(" *.java - sjc-java files");
		v.println(" *.pob  - compile-time prepared objects");
	}
	
	public FrontAdmin(Context iCtx)
	{
		int i;
		
		ctx = iCtx;
		if (ctx.root == null)
			ctx.root = new Pack(null, null);
		ctx.rte = ctx.root.searchSubPackage(new StringList("rte"), true);
		
		ctx.objectType = new TypeRef(-2, 0, 0);
		ctx.objectType.baseType = TypeRef.T_QID;
		ctx.objectType.qid = new QualID(new StringList("Object"), QualID.Q_UNIT, -2, 0, 0);
		ctx.stringType = new TypeRef(-2, 0, 0);
		ctx.stringType.baseType = TypeRef.T_QID;
		ctx.stringType.qid = new QualID(new StringList("String"), QualID.Q_UNIT, -2, 0, 0);
		ctx.clssType = new TypeRef(-2, 0, 0);
		ctx.clssType.baseType = TypeRef.T_QID;
		ctx.clssType.qid = new QualID(new StringList("SClassDesc"), QualID.Q_UNIT, -2, 0, 0);
		ctx.intfType = new TypeRef(-2, 0, 0);
		ctx.intfType.baseType = TypeRef.T_QID;
		ctx.intfType.qid = new QualID(new StringList("SIntfDesc"), QualID.Q_UNIT, -2, 0, 0);
		
		ctx.boolType = new TypeRef(-2, 0, 0);
		ctx.boolType.baseType = TypeRef.T_BOOL;
		ctx.intType = new TypeRef(-2, 0, 0);
		ctx.intType.baseType = TypeRef.T_INT;
		
		//sort the languages in descending file amount
		langs = new Language[4];
		langs[0] = new SJava();
		langs[1] = new BinImp();
		langs[2] = new CList();
		langs[3] = new PrepObj();
		for (i = 0; i < langs.length; i++)
			langs[i].init(ctx);
	}
	
	public void activateListCompiledFiles(String filename)
	{
		if ((filelister = ctx.osio.getNewFilePrinter(filename)) == null)
		{
			ctx.out.print("error creating output file for compiled list file ");
			ctx.out.println(filename);
		}
	}
	
	public void scanparseFinished()
	{
		if (filelister != null)
		{
			filelister.close();
			filelister = null;
		}
	}
	
	public boolean addByteArray(byte[] data, int startoffset, int stopoffset, String name)
	{
		//only FrontAdmin knows, which entry in langs is BinImp
		return ((BinImp) langs[1]).addByteArray(data, startoffset, stopoffset, name);
	}
	
	public boolean scanparse(String what)
	{
		boolean success, recurse;
		StringList allFiles, curFile;
		
		if (what == null || what.length() < 1)
			return true;
		if (what.endsWith(":"))
		{
			what = what.substring(0, what.length() - 1);
			recurse = false;
		}
		else
			recurse = true;
		ctx.out.print("Parse ");
		if (!ctx.osio.isDir(what))
		{
			ctx.out.print("file \"");
			ctx.out.print(what);
			ctx.out.println("\"...");
			return scanparseFile(new StringList(what), false); //not a directory - check for file
		}
		//"what" is not a file but a directory, do every file
		if (recurse)
			ctx.out.print("rdir \"");
		else
			ctx.out.print("sdir \"");
		ctx.out.print(what);
		ctx.out.println("\"...");
		success = true;
		allFiles = ctx.osio.listDir(what, recurse);
		while (allFiles != null)
		{
			curFile = allFiles;
			allFiles = allFiles.next;
			curFile.next = null;
			success &= scanparseFile(curFile, true);
		}
		return success;
	}
	
	private boolean scanparseFile(StringList what, boolean mayBeIgnored)
	{
		int i;
		
		for (i = 0; i < langs.length; i++)
		{
			//check if current language wants to parse the file
			if (langs[i].fileCompetence(what.str))
			{
				//enter file in our list and set unique ID (==counter)
				ctx.addFile(what);
				//add file in filelist if requested
				if (filelister != null)
					filelister.println(what.str);
				//try to scan and parse the file
				return langs[i].scanparseFile(what);
			}
		}
		//no language for this particular file found, success depends on caller
		if (mayBeIgnored)
			return true;
		ctx.out.print("File \"");
		ctx.out.print(what.str);
		ctx.out.println("\" has no assigned language or does not exist");
		ctx.err = true;
		return false;
	}
	
	public boolean checkCompEnvironment()
	{
		boolean error = false;
		int i;
		
		error |= (ctx.langRoot = ctx.defUnits.searchUnit("Object")) == null;
		error |= (ctx.langString = checkUnit(ctx.defUnits, "String", false)) == null;
		error |= (ctx.rteSArray = checkUnit(ctx.rte, "SArray", false)) == null;
		error |= (ctx.rteSClassDesc = checkUnit(ctx.rte, "SClassDesc", true)) == null;
		error |= (ctx.rteSIntfDesc = checkUnit(ctx.rte, "SIntfDesc", true)) == null;
		error |= (ctx.rteSIntfMap = checkUnit(ctx.rte, "SIntfMap", true)) == null;
		error |= (ctx.rteSMthdBlock = checkUnit(ctx.rte, "SMthdBlock", false)) == null;
		error |= (ctx.rteDynamicRuntime = checkUnit(ctx.rte, "DynamicRuntime", false)) == null;
		for (i = 1; i < StdTypes.MAXBASETYPE + 1; i++)
			if (ctx.arch.binAriCall[i] != 0 || ctx.arch.unaAriCall[i] != 0)
			{
				error |= (ctx.rteDynamicAri = checkUnit(ctx.rte, "DynamicAri", false)) == null;
				break;
			}
		if (error)
			return false;
		if (ctx.throwUsed)
		{
			if ((ctx.excThrowable = ctx.defUnits.searchUnit("Throwable")) == null || (ctx.excChecked = ctx.defUnits.searchUnit("Exception")) == null)
			{
				ctx.out.println("to use exceptions at least declare Exception extends Throwable");
				return false;
			}
		}
		ctx.objectType.qid.unitDest = ctx.langRoot;
		ctx.stringType.qid.unitDest = ctx.langString;
		ctx.clssType.qid.unitDest = ctx.rteSClassDesc;
		ctx.intfType.qid.unitDest = ctx.rteSIntfDesc;
		return true;
	}
	
	public boolean precheckLangEnvironment()
	{ //do only basic existance-checks, offsets are not assigned yet
		Unit root;
		boolean error = false;
		
		root = ctx.langRoot;
		error |= !checkVrbl(root, "_r_type", Vrbl.L_INSTREL, 0, false);
		if (!ctx.mem.streamObjects)
		{
			if (!ctx.alternateObjNew)
			{
				error |= !checkVrbl(root, "_r_relocEntries", Vrbl.L_INSTSCL, 0, false);
				error |= !checkVrbl(root, "_r_scalarSize", Vrbl.L_INSTSCL, 0, false);
			}
			error |= !checkVrbl(root, "_r_next", Vrbl.L_INSTREL, 0, false);
		}
		if (error)
		{
			ctx.out.println("Missing variables in header of root object, required fields are (ordered):");
			ctx.out.println("   SClassDesc _r_type                : always");
			ctx.out.println("   Object _r_next                    : for not streamlined objects");
			ctx.out.println("   int _r_relocEntries, _r_scalarSize: for not streamlined objects or alternate new");
			return false;
		}
		if (ctx.indirScalars)
		{
			error |= (ctx.indirScalarSizeOff = checkVrbl(root, "_r_indirScalarSize", true)) == AccVar.INV_RELOFF;
			error |= (ctx.indirScalarAddrOff = checkVrbl(root, "_r_indirScalarAddr", true)) == AccVar.INV_RELOFF;
			if (error)
			{
				ctx.out.println("Missing indir-variables in header of root object:");
				ctx.out.println("   int _r_indirScalarSize, _r_indirScalarAddr");
				return false;
			}
		}
		return true;
	}
	
	public boolean checkLangEnvironment()
	{
		boolean error = false;
		Unit root;
		Pack kernelPck;
		StringList strTmp, strPck;
		Vrbl varTmp;
		String strUnit, strMthd;
		
		root = ctx.langRoot;
		error |= !checkVrbl(root, "_r_type", Vrbl.L_INSTREL, -ctx.arch.relocBytes, true);
		if (!ctx.mem.streamObjects)
		{
			if (!ctx.alternateObjNew)
			{
				error |= !checkVrbl(root, "_r_relocEntries", Vrbl.L_INSTSCL, 0, true);
				error |= !checkVrbl(root, "_r_scalarSize", Vrbl.L_INSTSCL, 4, true);
			}
			error |= !checkVrbl(root, "_r_next", Vrbl.L_INSTREL, -2 * ctx.arch.relocBytes, true);
		}
		if (error)
		{
			ctx.out.println("Missing variables in header of root object, required fields are (ordered):");
			ctx.out.println("   SClassDesc _r_type                : always");
			ctx.out.println("   Object _r_next                    : for not streamlined objects");
			ctx.out.println("   int _r_relocEntries, _r_scalarSize: for not streamlined objects or alternate new");
			return false;
		}
		if (ctx.indirScalars)
		{
			error |= (ctx.indirScalarSizeOff = checkVrbl(root, "_r_indirScalarSize", true)) == AccVar.INV_RELOFF;
			error |= (ctx.indirScalarAddrOff = checkVrbl(root, "_r_indirScalarAddr", true)) == AccVar.INV_RELOFF;
			if (error)
			{
				ctx.out.println("Missing indir-variables in header of root object:");
				ctx.out.println("   int _r_indirScalarSize, _r_indirScalarAddr");
				return false;
			}
		}
		
		if ((ctx.langStrVal = checkVrbl(ctx.langString, "value", true)) == AccVar.INV_RELOFF)
			error = true;
		else
		{ //additionally check size
			varTmp = ctx.langString.searchVariable("value", ctx); //existing as checked above
			if (varTmp.type.arrDim != 1 || (varTmp.modifier & (Modifier.M_STAT | Modifier.M_STRUCT)) != 0)
			{
				ctx.out.println("java.lang.String.value has to be dynamic array with dim==1");
				error = true;
			}
			else if (ctx.byteString)
			{
				if (varTmp.type.baseType != StdTypes.T_BYTE)
				{
					ctx.out.println("java.lang.String.value has to be byte[] if compiled with byteString");
					error = true;
				}
			}
			else
			{
				if (varTmp.type.baseType != StdTypes.T_CHAR)
				{
					ctx.out.println("java.lang.String.value has to be char[] if not compiled with byteString");
					error = true;
				}
			}
		}
		if ((ctx.langStrCnt = checkVrbl(ctx.langString, "count", false)) == AccVar.INV_RELOFF && ctx.langString.inlArr != null)
		{
			ctx.out.println("String.count is not optional if String is implemented with inline array");
			error = true;
		}
		error |= (ctx.rteSClassParent = checkVrbl(ctx.rteSClassDesc, "parent", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSClassImpl = checkVrbl(ctx.rteSClassDesc, "implementations", true)) == AccVar.INV_RELOFF;
		if (ctx.alternateObjNew)
		{
			error |= (ctx.rteSClassInstScalarSize = checkVrbl(ctx.rteSClassDesc, "instScalarSize", true)) == AccVar.INV_RELOFF;
			error |= (ctx.rteSClassInstRelocTableEntries = checkVrbl(ctx.rteSClassDesc, "instRelocEntries", true)) == AccVar.INV_RELOFF;
			if (ctx.indirScalars)
				error |= (ctx.rteSClassInstIndirScalarSize = checkVrbl(ctx.rteSClassDesc, "instIndirScalarSize", true)) == AccVar.INV_RELOFF;
		}
		if (ctx.genIntfParents)
			error |= (ctx.rteSIntfParents = checkVrbl(ctx.rteSIntfDesc, "parents", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSIMowner = checkVrbl(ctx.rteSIntfMap, "owner", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSIMnext = checkVrbl(ctx.rteSIntfMap, "next", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSArrayLength = checkVrbl(ctx.rteSArray, "length", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSArrayDim = checkVrbl(ctx.rteSArray, "_r_dim", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSArrayStd = checkVrbl(ctx.rteSArray, "_r_stdType", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteSArrayExt = checkVrbl(ctx.rteSArray, "_r_unitType", true)) == AccVar.INV_RELOFF;
		error |= (ctx.rteDRNewInstMd = checkMthd(ctx.rteDynamicRuntime, "newInstance", ctx.alternateObjNew ? parSize(StdTypes.T_PTR) //type
				: parSize(StdTypes.T_INT) + (ctx.indirScalars ? parSize(StdTypes.T_INT) : 0) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_PTR))) == null; //scalarSize, (indirScalarSize), relocEntries, type
		error |= (ctx.rteDRNewArrayMd = checkMthd(ctx.rteDynamicRuntime, "newArray", //length, dimension, entrySize, basicType, extType
				parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_PTR))) == null;
		error |= (ctx.rteDRNewMultArrayMd = checkMthd(ctx.rteDynamicRuntime, "newMultArray", //root-array, root-level, current level, length, dimension, entrySize, basicType, extType
				parSize(StdTypes.T_PTR) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_PTR))) == null;
		error |= (ctx.rteDRIsInstMd = checkMthd(ctx.rteDynamicRuntime, "isInstance", //object, type, asCast
				parSize(StdTypes.T_PTR) + parSize(StdTypes.T_PTR) + parSize(StdTypes.T_BOOL))) == null;
		error |= (ctx.rteDRIsImplMd = checkMthd(ctx.rteDynamicRuntime, "isImplementation", //object, type, asCast
				parSize(StdTypes.T_PTR) + parSize(StdTypes.T_PTR) + parSize(StdTypes.T_BOOL))) == null;
		error |= (ctx.rteDRIsArrayMd = checkMthd(ctx.rteDynamicRuntime, "isArray", //object, basicType, extType, dimension, asCast
				parSize(StdTypes.T_PTR) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_PTR) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_BOOL))) == null;
		if (ctx.assignCall || ctx.assignHeapCall)
			error |= (ctx.rteDRAssignMd = checkMthd(ctx.rteDynamicRuntime, "assign", //asInterface, address, intfMap, object
					parSize(StdTypes.T_BOOL) + parSize(StdTypes.T_PTR) + parSize(StdTypes.T_PTR) + parSize(StdTypes.T_PTR))) == null;
		if (ctx.doArrayStoreCheck)
			error |= (ctx.rteDRCheckArrayStoreMd = checkMthd(ctx.rteDynamicRuntime, "checkArrayStore", //address, newValue
					parSize(StdTypes.T_PTR) + parSize(StdTypes.T_PTR))) == null;
		if (ctx.runtimeBound)
			error |= (ctx.rteDRBoundExcMd = checkMthd(ctx.rteDynamicRuntime, "boundException", //object, index
					parSize(StdTypes.T_PTR) + parSize(StdTypes.T_INT))) == null;
		if (ctx.runtimeNull)
			error |= (ctx.rteDRNullExcMd = checkMthd(ctx.rteDynamicRuntime, "nullException", //no parameter
					0)) == null;
		if (ctx.profilerUsed)
		{
			if ((ctx.rteDRProfilerMd = checkMthd(ctx.rteDynamicRuntime, "profiler", //id, mode
					parSize(StdTypes.T_INT) + parSize(StdTypes.T_BYTE))) == null)
				error = true;
			else
				ctx.rteDRProfilerMd.marker |= Marks.K_NPRF;
		}
		if (ctx.stackExtremeUsed)
		{
			if ((ctx.rteDRStackExtremeErrMd = checkMthd(ctx.rteDynamicRuntime, "stackExtremeError", //no parameter
					0)) == null)
				error = true;
			else
				ctx.rteDRStackExtremeErrMd.marker |= Marks.K_NSPC;
			error |= (ctx.rteStackExtreme = checkVrbl(ctx.rteDynamicRuntime, "stackExtreme", true)) == AccVar.INV_RELOFF;
		}
		if (ctx.arch.binAriCall[StdTypes.T_CHAR] < 0 || ctx.arch.binAriCall[StdTypes.T_BOOL] < 0)
		{
			ctx.out.println("internal compiler error: invalid usage of rte calls for arithmetic operations");
			return false;
		}
		if (ctx.arch.binAriCall[StdTypes.T_BYTE] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_BYTE] = checkMthd(ctx.rteDynamicAri, "binByte", parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.binAriCall[StdTypes.T_SHRT] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_SHRT] = checkMthd(ctx.rteDynamicAri, "binShort", parSize(StdTypes.T_SHRT) + parSize(StdTypes.T_SHRT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.binAriCall[StdTypes.T_INT] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_INT] = checkMthd(ctx.rteDynamicAri, "binInt", parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.binAriCall[StdTypes.T_LONG] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_LONG] = checkMthd(ctx.rteDynamicAri, "binLong", parSize(StdTypes.T_LONG) + parSize(StdTypes.T_LONG) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.binAriCall[StdTypes.T_FLT] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_FLT] = checkMthd(ctx.rteDynamicAri, "binFloat", parSize(StdTypes.T_FLT) + parSize(StdTypes.T_FLT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.binAriCall[StdTypes.T_DBL] < 0)
			error |= (ctx.rteDABinAriCallMds[StdTypes.T_DBL] = checkMthd(ctx.rteDynamicAri, "binDouble", parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_BYTE] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_BYTE] = checkMthd(ctx.rteDynamicAri, "unaByte", parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_SHRT] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_SHRT] = checkMthd(ctx.rteDynamicAri, "unaShort", parSize(StdTypes.T_SHRT) + parSize(StdTypes.T_SHRT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_INT] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_INT] = checkMthd(ctx.rteDynamicAri, "unaInt", parSize(StdTypes.T_INT) + parSize(StdTypes.T_INT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_LONG] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_LONG] = checkMthd(ctx.rteDynamicAri, "unaLong", parSize(StdTypes.T_LONG) + parSize(StdTypes.T_LONG) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_FLT] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_FLT] = checkMthd(ctx.rteDynamicAri, "unaFloat", parSize(StdTypes.T_FLT) + parSize(StdTypes.T_FLT) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.arch.unaAriCall[StdTypes.T_DBL] < 0)
			error |= (ctx.rteDAUnaAriCallMds[StdTypes.T_DBL] = checkMthd(ctx.rteDynamicAri, "unaDouble", parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE) + parSize(StdTypes.T_BYTE))) == null;
		if (ctx.throwUsed)
		{
			if ((ctx.rteDoThrowMd = checkMthd(ctx.rteDynamicRuntime, "doThrow", //object
					parSize(StdTypes.T_PTR))) == null)
				error = true;
			else
				ctx.rteDoThrowMd.marker |= Marks.K_THRW;
			error |= (ctx.rteThrowFrame = checkVrbl(ctx.rteDynamicRuntime, "currentThrowFrame", true)) == AccVar.INV_RELOFF;
		}
		if (ctx.syncUsed && !ctx.noSyncCalls)
		{
			if ((ctx.rteDoSyncMd = checkMthd(ctx.rteDynamicRuntime, "doSynchronize", //object, enter
					parSize(StdTypes.T_PTR) + parSize(StdTypes.T_BOOL))) == null)
				error = true;
		}
		if (ctx.arrayDeepCopyUsed)
		{
			if ((ctx.rteArrayDeepCopyMd = checkMthd(ctx.rteDynamicRuntime, "arrayDeepCopy", //object
					parSize(StdTypes.T_PTR))) == null)
				error = true;
		}
		if (ctx.assertUsed)
		{
			if ((ctx.rteAssertFailedMd = checkMthd(ctx.rteDynamicRuntime, "assertFailed", //string
					parSize(StdTypes.T_PTR))) == null)
				error = true;
		}
		if (error)
		{
			ctx.out.println("...environment is not satisfactory");
			return false;
		}
		ctx.codeStart = ctx.rteSMthdBlock.instScalarTableSize;
		if (ctx.throwUsed)
		{
			if (!ctx.excThrowable.isParent(ctx.excChecked, ctx))
			{
				ctx.out.println("Exception must extend Throwable");
				return false;
			}
		}
		
		if ((strTmp = strPck = StringList.buildStringList(ctx.startMethod)) == null || strTmp.tablePos < 2)
		{
			ctx.out.println("...start method invalid (give at least unit and method name, separated by '.')");
			return false;
		}
		if (strTmp.tablePos == 2)
		{
			kernelPck = ctx.root;
			strUnit = strTmp.str;
			strMthd = strTmp.next.str;
		}
		else
		{
			while (strTmp.next.next.next != null)
				strTmp = strTmp.next;
			strUnit = strTmp.next.str;
			strMthd = strTmp.next.next.str;
			strTmp.next = null;
			if ((kernelPck = ctx.root.searchSubPackage(strPck, false)) == null)
			{
				ctx.out.println("...package for start method not found");
				return false;
			}
		}
		if ((ctx.startUnit = checkUnit(kernelPck, strUnit, false)) == null || (ctx.startMthd = checkMthd(ctx.startUnit, strMthd, -1)) == null)
		{
			ctx.out.println("...start method not found");
			return false;
		}
		if ((ctx.startMthd.modifier & Modifier.M_STAT) == 0)
		{
			ctx.out.println("...start method has to be static");
			return false;
		}
		
		if (ctx.staticInitMthds != null && !ctx.staticInitDone)
		{
			ctx.out.println("warning: static init code is never inserted (use \"MAGIC.doStaticInit()\")");
		}
		
		return true;
	}
	
	private Unit checkUnit(Pack pack, String what, boolean mark)
	{
		Unit ret;
		
		if ((ret = pack.searchUnit(what)) == null)
		{
			ctx.out.print("missing class ");
			pack.printFullName(ctx.out);
			ctx.out.print('.');
			ctx.out.println(what);
			return null;
		}
		if (mark)
			ret.modifier |= Modifier.MA_ACCSSD;
		return ret;
	}
	
	private boolean checkVrbl(Unit inUnit, String name, int location, int relOff, boolean checkRelOff)
	{
		Vrbl var;
		
		if ((var = inUnit.searchVariable(name, ctx)) == null)
		{
			inUnit.printPos(ctx, "missing ");
			ctx.out.println(name);
			return false;
		}
		if (var.location != location)
		{
			ctx.out.print("wrong type or modifier for ");
			ctx.out.print(name);
			ctx.out.print(" in ");
			ctx.out.print(inUnit.name);
			ctx.out.println(ctx.indirScalars ? " (perhaps @SJC.Head is needed)" : "");
			return false;
		}
		if (checkRelOff && var.relOff != relOff)
		{
			ctx.out.print("wrong position for ");
			ctx.out.print(name);
			ctx.out.print(": ");
			ctx.out.print(var.relOff);
			ctx.out.print(" instead of ");
			ctx.out.println(relOff);
			return false;
		}
		if (var.location == Vrbl.L_INSTIDS)
		{
			ctx.out.print("variable ");
			ctx.out.print(name);
			ctx.out.print(" needs to be in header (use @SJC.Head) in unit ");
			ctx.out.println(inUnit.name);
			return false;
		}
		return true;
	}
	
	private int checkVrbl(Unit inUnit, String name, boolean printErr)
	{
		Vrbl var;
		
		if ((var = inUnit.searchVariable(name, ctx)) == null)
		{
			if (printErr)
				errPrint("missing variable ", name, inUnit, ctx);
			return AccVar.INV_RELOFF;
		}
		if (var.relOff == AccVar.INV_RELOFF)
		{
			errPrint("invalid variable ", name, inUnit, ctx);
			return AccVar.INV_RELOFF;
		}
		if (var.location == Vrbl.L_INSTIDS)
		{
			errPrint("need header-variable (use @SJC.Head) ", name, inUnit, ctx);
			return AccVar.INV_RELOFF;
		}
		return var.relOff;
	}
	
	private void errPrint(String msg, String name, Unit inUnit, Context ctx)
	{
		ctx.out.print(msg);
		ctx.out.print(name);
		ctx.out.print(" in unit ");
		ctx.out.println(inUnit.name);
	}
	
	private int parSize(int type)
	{
		int res;
		switch (type)
		{
			case StdTypes.T_PTR:
				res = ctx.arch.relocBytes;
				break;
			case StdTypes.T_DPTR:
				res = 2 * ctx.arch.relocBytes;
			default:
				res = TypeRef.getMinSize(type);
		}
		return (res + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits;
	}
	
	private Mthd checkMthd(Unit inUnit, String name, int reqParSize)
	{
		Mthd mthd, foundMthd = null;
		
		mthd = inUnit.mthds;
		while (mthd != null)
		{
			if (mthd.name != null && mthd.name.equals(name))
			{
				if (foundMthd != null)
				{
					ctx.out.print("warning: ambiguous");
					printMthdUnit(inUnit, name);
				}
				foundMthd = mthd;
			}
			mthd = mthd.nextMthd;
		}
		if (foundMthd == null)
		{
			ctx.out.print("missing");
			printMthdUnit(inUnit, name);
			return null;
		}
		if (foundMthd.relOff == Mthd.INV_RELOFF)
		{
			ctx.out.print("invalid");
			printMthdUnit(inUnit, name);
			return null;
		}
		if (reqParSize != -1 && foundMthd.parSize != reqParSize)
		{
			ctx.out.print("warning: non-matching signature of");
			printMthdUnit(inUnit, name);
		}
		foundMthd.modifier |= Modifier.MA_ACCSSD | Modifier.M_NDCODE;
		return foundMthd;
	}
	
	private void printMthdUnit(Unit inUnit, String name)
	{
		ctx.out.print(" method ");
		ctx.out.print(name);
		ctx.out.print(" in unit ");
		ctx.out.println(inUnit.name);
	}
}
