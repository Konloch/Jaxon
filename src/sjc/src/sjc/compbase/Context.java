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

package sjc.compbase;

import sjc.backend.ArchFactory;
import sjc.backend.Architecture;
import sjc.compression.CompressionFactory;
import sjc.compression.Compressor;
import sjc.debug.DebugFactory;
import sjc.debug.DebugLister;
import sjc.debug.DebugWriter;
import sjc.frontend.FrontAdmin;
import sjc.memory.BootableImage;
import sjc.memory.MemoryImage;
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;
import sjc.osio.TextReader;
import sjc.output.OutputFactory;
import sjc.output.OutputFormat;
import sjc.real.EmulReal;
import sjc.real.NativeReal;
import sjc.relations.RelationManager;
import sjc.symbols.SymbolFactory;
import sjc.symbols.SymbolInformer;

/**
 * Context: root of all information needed for a compilation
 *
 * @author S. Frenz
 */

public class Context
{
	public TextPrinter out;
	public OsIO osio;
	
	public OutputFormat imgOut;
	public SymbolInformer symGen;
	public RelationManager relations;
	public FrontAdmin fa;
	public Architecture arch;
	public MemoryImage mem;
	public BootableImage bootMem; //if mem instanceof BootableImage, then bootMem==mem
	public boolean embedded, embConstRAM, leanRTE, dynaMem, indirScalars, noSyncCalls, noThrowFrames, noInlineMthdObj;
	public boolean verbose, debugCode, printCode, err;
	public Object ramLoc, ramInitLoc; //used only in embedded mode
	public int ramSize, ramOffset; //used only in embedded mode
	public int constMemorySize, symGenSize; //valid after genConstObj / generateSymbols
	public int maxStmtAutoInline;
	public int alignBlockMask; //initialized to 0
	public String debugPrefix;
	public boolean assignCall, assignHeapCall, doBoundCheck, runtimeBound, doArrayStoreCheck, runtimeNull;
	public boolean byteString, genAllUnitDesc, genAllMthds, genIntfParents;
	public boolean globalProfiling, profilerUsed, globalStackExtreme, globalAssert;
	public boolean stackExtremeUsed, globalSourceLineHints;
	public boolean throwUsed, syncUsed, arrayDeepCopyUsed, assertUsed;
	public boolean explicitTypeConversion, alternateObjNew;
	public boolean buildAssemblerText;
	
	public boolean doNotInsertConstantObjectsDuringResolve; //only used during smart recompilation
	public boolean doNotCheckVisibility; //only used during instance conversion
	
	public Pack root, rte;
	public Pack defUnits;
	public Unit langRoot;
	public String startMethod;
	public Unit startUnit;
	public Mthd startMthd;
	public Unit langString;
	public Unit rteSArray, rteSClassDesc, rteSIntfDesc, rteSIntfMap, rteSMthdBlock;
	public Unit rteDynamicRuntime, rteDynamicAri, structClass, flashClass;
	public Unit excThrowable, excChecked;
	public int rteSIMowner, rteSIMnext; //index in rteSIntfMap
	public Mthd rteDRNewInstMd, rteDRNewArrayMd, rteDRNewMultArrayMd;
	public Mthd rteDRIsInstMd, rteDRIsImplMd, rteDRIsArrayMd, rteDRCheckArrayStoreMd;
	public Mthd rteDRAssignMd, rteDRBoundExcMd, rteDRNullExcMd;
	public Mthd[] rteDABinAriCallMds = new Mthd[StdTypes.MAXBASETYPE];
	public Mthd[] rteDAUnaAriCallMds = new Mthd[StdTypes.MAXBASETYPE];
	public Mthd rteDRProfilerMd, rteDRStackExtremeErrMd; //rte-methods for profiler and stack extreme check
	public Mthd rteDoThrowMd; //method called if something is thrown
	public Mthd rteDoSyncMd; //method called if something is synchronized
	public Mthd rteArrayDeepCopyMd; //method called if deep copy of array is to be generated
	public Mthd rteAssertFailedMd; //method called if assert failed
	public Mthd staticInitMthds; //list of methods for static initialization
	public boolean staticInitDone; //flag to check if static initialization code is inserted somewhere
	public boolean needSecondGenConstObj; //flag to signal second run of genConstObj (constant objects in second pool)
	public int rteThrowFrame, rteStackExtreme; //offset of variable containing address of throw-frame / stack extreme value
	public int langStrVal, langStrCnt; //index/offset in langString;
	public int rteSClassParent, rteSClassImpl; //index in rteSClassDesc
	public int rteSIntfParents; //index in rteSIntfDesc
	public int rteSClassInstScalarSize, rteSClassInstRelocTableEntries, rteSClassInstIndirScalarSize; //only for alternateNewObj
	public int rteSArrayLength, rteSArrayDim, rteSArrayStd, rteSArrayExt; //offsets/index in rteSArray
	public int indirScalarSizeOff, indirScalarAddrOff;
	public DebugWriter debugWriter;
	public DebugLister debugLister;
	
	public TypeRef clssType, intfType, stringType, objectType, boolType, intType;
	public CtxBasedConfig config;
	public AddrList freeAddrLists;
	
	public int stringCount, stringChars, stringMemBytes;
	public int mthdCount, mthdCodeSize;
	
	public DataBlockList sourceBlocks, codeBlocks;
	
	public BootableImage compressedImage;
	public int compressedRelocateOption;
	protected UnitList unitList, lastUnit;
	private int compressedImageOrigLen;
	private byte[] compressedImageInfo;
	private StringList fileList;
	private Context decompressor;
	
	public int ramStart, relocateOption, codeStart;
	private boolean streamline, standardHeader = true, timing, prefereNativeReal;
	private int inlineLevels = 3, memStart = -1, memSize = 131072; //init with 128K image size starting at 1MB (entered below)
	private String headerFile;
	private byte[] headerData;
	private String[] secArgv;
	private String compressorName;
	private Compressor compressor;
	private String compiledFiledListname;
	private VrblStateList emptyVrblStateList;
	
	public Context(OsIO iOS)
	{
		out = (osio = iOS).getNewFilePrinter(null);
		debugPrefix = "";
		doBoundCheck = true;
		doArrayStoreCheck = true;
		startMethod = "kernel.Kernel.main";
		if (fileList == null)
			fileList = new StringList("internal init"); //tablePos is initialized with -1
	}
	
	public int compile(String[] argv, String versionInfo)
	{
		UnitList ulist;
		boolean error = false, kbSize;
		int i, uCnt = 0;
		long t0 = 0l, t1 = 0l, t2 = 0l, t3 = 0l, t4 = 0l, t5 = 0l, t6 = 0l, t7 = 0l, t8 = 0l, t9 = 0l, t10 = 0l;
		SymbolInformer sif;
		
		//get output and check parameters
		if (compressedImage == null)
		{
			out.print("Welcome to the smfCompiler SJC");
			if (versionInfo != null)
			{
				out.print(" (");
				out.print(versionInfo);
				out.print(')');
			}
			out.println();
		}
		else
			out.println("Processing decompressor:");
		if (argv.length < 1)
		{
			printHelp();
			return -1;
		}
		
		if (!getParameters(argv))
			return -1;
		
		if (timing)
		{
			out.print("(t0) ");
			t0 = osio.getTimeInfo();
		}
		//initialize environment
		if (arch == null)
			arch = ArchFactory.getArchitecture(null, null, out);
		if (memStart == -1)
			memStart = 1048576; //set default value
		if (arch.relocBytes == 2)
		{
			if (memStart >= 0x10000)
			{
				out.println("Invalid base address for 16 bit architecture");
				return -1;
			}
			if (memStart + memSize >= 0x10000)
			{
				memSize = 0x10000 - memStart;
				out.print("Memory for 16 bit architecture limited to ");
				out.print(memSize >>> 10);
				out.println(" kb to fit address range");
			}
		}
		if (compressor != null)
		{
			if (imgOut != null)
			{
				out.println("Output format can not be specified for compressed image");
				return -1;
			}
			mem = bootMem = new BootableImage();
		}
		else if (memSize != 0)
		{
			if (imgOut == null)
				imgOut = OutputFactory.getOutputFormat(null);
			if (!imgOut.checkParameter(osio, out))
				return -1;
			mem = bootMem = new BootableImage();
		}
		else
		{
			if (mem == null)
			{
				out.println("Requested bootstrap mode with mem==null");
				return -1;
			}
			if (imgOut != null)
			{
				out.println("Requested bootstrap mode with imgOut!=null");
				return -1;
			}
		}
		if (debugLister != null)
			mem.enableDebugListerSupport();
		if (relocateOption != 0)
		{
			if (bootMem != null)
			{
				bootMem.setRelocation(((long) relocateOption) << 30);
				if (verbose)
				{
					out.print("Relocating by ");
					out.print(relocateOption);
					out.println("gb");
				}
			}
			else
			{
				out.println("Requested relocation but not a bootable image");
				return -1;
			}
		}
		if (memSize != 0 && memSize < 512)
		{
			out.println("specified max. size below 512 bytes - did you forget a 'k' or 'm' in for '-s'?");
			return -1;
		}
		if (bootMem != null)
		{
			if (!standardHeader && headerFile != null && (headerData = osio.readFile(headerFile)) == null)
			{
				out.print("Could not read header file ");
				out.println(headerFile);
				return -1;
			}
			bootMem.init(memSize, memStart, standardHeader, headerData, this);
		}
		else if (!standardHeader)
		{
			out.println("Bootstrap mode needs standard header");
			return -1;
		}
		mem.streamObjects = streamline;
		mem.noSizeScalars = alternateObjNew;
		arch.init(mem, inlineLevels, this);
		if (err)
		{
			out.println("There were errors in initialization step 1");
			return -1;
		}
		if (arch.real == null)
		{ //architecture may have initialized arch.real already
			if (prefereNativeReal)
				arch.real = new NativeReal();
			else
				arch.real = new EmulReal();
		}
		fa = new FrontAdmin(this);
		if (compiledFiledListname != null)
			fa.activateListCompiledFiles(compiledFiledListname);
		if (err)
		{
			out.println("There were errors in initialization step 2");
			return -1;
		}
		
		//tell the current configuration
		if (verbose)
			printInfo();
		out.print("Compiling ");
		out.print(dynaMem ? "movable " : (embedded ? "embedded " : "static "));
		out.print(streamline ? "streamlined" : "fullsized");
		out.print(" objects for ");
		out.print(arch.relocBytes << 3);
		out.print(" bit, assign ");
		out.println(assignCall ? "all pointers via call" : (assignHeapCall ? "heap pointers via call" : "pointers directly"));
		if (verbose)
		{
			out.print("   bound exc is ");
			out.print(doBoundCheck ? runtimeBound ? "runtime" : "native" : "not checked");
			out.print(", null exc is ");
			out.println(runtimeNull ? "checked" : "native");
			out.print("   maxInlineLevels==");
			out.print(inlineLevels);
			out.print(", maxStmtAutoInline==");
			out.println(maxStmtAutoInline);
		}
		
		//parse everything
		if (timing)
		{
			out.print("(t1) ");
			t1 = osio.getTimeInfo();
		}
		for (i = 0; i < argv.length; i++)
			error |= !fa.scanparse(argv[i]);
		if (compressedImage != null)
		{
			error |= !fa.addByteArray(compressedImage.memBlock, 0, compressedImage.memBlockLen, "comprImg");
			error |= !fa.addByteArray(compressedImageInfo, 0, compressedImageInfo.length, "comprInfo");
		}
		fa.scanparseFinished();
		if (error || err)
		{
			out.println("There were errors");
			return 1;
		}
		
		//check if minimalistic runtime-entvironment exists
		if (timing)
		{
			out.print("(t2) ");
			t2 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Checking environment...");
		if (!fa.checkCompEnvironment())
		{
			out.println("...minimalistic environment not existing");
			return 2;
		}
		
		//tell in-system-recompiler that we are before unit resolving
		if (!beforeUnitResolving())
		{
			out.println("There were errors");
			return 101;
		}
		
		//resolve interfaces of units
		if (timing)
		{
			out.print("(t3) ");
			t3 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Resolving unit-interfaces...");
		ulist = unitList;
		while (ulist != null)
		{
			error |= !ulist.unit.validateModifier(this);
			ulist = ulist.next;
		}
		if (!error && !err)
		{
			ulist = unitList;
			while (ulist != null)
			{
				error |= !ulist.unit.resolveInterface(this);
				ulist = ulist.next;
			}
		}
		if (error || err)
		{
			out.println("...resolving interfaces failed");
			return 3;
		}
		
		//resolve method-blocks and update imports
		if (timing)
		{
			out.print("(t4) ");
			t4 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Resolving methods and updating imports...");
		ulist = unitList;
		while (ulist != null)
		{
			error |= !ulist.unit.resolveMethodBlocks(this);
			ulist = ulist.next;
		}
		if (error || err)
		{
			out.println("...resolving method-blocks or updating imports failed");
			return 4;
		}
		
		//re-check all the offset, assign unassigned ones and updatge references
		if (timing)
		{
			out.print("(t5) ");
			t5 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Assigning offsets and preparing descriptors...");
		mem.alignBlock(alignBlockMask);
		//pre-check and resolve java.lang.Object and java.rte.SClassDesc and update vice-versa
		if (!fa.precheckLangEnvironment())
			return 4; //output already done
		error |= !langRoot.assignOffsets(false, this);
		error |= !rteSClassDesc.assignOffsets(false, this);
		error |= !langRoot.assignOffsets(true, this);
		error |= !rteSClassDesc.assignOffsets(true, this);
		error |= !rteSClassDesc.genDescriptor(this); //SClassDesc must be the first allocated object
		if (!error && !leanRTE)
			arch.putRef(rteSClassDesc.outputLocation, -arch.relocBytes, rteSClassDesc.outputLocation, 0); //fix type of SClassDesc
		error |= !langRoot.genDescriptor(this);
		//prepare interface descriptors and maps
		error |= !rteSIntfDesc.assignOffsets(true, this);
		error |= !rteSIntfDesc.genDescriptor(this);
		error |= !rteSIntfMap.assignOffsets(true, this);
		error |= !rteSIntfMap.genDescriptor(this);
		if (!leanRTE)
			rteSMthdBlock.modifier |= Modifier.MA_ACCSSD;
		//do all the others
		ulist = unitList;
		while (ulist != null)
		{
			error |= !ulist.unit.assignOffsets(true, this);
			error |= !ulist.unit.genDescriptor(this);
			ulist = ulist.next;
		}
		if (error || err)
		{
			out.println("...assigning offsets failed");
			return 5;
		}
		
		//tell in-system-recompiler that we are after unit and before method resolving
		if (!afterUnitBeforeMethodResolving())
		{
			out.println("There were errors");
			return 102;
		}
		
		//get offsets of variables and methods
		if (timing)
		{
			out.print("(t6) ");
			t6 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Check environment-structure of languages...");
		if (!fa.checkLangEnvironment())
			return 6; //output already done
		if (timing)
		{
			out.print("(t7) ");
			t7 = osio.getTimeInfo();
		}
		
		//build constant objects
		if (verbose || timing)
			out.println("Building constant objects and strings...");
		mem.alignBlock(alignBlockMask);
		if (embedded)
		{ //create byte array for initialization of class variables
			ramSize = (ramSize + arch.allocClearBits) & ~arch.allocClearBits;
			ramLoc = mem.getStructOutputObject(null, ramStart);
			if ((ramInitLoc = mem.allocateArray(ramSize, 1, 1, StdTypes.T_BYTE, null)) == null)
				return 7;
			ramInitLoc = mem.getStructOutputObject(ramInitLoc, rteSArray.instScalarTableSize); //point to first element insted of header
			mem.enterInitObject(ramInitLoc); //remember address if neccessary
			ramOffset = -mem.getAddrAsInt(ramInitLoc, 0) + ramStart;
		}
		constMemorySize = -mem.getCurrentAllocAmountHint(); //remember current hint
		ulist = unitList;
		while (ulist != null)
		{
			error |= !ulist.unit.genConstObj(this, false);
			ulist = ulist.next;
		}
		constMemorySize += mem.getCurrentAllocAmountHint(); //now constMemorySize is valid
		if (needSecondGenConstObj)
		{
			if (verbose)
				out.println("Building second pool of constants...");
			ulist = unitList;
			while (ulist != null)
			{
				error |= !ulist.unit.genConstObj(this, true);
				ulist = ulist.next;
			}
		}
		if (err)
		{
			out.println("There were errors");
			return 7;
		}
		
		//create descriptors and generate code
		if (timing)
		{
			out.print("(t8) ");
			t8 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Filling descriptors, generating code...");
		mem.alignBlock(alignBlockMask);
		ulist = unitList;
		while (ulist != null)
		{
			uCnt++;
			error |= !ulist.unit.genOutput(this);
			ulist = ulist.next;
		}
		if (error || err)
		{
			out.println("There were errors");
			return 8;
		}
		
		//tell in-system-recompiler that we are after method resolving
		if (!afterMethodResolving())
		{
			out.println("There were errors");
			return 103;
		}
		
		//include symbol information if wanted
		if (timing)
		{
			out.print("(t9) ");
			t9 = osio.getTimeInfo();
		}
		if ((sif = symGen) != null)
		{
			out.println("Generating symbol-information...");
			symGenSize = -mem.getCurrentAllocAmountHint(); //remember current hint
			mem.alignBlock(alignBlockMask);
			while (sif != null)
			{ //generate for each SymbolInformer
				int curAllocHint = -mem.getCurrentAllocAmountHint();
				if (!sif.generateSymbols(unitList, this))
				{ //SymbolInformer may increase symGenSize if wanted
					out.println("There were errors");
					return 9;
				}
				else
				{
					out.print("symbols for ");
					out.print(sif.getName());
					out.print(": ");
					printSize(curAllocHint + mem.getCurrentAllocAmountHint(), false);
					out.println();
				}
				sif = sif.nextInformer; //get next symbol informer if existing
			}
			symGenSize += mem.getCurrentAllocAmountHint(); //now symGenSize is valid
		}
		else if (verbose || timing)
			out.println("(symbol-information are skipped)");
		
		//enter information of method to start in image
		if (timing)
		{
			out.print("(t10) ");
			t10 = osio.getTimeInfo();
		}
		if (verbose || timing)
			out.println("Entering startup-information...");
		//previously, there was an "return 8" on errors, but there can't be an error any more
		mem.finalizeImage(startUnit.outputLocation, startMthd.outputLocation, codeStart);
		
		if (memSize != 0)
		{
			//write output to disk if there is an output-formatter (ie. there is no compression)
			if (imgOut != null)
			{
				if (verbose || timing)
					out.println("Writing output to target...");
				if (!imgOut.writeOutput(bootMem, compressedImage, compressedImageOrigLen))
				{
					out.println("Error writing image");
					return 10;
				}
			}
			else if (verbose || timing)
				out.println("No output written so far.");
		}
		else if (verbose || timing)
			out.println("Objects stored in heap.");
		
		//done it, print some statistics and exit with errorcode==0
		if (verbose || timing)
			out.println("Image finalized.");
		out.print("Created ");
		out.print(mem.objectsAllocated);
		out.print(" objects for ");
		out.print(uCnt);
		out.print(" units, ");
		out.print(mthdCount);
		out.print(" methods and ");
		out.print(stringCount);
		out.println(" strings.");
		out.print("Included ");
		printSize(mthdCodeSize, false);
		out.print(" code and ");
		out.print(stringChars);
		out.print(" string-chars (");
		printSize(stringMemBytes, false);
		out.print(")");
		if (memSize != 0)
		{
			out.print(" in an ");
			printSize(bootMem.memBlockLen, false);
			out.print(" image");
		}
		out.println(".");
		if (embedded)
		{
			out.print("Embedded mode with ");
			out.print(ramSize + (embConstRAM ? constMemorySize : 0));
			out.print(" b RAM at 0x");
			out.printHexFix(ramStart, 8);
			out.print(" (first free: 0x");
			out.printHexFix(ramStart + ramSize + (embConstRAM ? constMemorySize : 0), 8);
			out.println(").");
		}
		if (symGen != null && verbose)
		{
			out.print("The image contains symbol information which require ");
			printSize(symGenSize, false);
			out.print(" (+");
			out.print(100 * symGenSize / (bootMem.memBlockLen - symGenSize));
			out.println("%).");
		}
		
		arch.printStatistics();
		
		if (timing)
		{
			out.print("t1-t0=");
			out.print(t1 - t0);
			out.print(", t2-t1=");
			out.print(t2 - t1);
			out.print(", t3-t2=");
			out.println(t3 - t2);
			out.print("t4-t3=");
			out.print(t4 - t3);
			out.print(", t5-t4=");
			out.print(t5 - t4);
			out.print(", t6-t5=");
			out.println(t6 - t5);
			out.print("t7-t6=");
			out.print(t7 - t6);
			out.print(", t8-t7=");
			out.print(t8 - t7);
			out.print(", t9-t8=");
			out.println(t9 - t8);
			out.print("t10-t9=");
			out.println(t10 - t9);
		}
		
		//if compression is enabled, compress image and start second run
		if (compressor != null)
		{
			//create environment for decompressor
			decompressor = new Context(osio);
			decompressor.compressedImage = new BootableImage();
			decompressor.compressedImage.memBlock = new byte[decompressor.compressedImageOrigLen = bootMem.memBlockLen];
			decompressor.compressedImage.baseAddress = bootMem.baseAddress;
			decompressor.compressedImage.startUnit = bootMem.startUnit;
			decompressor.compressedImage.startCode = bootMem.startCode;
			//get compressed image with information block
			decompressor.compressedRelocateOption = relocateOption;
			decompressor.compressedImageInfo = bootMem.getInfoBlock(compressorName);
			out.print("Compressing... ");
			if ((i = compressor.compress(bootMem.memBlock, bootMem.memBlockLen, decompressor.compressedImage.memBlock, bootMem.memBlockLen)) >= bootMem.memBlockLen || i <= 0)
			{
				out.println("could not compress image below original size");
				return 11;
			}
			decompressor.compressedImage.memBlockLen = i;
			//output statistics and start compilation of decompressor
			kbSize = printSize(bootMem.memBlockLen, false);
			out.print(" to ");
			printSize(i, kbSize);
			out.print(" (now ");
			out.print(bootMem.memBlockLen > 1024 * 1024 ? i / (bootMem.memBlockLen / 100) : i * 100 / bootMem.memBlockLen);
			out.println("%) done.");
			if ((i = decompressor.compile(secArgv, null)) != 0)
				return i;
			//check overlapping areas
			if (decompressor.relocateOption == relocateOption)
			{ //if different relocation, there is no overlapping
				if (decompressor.bootMem.baseAddress <= bootMem.baseAddress)
				{ //decompressor before us
					if (decompressor.bootMem.baseAddress + decompressor.bootMem.memBlockLen >= bootMem.baseAddress)
					{
						out.println("warning: lower decompressor end is behind compressed start");
					}
				}
				else
				{ //decompressor behind us
					if (bootMem.baseAddress + bootMem.memBlockLen >= decompressor.bootMem.baseAddress)
					{
						out.println("warning: higher decompressor start is below compressed end");
					}
				}
			}
		}
		
		//everything done
		return 0;
	}
	
	public boolean isCrossCompilation()
	{
		return memSize != 0;
	}
	
	public int getMaximumImageSize()
	{
		return memSize;
	}
	
	protected boolean beforeUnitResolving()
	{
		return true;
	} //will be overwritten by in-system-compilation Context
	
	protected boolean afterUnitBeforeMethodResolving()
	{
		return true;
	} //will be overwritten by in-system-compilation Context
	
	protected boolean afterMethodResolving()
	{
		return true;
	} //will be overwritten by in-system-compilation Context
	
	private boolean printSize(int size, boolean kbForce)
	{
		if (kbForce || size >= 32 << 10)
		{
			out.print((size + 1023) >>> 10);
			out.print(" kb");
			return true;
		}
		out.print(size);
		out.print(" b");
		return false;
	}
	
	private boolean getParameters(String[] argv)
	{
		int curPar, i, j;
		DebugWriter dbw;
		DebugLister dbl;
		SymbolInformer sif;
		String asmDebugID;
		
		if (argv == null)
			return false; //empty parameter not supported
		for (curPar = 0; curPar < argv.length; curPar++)
			if (argv[curPar] != null && argv[curPar].length() > 0 && argv[curPar].charAt(0) == '-')
			{
				if (argv[curPar].length() != 2)
				{
					out.print("Invalid parameter \"");
					out.print(argv[curPar]);
					out.println("\", available options:");
					printHelp();
					return false;
				}
				switch (argv[curPar].charAt(1))
				{
					case 'i':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-i\"");
							return false;
						}
						if (!getParameters(getTokensFromFile(argv[curPar])))
							return false;
						break;
					case 'V':
						timing = true;
						break;
					case 'v':
						verbose = true;
						break;
					case 'L':
						if (dynaMem)
						{
							out.println("Can not combine \"-L\" with \"-m\"");
							return false;
						}
						leanRTE = streamline = true;
						break;
					case 'l':
						if (dynaMem)
						{
							out.println("Can not combine \"-l\" with \"-m\"");
							return false;
						}
						streamline = true;
						break;
					case 'w':
						alternateObjNew = true;
						break;
					case 'M':
						if (streamline || embedded)
						{
							out.println("Can not combine \"-M\" with \"-l\" or \"-e\"");
							return false;
						}
						indirScalars = dynaMem = true;
						break;
					case 'm':
						if (streamline || embedded)
						{
							out.println("Can not combine \"-m\" with \"-l\" or \"-e\"");
							return false;
						}
						dynaMem = true;
						break;
					case 'H':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-H\"");
							return false;
						}
						compiledFiledListname = argv[curPar];
						break;
					case 'h':
						assignHeapCall = true;
						break;
					case 'c':
						assignCall = true;
						break;
					case 'C':
						doArrayStoreCheck = false;
						break;
					case 'B':
						doBoundCheck = false;
						break;
					case 'b':
						runtimeBound = true;
						break;
					case 'x':
						globalStackExtreme = true;
						break;
					case 'X':
						explicitTypeConversion = true;
						break;
					case 'r':
						globalAssert = true;
						break;
					case 'R':
						noSyncCalls = true;
						break;
					case 'n':
						runtimeNull = true;
						break;
					case 'N':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-N\"");
							return false;
						}
						startMethod = argv[curPar];
						break;
					case 'g':
						genAllUnitDesc = true;
						break;
					case 'G':
						genAllMthds = true;
						break;
					case 'j':
						genIntfParents = true;
						break;
					case 'J':
						argv[curPar++] = null; //get next parameter, too
						if (buildAssemblerText)
						{
							out.println("-J already given, ignoring additional ones");
							break;
						}
						if (curPar == argv.length)
						{
							out.println("Missing file name for \"-J\"");
							return false;
						}
						if (arch == null)
						{
							out.println("No architecture specified yet, please give before \"-J\"");
							return false;
						}
						buildAssemblerText = true;
						if ((asmDebugID = arch.checkBuildAssembler(this)) == null)
							return false;
						if ((dbl = DebugFactory.getDefaultAsmDebugLister(asmDebugID, argv[curPar], this)) == null)
						{
							out.println("Invalid parameter or unknown archID to print assembler output");
							return false;
						}
						dbl.nextLister = debugLister;
						debugLister = dbl;
						break;
					case 'd':
						debugCode = true;
						break;
					case 'D':
						if (curPar + 2 >= argv.length)
						{
							out.println("Missing parameter for \"-D\"");
							return false;
						}
						dbl = null;
						if ((dbw = DebugFactory.getDebugWriter(argv[curPar + 1], argv[curPar + 2], this)) == null && (dbl = DebugFactory.getDebugLister(argv[curPar + 1], argv[curPar + 2], this)) == null)
						{
							out.println("Invalid parameter or unknown debug info writer, currently available:");
							DebugFactory.printKnownDebuggers(out);
							return false;
						}
						argv[curPar++] = null; //get next parameter, too
						argv[curPar++] = null; //get next parameter, too
						if (dbw != null)
						{
							dbw.nextWriter = debugWriter;
							debugWriter = dbw;
						}
						else
						{
							dbl.nextLister = debugLister;
							debugLister = dbl;
						}
						break;
					case 'q':
						relations = new RelationManager();
						break;
					case 'Q':
						globalSourceLineHints = true;
						break;
					case 'W':
						printCode = true;
						break;
					case 'F':
						globalProfiling = true;
						break;
					case 'f':
						noThrowFrames = true;
						break;
					case 'Y':
						prefereNativeReal = true;
						break;
					case 'y':
						byteString = true;
						break;
					case 'k':
						noInlineMthdObj = true;
						break;
					case 'K':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-K\"");
							return false;
						}
						if ((alignBlockMask = parseInt(argv[curPar])) < 0)
						{
							out.println("Invalid number in parameter for \"-K\"");
							return false;
						}
						break;
					case 'P':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-P\"");
							return false;
						}
						standardHeader = false;
						if (!argv[curPar].equals("none"))
							headerFile = argv[curPar];
						break;
					case 'p':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-p\"");
							return false;
						}
						debugPrefix = argv[curPar];
						break;
					case 'I':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-I\"");
							return false;
						}
						if ((inlineLevels = parseInt(argv[curPar])) < 0)
						{
							out.println("Invalid number in parameter for \"-I\"");
							return false;
						}
						break;
					case 'S':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-S\"");
							return false;
						}
						if ((maxStmtAutoInline = parseInt(argv[curPar])) < 0)
						{
							out.println("Invalid number in parameter for \"-S\"");
							return false;
						}
						break;
					case 's':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-s\"");
							return false;
						}
						if ((memSize = parseInt(argv[curPar])) == -1)
						{
							out.println("Invalid number in parameter for \"-s\"");
							return false;
						}
						break;
					case 'e':
						if (dynaMem)
						{
							out.println("Can not combine \"-e\" with \"-m\"");
							return false;
						}
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-e\"");
							return false;
						}
						if ((ramStart = parseInt(argv[curPar])) == -1)
						{
							out.println("Invalid number in parameter for \"-e\"");
							return false;
						}
						embedded = true;
						break;
					case 'E':
						if (!embedded)
						{
							out.println("Please give \"-e\" before \"-E\"");
							return false;
						}
						embConstRAM = true;
						break;
					case 'a':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-a\"");
							return false;
						}
						if ((memStart = parseInt(argv[curPar])) == -1)
						{
							out.println("Invalid number in parameter for \"-a\"");
							return false;
						}
						if ((memStart & 7) != 0)
						{
							out.println("Address of image has to be at least 8-byte-aligned");
							return false;
						}
						break;
					case 'A':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-A\"");
							return false;
						}
						if (compressedImage == null)
						{
							out.println("Parameter \"-A\" is not valid without compressed image");
							return false;
						}
						if (argv[curPar].length() != 1)
						{
							out.println("Invalid parameter for \"-A\"");
							return false;
						}
						if (memStart == -1)
						{
							out.println("No \"-a\" parameter given yet, please give before \"-A\"");
							return false;
						}
						j = 0;
						for (i = 0; i < 32; i++)
							if ((memStart & (1 << i)) != 0)
								j++;
						if (j != 1)
						{
							out.println("Alignment boundary has to be power of 2");
							return false;
						}
						memStart--; //alignment uses bitmask
						switch (argv[curPar].charAt(0))
						{
							case 'r':
								memStart = ((compressedImage.baseAddress + compressedImageOrigLen + memStart) & ~memStart) | (compressedImage.baseAddress & memStart);
								break;
							case 'a':
								memStart = (compressedImage.baseAddress + compressedImageOrigLen + memStart) & ~memStart;
								break;
							default:
								out.println("Unknown parameter for \"-A\"");
								return false;
						}
						break;
					case 'Z':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-Z\"");
							return false;
						}
						if ((relocateOption = parseInt(argv[curPar])) == -1)
						{
							out.println("Invalid number in parameter for \"-Z\"");
							return false;
						}
						break;
					case 't':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-t\"");
							return false;
						}
						if ((arch = ArchFactory.getArchitecture(argv[curPar], arch, out)) == null)
						{
							out.println("Unknown target architecture, currently available:");
							ArchFactory.printKnownArchitectures(out);
							return false;
						}
						break;
					case 'T':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-T\"");
							return false;
						}
						if (arch == null)
						{
							out.println("No architecture specified yet, please give before \"-T\"");
							return false;
						}
						if (!arch.setParameter(argv[curPar], out))
							return false;
						break;
					case 'o':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-o\"");
							return false;
						}
						if ((imgOut = OutputFactory.getOutputFormat(argv[curPar])) == null)
						{
							out.println("Unknown output format, currently available:");
							OutputFactory.printKnownOutputFormats(out);
							return false;
						}
						break;
					case 'O':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-O\"");
							return false;
						}
						if (imgOut == null)
						{
							out.println("No output format specified yet, please give before \"-O\"");
							return false;
						}
						if (!imgOut.setParameter(argv[curPar], out))
							return false;
						break;
					case 'u':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-u\"");
							return false;
						}
						if ((sif = SymbolFactory.getSymbolGenerator(argv[curPar])) == null)
						{
							out.println("Unknown output format, currently available:");
							SymbolFactory.printKnownOutputFormats(out);
							return false;
						}
						sif.nextInformer = symGen;
						symGen = sif;
						break;
					case 'U':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing parameter for \"-U\"");
							return false;
						}
						if (symGen == null)
						{
							out.println("No output format specified yet, please give before \"-U\"");
							return false;
						}
						if (!symGen.setParameter(argv[curPar], out))
							return false;
						break;
					case 'z':
						argv[curPar++] = null; //get next parameter, too
						if (curPar == argv.length)
						{
							out.println("Missing algorithm for \"-z\"");
							return false;
						}
						if ((compressor = CompressionFactory.getCompressor(compressorName = argv[curPar])) == null)
						{
							out.println("Unknown compressor, currently available:");
							CompressionFactory.printKnownCompressors(out);
							return false;
						}
						argv[curPar++] = null; //remove compressor name
						//snip all the following parameters to a new string array for a second compilation
						secArgv = new String[argv.length - curPar];
						for (i = curPar; i < argv.length; i++)
						{
							secArgv[i - curPar] = argv[i];
							argv[i] = null;
						}
						curPar = argv.length - 1; //terminate parameter detection loop
						break;
					default:
						out.print("Unknown parameter \"");
						out.print(argv[curPar].charAt(1));
						out.println("\", available options:");
						printHelp();
						return false;
				}
				argv[curPar] = null;
			}
		return true;
	}
	
	private void printInfo()
	{
		out.println("SJC by Stefan Frenz, released under GPL");
		out.println("The software comes \"as is\" and without any warranty");
	}
	
	private void printHelp()
	{
		printInfo();
		//available chars:    abcdefghijklmnopqrstuvwxyz
		//unused lower case: |                          |
		//unused upper case: |                          |
		out.println("Please give at least one file to compile");
		out.println("Available options are:");
		out.println(" -i FILE    get options from FILE");
		out.println(" -V         verbose timing information");
		out.println(" -v         verbose mode, print more infos");
		out.println(" -L         flat rte (no obj.), implies \"-l\"");
		out.println(" -l         streamline objects, inhibits \"-m\"");
		out.println(" -w         use alternate Object and newInstance");
		out.println(" -M         build indirect scalars, implies \"-m\"");
		out.println(" -m         generate movable code and descriptors");
		out.println(" -H FILE    output file-listing of compiled files");
		out.println(" -h         generate call for heap pointer assign");
		out.println(" -c         generate call for every pointer assign");
		out.println(" -C         completely skip all array store checks");
		out.println(" -B         completely skip all array bound checks");
		out.println(" -b         generate runtime call on bound exception");
		out.println(" -n         generate runtime call on nullpointer use");
		out.println(" -x         generate stack extreme check for methods");
		out.println(" -X         force explicit conversion for base types");
		out.println(" -r         generate assert checks (enable globally)");
		out.println(" -R         do not encode rte-calls for synchronized");
		out.println(" -N PK.U.MT use method MT in unit PK.U as entry method");
		out.println(" -g         generate all unit-descriptors (don't skip)");
		out.println(" -G         generate all mthd-code-bodies (don't skip)");
		out.println(" -j         generate array with parents for interfaces");
		out.println(" -J FILE    try to build an assembler source from java");
		out.println(" -d         create binary debug output for each method");
		out.println(" -D DW FILE debug writer DW with FILE (sym is default)");
		out.println(" -q         enable relation manager to watch relations");
		out.println(" -Q         enable global source line hint integrating");
		out.println(" -W         print all method's instructions if possible");
		out.println(" -F         insert calls to runtime-profiler at each method");
		out.println(" -f         do not generate code for throw-frames (no catch)");
		out.println(" -Y         prefer native float/double usage inside compiler");
		out.println(" -y         use byte-values instead of char-values in String");
		out.println(" -k         skip method code objects for inline-marked methods");
		out.println(" -K MASK    align blocks of allocated objects with mask MASK");
		out.println(" -P FILE    special (or \"none\") instead of standard header");
		out.println(" -p PREF    naming prefix for all debug files to be written");
		out.println(" -I LEVL    specify maximum level of method inlining (def==3)");
		out.println(" -S AMNT    maximum statement amount for auto-inline (def==0)");
		out.println(" -s SIZE    specify maximum size of memory image to SIZE");
		out.println(" -s 0       do not use image, allocate in bootstrap mode");
		out.println(" -e ADDR    embedded mode, set static vars' RAM address");
		out.println(" -E         constant objects through RAM, needs \"-e\"");
		out.println(" -a ADDR    specify start address of memory image");
		out.println(" -Z GB      relocate image address by GB gb");
		out.println(" -t ARCH    specify target architecture");
		out.println(" -T APRM    parameter for architecture");
		out.println(" -o OFMT    specify output format");
		out.println(" -O OPRM    parameter for output");
		out.println(" -u UFMT    specify symbol generator");
		out.println(" -U UPRM    parameter for symbol generator");
		out.println(" -z CALG X  compression, following params X are for decompressor");
		out.println("    -A        change mode of given address to one of these:");
		out.println("       r      relative: align to boundary, keep lower bits");
		out.println("       a      align: align to next boundary");
		out.println("Possible input types are:");
		FrontAdmin.printKnownLanguages(out);
		out.println("Possible architectures are:");
		ArchFactory.printKnownArchitectures(out);
		out.println("Possible output formats are:");
		OutputFactory.printKnownOutputFormats(out);
		out.println("Possible symbol generators are:");
		SymbolFactory.printKnownOutputFormats(out);
		out.println("Possible compression modules are:");
		CompressionFactory.printKnownCompressors(out);
		out.println("Possible debug file writers are:");
		DebugFactory.printKnownDebuggers(out);
	}
	
	private String[] getTokensFromFile(String filename)
	{
		TextReader r = new TextReader();
		boolean inString = false, nextESC = false;
		int bufLen = 0, tmpLen, tokCnt = 0;
		char[] buffer = new char[30], tmpBuf;
		char c;
		StringList tokens = null, lastTok = null;
		String[] ret;
		
		if (!r.initData(osio.readFile(filename)))
		{
			out.print("error opening file ");
			out.println(filename);
			return null;
		}
		while (true)
		{ //read until end of file or end of line
			if ((c = r.nextChar) == '\n' || c == '\r' || c == '\0')
			{ //finished reading
				if (bufLen > 0)
				{ //build remaining string
					if (tokens == null)
						lastTok = tokens = new StringList(new String(buffer, 0, bufLen));
					else
						lastTok = new StringList(lastTok, new String(buffer, 0, bufLen));
					tokCnt++;
				}
				break; //stop loop
			}
			if (nextESC)
			{
				buffer[bufLen++] = c;
				nextESC = false;
			} //after escape character
			else if (c == '\\')
				nextESC = true; //escape character
			else if (inString && c == '"')
				inString = false; //end of string
			else if (c == '"')
				inString = true; //start of string
			else if (inString || c != ' ')
				buffer[bufLen++] = c; //normal character
			else if (bufLen > 0)
			{ //option finished, build string
				if (tokens == null)
					lastTok = tokens = new StringList(new String(buffer, 0, bufLen));
				else
					lastTok = new StringList(lastTok, new String(buffer, 0, bufLen));
				bufLen = 0;
				tokCnt++;
			}
			r.readChar(); //get next character
			if (bufLen + 1 >= buffer.length)
			{ //buffer exceeded, build new one
				tmpBuf = new char[buffer.length];
				for (tmpLen = 0; tmpLen < bufLen; tmpLen++)
					tmpBuf[tmpLen] = buffer[tmpLen];
				buffer = tmpBuf;
			}
		}
		if (inString || nextESC)
		{
			out.print("unfinished string or escape sequence in file ");
			out.println(filename);
			return null;
		}
		//everything ok, got all tokens, now build array of tokens
		ret = new String[tokCnt];
		for (tmpLen = 0; tmpLen < tokCnt; tmpLen++)
		{
			ret[tmpLen] = tokens.str;
			tokens = tokens.next;
		}
		return ret;
	}
	
	public void addUnit(Unit u)
	{
		UnitList newOne;
		
		newOne = new UnitList(u);
		if (lastUnit != null)
		{
			lastUnit.next = newOne;
			lastUnit = newOne;
		}
		else
			unitList = lastUnit = newOne;
	}
	
	public void writeSymInfo()
	{
		DebugWriter dbw;
		DebugLister dbl;
		
		if (verbose)
			out.println("Writing debug info...");
		if (debugWriter == null && compressedImage == null)
			debugWriter = DebugFactory.getDebugWriter(null, debugPrefix.concat("syminfo.txt"), this); //get standard debug writer only if there is no compressed image
		dbw = debugWriter;
		while (dbw != null)
		{
			writeSymInfo(dbw);
			dbw.finalizeImageInfo();
			dbw = dbw.nextWriter;
		}
		dbl = debugLister;
		while (dbl != null)
		{
			dbl.globalRAMInfo(compressedImage != null, ramInitLoc, ramSize, constMemorySize);
			dbl.listMemory(mem);
			dbl = dbl.nextLister;
		}
		if (verbose)
			out.println("Debug info written.");
		if (decompressor != null)
			decompressor.writeSymInfo();
	}
	
	private void writeSymInfo(DebugWriter symOut)
	{
		UnitList ulist;
		
		symOut.startImageInfo(compressedImage != null);
		
		symOut.globalMemoryInfo(bootMem.baseAddress, bootMem.memBlockLen);
		symOut.globalMethodInfo(mthdCodeSize, mthdCount);
		symOut.globalStringInfo(stringCount, stringChars, stringMemBytes);
		symOut.globalRAMInfo(ramInitLoc, ramSize, constMemorySize);
		symOut.globalSymbolInfo(symGenSize);
		
		ulist = unitList;
		while (ulist != null)
		{
			ulist.unit.writeDebug(this, symOut);
			ulist = ulist.next;
		}
		
		if (decompressor != null)
			decompressor.writeSymInfo(symOut);
	}
	
	public void addFile(StringList fileName)
	{ //recycle list, next is not used
		fileName.tablePos = fileList.tablePos + 1;
		fileName.next = fileList;
		fileList = fileName;
	}
	
	public void attachSource(int fileID, byte[] data)
	{
		DataBlockList source;
		
		source = new DataBlockList();
		source.id = fileID;
		source.name = getNameOfFile(fileID);
		source.nextDataBlock = sourceBlocks;
		source.data = data;
		sourceBlocks = source;
	}
	
	private void printUniquePackName(TextPrinter v, StringList s)
	{
		while (s != null)
		{
			v.print(s.str);
			v.print('$');
			s = s.next;
		}
	}
	
	private void printUniqueUnitName(TextPrinter v, Unit u)
	{
		if (u.outerUnit != null)
		{
			printUniqueUnitName(v, u.outerUnit);
			v.print('$');
		}
		v.print(u.name);
	}
	
	private void printUniqueType(TextPrinter v, TypeRef t)
	{
		int i;
		
		if (t == null)
		{
			v.print('C');
			return;
		}
		switch (t.baseType)
		{
			case TypeRef.T_RES:
				v.print('r');
				break;
			case TypeRef.T_VOID:
				v.print('v');
				break;
			case TypeRef.T_QID:
				v.print('$');
				v.print(t.qid.getLastPID());
				v.print('$');
				break;
			case TypeRef.T_BYTE:
				v.print('b');
				break;
			case TypeRef.T_SHRT:
				v.print('s');
				break;
			case TypeRef.T_INT:
				v.print('i');
				break;
			case TypeRef.T_LONG:
				v.print('l');
				break;
			case TypeRef.T_FLT:
				v.print('f');
				break;
			case TypeRef.T_DBL:
				v.print('d');
				break;
			case TypeRef.T_CHAR:
				v.print('c');
				break;
			case TypeRef.T_BOOL:
				v.print('b');
				break;
			default:
				v.print('O');
		}
		for (i = 0; i < t.arrDim; i++)
			v.print('A');
	}
	
	public void printUniqueMethodName(TextPrinter v, Mthd m, Unit owner)
	{
		Param p = m.param;
		v.print('$');
		if (m.owner.pack != null)
			printUniquePackName(v, owner.pack.name);
		v.print('$');
		printUniqueUnitName(v, owner);
		v.print('$');
		v.print(m.name);
		v.print('$');
		printUniqueType(v, m.retType);
		v.print('$');
		while (p != null)
		{
			printUniqueType(v, p.type);
			p = p.nextParam;
		}
		v.print('$');
	}
	
	public void printUniqueMethodName(TextPrinter v, Mthd m)
	{
		printUniqueMethodName(v, m, m.owner);
	}
	
	public void printUniqueVarName(TextPrinter v, AccVar m, Unit forUnit)
	{
		v.print('$');
		if (forUnit.pack != null)
			printUniquePackName(v, m.owner.pack.name);
		v.print('$');
		printUniqueUnitName(v, forUnit);
		v.print('$');
		v.print(m.name);
	}
	
	public String getPlainNameOfFile(int fileID)
	{
		StringList s;
		int first, last;
		
		if (fileID != -1)
		{
			s = fileList;
			while (s != null)
			{
				if (s.tablePos == fileID)
				{
					first = s.str.lastIndexOf('/');
					if (first == -1)
						first = s.str.lastIndexOf('\\');
					first++;
					last = s.str.lastIndexOf('.');
					return s.str.substring(first, last);
				}
				s = s.next;
			}
		}
		return null;
	}
	
	public String getNameOfFile(int fileID)
	{
		StringList s;
		
		if (fileID != -1)
		{
			s = fileList;
			while (s != null)
			{
				if (s.tablePos == fileID)
					return s.str;
				s = s.next;
			}
		}
		return null;
	}
	
	public byte[] getAttachedSourceOfFile(int fileID)
	{
		DataBlockList s;
		
		if (fileID != -1)
		{
			s = sourceBlocks;
			while (s != null)
			{
				if (s.id == fileID)
					return s.data;
				s = s.nextDataBlock;
			}
		}
		return null;
	}
	
	public void printPos(int fileID, int line, int col)
	{
		String name = getNameOfFile(fileID);
		if (name == null)
			out.print("#internal");
		else
			out.print(name);
		if (line != -1 || col != -1)
		{
			out.print(':');
			out.print(line);
			out.print(':');
			out.print(col);
		}
	}
	
	public void printSourceHint(Token token)
	{
		printSourceHint(out, token, "//");
	}
	
	public void printSourceHint(TextPrinter v, Token token, String commentStarter)
	{
		byte[] data;
		if (token == null)
			return;
		v.print(commentStarter);
		String name = getNameOfFile(token.fileID);
		if (name == null)
			v.print("#internal");
		else
			v.print(name);
		if (token.line != -1)
		{
			v.print(':');
			v.print(token.line);
		}
		if (token.srcLength != 0 && (data = getAttachedSourceOfFile(token.fileID)) != null)
		{
			v.print(": ");
			v.printSequence(data, token.srcStart, token.srcLength, true, true);
		}
		v.println();
	}
	
	public Object allocateString(String what)
	{
		int off, len, tmp, destOff = 0;
		Object str, arr, dest;
		
		if (what == null || err)
			return null;
		len = what.length();
		if (langString.inlArr != null)
		{ //inline array
			//allocate space
			tmp = len;
			if (!byteString)
				tmp = tmp << 1;
			if ((str = mem.allocate(langString.instScalarTableSize + tmp, 0, langString.instRelocTableEntries, langString.outputLocation)) == null)
			{
				out.println("error allocating memory while creating inline strings");
				err = true;
				return null;
			}
			mem.allocationDebugHint(what);
			//enter pointers and infos
			mem.putInt(str, langStrCnt, len);
			//enter destination
			dest = str;
			destOff = langString.instScalarTableSize;
		}
		else
		{ //normal array
			//allocate space
			if ((str = mem.allocate(langString.instScalarTableSize, langString.instIndirScalarTableSize, langString.instRelocTableEntries, langString.outputLocation)) == null)
			{
				out.println("error allocating memory while creating strings");
				err = true;
				return null;
			}
			mem.allocationDebugHint(what);
			if (byteString)
				arr = mem.allocateArray(len, 1, 1, StdTypes.T_BYTE, null);
			else
				arr = mem.allocateArray(len, 1, 2, StdTypes.T_CHAR, null);
			if (arr == null)
			{
				out.println("error allocating memory while creating array for string");
				err = true;
				return null;
			}
			//enter pointers and infos
			if (langStrCnt != AccVar.INV_RELOFF)
				mem.putInt(str, langStrCnt, len);
			arch.putRef(str, langStrVal, arr, embConstRAM ? ramOffset : 0);
			if (indirScalars)
			{
				dest = mem.getIndirScalarObject(arr);
				destOff = rteSArray.instIndirScalarTableSize;
			}
			else
			{
				dest = arr;
				destOff = rteSArray.instScalarTableSize;
			}
		}
		//enter characters
		if (byteString)
			for (off = 0; off < len; off++)
				mem.putByte(dest, destOff + off, (byte) what.charAt(off));
		else
			for (off = 0; off < len; off++)
				mem.putShort(dest, destOff + (off << 1), (short) what.charAt(off));
		return str;
	}
	
	public Object allocateIntArray(int[] array)
	{
		Object arrayObj = null, arrayAcc;
		int len, off, arrOff;
		
		if (array != null)
		{
			if ((arrayObj = mem.allocateArray(len = array.length, 1, 4, StdTypes.T_INT, null)) == null)
				return null;
			if (indirScalars)
			{
				arrayAcc = mem.getIndirScalarObject(arrayObj);
				arrOff = rteSArray.instIndirScalarTableSize;
			}
			else
			{
				arrayAcc = arrayObj;
				arrOff = rteSArray.instScalarTableSize;
			}
			for (off = 0; off < len; off++)
				mem.putInt(arrayAcc, arrOff + (off << 2), array[off]);
		}
		return arrayObj;
	}
	
	public Object allocateMultiArray(int curDim, FilledParam sizes, int entrySize, int stdType, Object extTypeLoc)
	{
		int i, max, arrOff;
		Object obj;
		
		if (sizes == null || sizes.expr == null)
			return null;
		max = sizes.expr.getConstIntValue(this);
		if ((obj = mem.allocateArray(max, curDim, entrySize, stdType, extTypeLoc)) != null && curDim > 1)
		{
			arrOff = -(rteSArray.instRelocTableEntries + 1) * arch.relocBytes;
			for (i = 0; i < max; i++, arrOff -= arch.relocBytes)
				arch.putRef(obj, arrOff, allocateMultiArray(curDim - 1, sizes.nextParam, entrySize, stdType, extTypeLoc), 0);
		}
		return obj;
	}
	
	private static int parseInt(String s)
	{
		int ret = 0, c, v, sign, pos = 0;
		int base = 10;
		
		if (s == null || s.length() == 0)
			return -1;
		
		while (s.charAt(pos) == ' ' || s.charAt(pos) == '\t')
		{ //skip leading blanks
			if (++pos >= s.length())
				return -1;
		}
		
		if (s.charAt(pos) == '-')
		{ //get sign
			sign = -1;
			pos++;
		}
		else
		{ //default is positive value
			if (s.charAt(pos) == '+')
				pos++; //accept sign
			sign = 1;
		}
		
		//check for hex number
		if (pos + 2 < s.length() && s.charAt(pos) == '0' && (s.charAt(pos + 1) == 'x' || s.charAt(pos + 1) == 'X'))
		{
			pos += 2;
			base = 16;
		}
		
		while (pos < s.length())
		{
			c = (int) s.charAt(pos) & 0xFF;
			if (c >= 48 && c <= 57)
				v = c - 48; //0..9
			else if (c >= 65 && c <= 70)
				v = c - 55; //10..15
			else if (c >= 97 && c <= 102)
				v = c - 87; //10..15
			else if (c == 75 || c == 107)
			{ //K/k
				if (pos != s.length() - 1)
					return -1;
				ret *= 1024;
				break;
			}
			else if (c == 77 || c == 109)
			{ //M/m
				if (pos != s.length() - 1)
					return -1;
				ret *= 1024 * 1024;
				break;
			}
			else
				return -1;
			if (v >= base)
				return -1;
			ret = ret * base + v;
			pos++;
		}
		return sign * ret;
	}
	
	private VrblStateList getVrblCopy(Vrbl singleVrbl)
	{
		VrblStateList ret;
		if (emptyVrblStateList == null)
			ret = new VrblStateList();
		else
		{
			ret = emptyVrblStateList;
			emptyVrblStateList = emptyVrblStateList.next;
			ret.next = null;
		}
		ret.vrbl = singleVrbl;
		ret.modCopy = singleVrbl.modifier;
		return ret;
	}
	
	public VrblStateList copyVrblListState(Vrbl vrblList1, VrblList vrblList2)
	{
		VrblStateList ret = null, last = null;
		Vrbl test;
		while (vrblList1 != null || vrblList2 != null)
		{
			if (vrblList1 != null)
			{
				test = vrblList1;
				vrblList1 = vrblList1.nextVrbl;
			}
			else
			{
				test = vrblList2.vrbl;
				vrblList2 = vrblList2.next;
			}
			if (last == null)
				ret = last = getVrblCopy(test);
			else
			{
				last.next = getVrblCopy(test);
				last = last.next;
			}
		}
		return ret;
	}
	
	public void recycleVrblStatelist(VrblStateList list)
	{
		VrblStateList tmp;
		while (list != null)
		{
			tmp = list.next;
			list.vrbl = null;
			list.next = emptyVrblStateList;
			emptyVrblStateList = list;
			list = tmp;
		}
	}
	
	public void setVrblListState(Vrbl vars1, VrblList vars2, VrblStateList destState)
	{
		Vrbl test;
		while (vars1 != null || vars2 != null)
		{
			if (vars1 != null)
			{
				test = vars1;
				vars1 = vars1.nextVrbl;
			}
			else
			{
				test = vars2.vrbl;
				vars2 = vars2.next;
			}
			test.modifier &= ~(Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN);
			if (destState != null && test == destState.vrbl)
			{
				test.modifier |= destState.modCopy & (Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN);
				destState = destState.next;
			}
		}
	}
	
	private void setMayBeWrittenIfIsWrittenAndClearIsWritten(Vrbl var)
	{
		if ((var.modifier & Modifier.MF_ISWRITTEN) != 0)
			var.modifier |= Modifier.MF_MAYBEWRITTEN;
		var.modifier &= ~Modifier.MF_ISWRITTEN;
	}
	
	public void setVrblStatePotential(Vrbl vars1, VrblList vars2, VrblStateList preState)
	{
		Vrbl test;
		while (vars1 != null || vars2 != null)
		{
			if (vars1 != null)
			{
				test = vars1;
				vars1 = vars1.nextVrbl;
			}
			else
			{
				test = vars2.vrbl;
				vars2 = vars2.next;
			}
			if (preState != null && test == preState.vrbl)
			{
				if ((preState.modCopy & Modifier.MF_ISWRITTEN) == 0)
					setMayBeWrittenIfIsWrittenAndClearIsWritten(test);
				preState = preState.next;
			}
			else
				setMayBeWrittenIfIsWrittenAndClearIsWritten(test);
		}
	}
	
	public void setVrblStateSure(Vrbl vars1, VrblList vars2, VrblStateList sureState)
	{
		Vrbl test;
		while (vars1 != null || vars2 != null)
		{
			if (vars1 != null)
			{
				test = vars1;
				vars1 = vars1.nextVrbl;
			}
			else
			{
				test = vars2.vrbl;
				vars2 = vars2.next;
			}
			if (sureState != null && test == sureState.vrbl)
			{
				if ((sureState.modCopy & Modifier.MF_ISWRITTEN) != 0)
					test.modifier |= Modifier.MF_ISWRITTEN;
				sureState = sureState.next;
			}
		}
	}
	
	public void setVrblStateCombined(Vrbl vars1, VrblList vars2, VrblStateList otherState)
	{
		Vrbl test;
		while (vars1 != null || vars2 != null)
		{
			if (vars1 != null)
			{
				test = vars1;
				vars1 = vars1.nextVrbl;
			}
			else
			{
				test = vars2.vrbl;
				vars2 = vars2.next;
			}
			if (otherState != null && test == otherState.vrbl)
			{
				if (((test.modifier & Modifier.MF_ISWRITTEN) == 0) || ((otherState.modCopy & Modifier.MF_ISWRITTEN) == 0))
				{
					//not both have "iswritten" set
					if (((test.modifier | otherState.modCopy) & (Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN)) != 0)
					{
						//at least one has at least "maybe" set
						test.modifier = (test.modifier & ~Modifier.MF_ISWRITTEN) | Modifier.MF_MAYBEWRITTEN;
					}
				}
				otherState = otherState.next;
			}
			else
				setMayBeWrittenIfIsWrittenAndClearIsWritten(test);
		}
	}
	
	public void mergeVrblListState(VrblStateList soFar, Vrbl toMerge1, VrblList toMerge2)
	{
		Vrbl test;
		while (toMerge1 != null || toMerge2 != null)
		{
			if (toMerge1 != null)
			{
				test = toMerge1;
				toMerge1 = toMerge1.nextVrbl;
			}
			else
			{
				test = toMerge2.vrbl;
				toMerge2 = toMerge2.next;
			}
			if (soFar != null && soFar.vrbl == test)
			{
				if (((test.modifier | soFar.modCopy) & (Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN)) != 0)
					soFar.modCopy |= Modifier.MF_MAYBEWRITTEN;
				soFar.modCopy &= test.modifier & Modifier.MF_ISWRITTEN;
			}
		}
	}
}
