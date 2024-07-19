/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2015, 2024 Stefan Frenz
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

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * JMthd: java-specific behaviour of methods
 *
 * @author S. Frenz
 * @version 240320 added STRUCT test
 * version 151108 added allocation debug hint
 * version 120923 added support for statement printer
 * version 120605 added check for invalid "final abstract" combination
 * version 110624 fixed verbose output for removing windows native flag
 * version 110211 fixed stack extreme check in embedded mode
 * version 101210 adopted changed Architecture
 * version 101122 inserted check of memory location in enterCodeRef
 * version 101021 added copy of new K_ASRT mark
 * version 101015 adopted changed Expr
 * version 100923 also copying parCnt and parSize for interface extensions
 * version 100826 added code for in-system compilation
 * version 100616 fixed check of noInlineMthdObj
 * version 100608 added modifier MA_ACCSSD if enterCodeAddress annotation is present
 * version 100512 adopted changed Modifier/Marks
 * version 100504 adopted changed StSync, moved special super-/this-call-checks to StBlock
 * version 100409 adopted changed TypeRef
 * version 100401 added checks for forced inlining, added support for enterCodeAddr annotation
 * version 100319 adopted changed Mthd
 * version 100126 added setting of MC_EXPCONV if parent unit has flag set
 * version 100115 adopted codeStart-movement
 * version 100114 adopted changed Architecture
 * version 091116 adopted simplified Mthd-signature
 * version 091111 added support for instance final-var-init, adopted changed resolve-signature
 * version 091109 rewriting of var's written state checks
 * version 091104 adopted changed Architecture
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090718 added support for non-static final variables
 * version 090717 adopted changed Architecture
 * version 090626 added support for stack extreme check
 * version 090619 adopted changed Architecture
 * version 090616 adopted changed ExCall
 * version 090508 adopted changes in StBlock
 * version 090506 added resolve-check for throw-types
 * version 090505 added support for doFlowAnalysis
 * version 090207 added copyright notice
 * version 080614 adopted changed Unit.searchVariable
 * version 080610 moved vars to Mthd, added throwable-handling
 * version 080603 added checking of throws-declaration
 * version 080414 checking of instance variable initialization as required for method redirection
 * version 080106 clarified comments
 * version 080105 added support for profiling
 * version 071215 adopted change in Mthd
 * version 071002 removed leading "_n_" for native method's variable
 * version 071001 added support for native methods
 * version 070917 optimized initialization of instance variables
 * version 070913 changed handling of local variables
 * version 070908 optimized signature of Stmt.resolve
 * version 070905 added support for this(.)-calls
 * version 070903 adopted changes in AccVar.checkNameAgainst*
 * version 070731 adopted renaming of id to name
 * version 070727 adopted change of Mthd.id from PureID to String
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070615 adopted change in outputAddr-handling, removed call to prepareMethodCoding
 * version 070527 added support for explicit initialization
 * version 070506 removed not needed writing to class descriptor
 * version 070303 adopted change in interface of MemoryImage
 * version 070127 added initialization of instance variables
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070101 restricted auto inline for abstract methods
 * version 061231 added flag to avoid recursive inlining, respected K_NINL
 * version 061228 respected rediretion in genInlineOutput
 * version 061225 added method to generate inline code
 * verison 061220 added check for validity of redirected method during code generation
 * version 061203 optimized calls to printPos and compErr
 * version 061111 adopted change of Architecture.codeProlog
 * version 061027 optimized check for special names
 * version 060822 added support for redirection
 * version 060723 added support for extension of explicit standard constructor in parent
 * version 060628 added support for static compilation
 * version 060621 updated print-method
 * version 060607 initial version
 */

public class JMthd extends Mthd
{
	public final static String REGNOTFREE = "registers not clear at beginning of method";
	
	//required fields for resolving
	protected StBlock block;
	protected TryCaFiContainer curTryFrame, freeTryFrames;
	private boolean tryProfiling, tryStackExtreme;
	private UnitList runtimeClass;
	protected FilledAnno anno;
	
	protected JMthd(String ii, int im, int fid, int il, int ic)
	{
		super(ii, im, fid, il, ic);
	}
	
	public Mthd copy()
	{
		JMthd n;
		
		n = new JMthd(name, modifier, fileID, line, col);
		n.retType = retType;
		n.param = param;
		n.parCnt = parCnt; //if already resolved, this will be valid, too
		n.parSize = parSize; //if already resolved, this will be valid, too
		n.block = block;
		//leave other fields blank
		return n;
	}
	
	public boolean checkNameAndType(Unit inUnit, Context ctx)
	{
		Mthd om;
		Param par;
		int offset = 0;
		QualIDList curThrows;
		
		//set owner
		owner = inUnit;
		//check abstract method in abstract class and non-final modifier
		if ((modifier & (Modifier.M_ABSTR | Modifier.M_FIN)) == (Modifier.M_ABSTR | Modifier.M_FIN))
		{
			printPos(ctx, "abstract method can not be final ");
			printNamePar(ctx.out);
			return false;
		}
		if ((modifier & Modifier.M_ABSTR) != 0 && (owner.modifier & Modifier.M_ABSTR) == 0)
		{
			printPos(ctx, "abstract method needs abstract class ");
			printNamePar(ctx.out);
			return false;
		}
		//test for special names
		if (SJava.isSpecialName(name))
		{
			printPos(ctx, "invalid use of keyword for method ");
			printNamePar(ctx.out);
			return false;
		}
		//check modifiers if native method
		if ((modifier & Modifier.M_NAT) != 0 && (modifier & Modifier.M_STAT) == 0)
		{
			printPos(ctx, "native method needs to be static");
			return false;
		}
		//check STRUCT
		if ((inUnit.modifier & Modifier.M_STRUCT) != 0)
		{
			printPos(ctx, "methods are not possible in STRUCT");
			return false;
		}
		//copy possible unit-marker
		marker |= inUnit.marker & (Marks.K_INTR | Marks.K_DEBG | Marks.K_FINL | Marks.K_NINL | Marks.K_PROF | Marks.K_NPRF | Marks.K_PRCD | Marks.K_NOPT | Marks.K_SEPC | Marks.K_NSPC | Marks.K_SLHI | Marks.K_EXPC | Marks.K_NWIN | Marks.K_FCDG | Marks.K_ASRT);
		if ((modifier & Modifier.M_NAT) == 0 && (marker & Marks.K_NWIN) != 0)
		{
			marker &= ~Marks.K_NWIN;
			if (ctx.verbose)
			{
				printPos(ctx, "removing windows native flag for non-native method");
				ctx.out.println();
			}
		}
		if ((marker & Marks.K_FCDG) != 0)
			modifier |= Modifier.M_NDCODE;
		//check return-type
		if (retType == null)
		{
			if (!name.equals(inUnit.name))
			{
				printPos(ctx, "constructor must have name of class or method needs return-type at ");
				printNamePar(ctx.out);
				return false;
			}
			if (!isConstructor)
			{
				printPos(ctx, "method needs return-type in method ");
				printNamePar(ctx.out);
				return false;
			}
			//set state of owner-class (if no constructor is here, state will be copied in checkDeclarations)
			inUnit.explConstr = true; //tell them we have an explicit constructor
			if (param == null)
				inUnit.explStdConstr = true; //there is an explicit standard constructor
		}
		else
		{
			if (isConstructor)
			{
				printPos(ctx, "constructor can not have return-type in method ");
				printNamePar(ctx.out);
				return false;
			}
			if (!retType.resolveType(inUnit, ctx))
				return false;
		}
		//check parameter-types
		parCnt = 0;
		par = param;
		while (par != null)
		{
			if (!par.type.resolveType(inUnit, ctx) || !param.checkNameAgainstParam(param, ctx) || !par.enterSize(Param.L_PARAM, ctx))
			{
				ctx.out.print(" in method ");
				printNamePar(ctx.out);
				return false;
			}
			if (par.minSize < 0)
				offset += par.minSize * ctx.arch.relocBytes; //this is a reloc, use specified pointer-size
			else
				offset -= (par.minSize + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits; //scalar, minimum size aligned to specified bits
			par.relOff = offset;
			if (ctx.relations != null && par.type.qid != null && par.type.qid.unitDest != null)
				ctx.relations.addRelation(par.type.qid.unitDest, null, null, inUnit, this);
			par = par.nextParam;
			parCnt++;
		}
		parSize = -offset;
		//correct by constant offset
		par = param;
		while (par != null)
		{
			par.relOff += parSize;
			par = par.nextParam;
		}
		//check against other declared methods in this unit
		om = inUnit.mthds;
		while (om != this)
		{
			if (matches(om, ctx))
			{ //name and types identical
				printPos(ctx, "method ");
				printNamePar(ctx.out);
				ctx.out.print(" already declared");
				return false;
			}
			om = om.nextMthd;
		}
		//resolve throws-list
		curThrows = throwsList;
		while (curThrows != null)
		{
			if (!curThrows.qid.resolveAsUnit(inUnit, ctx) || !curThrows.qid.unitDest.resolveInterface(ctx))
				return false;
			if (!ctx.excThrowable.isParent(curThrows.qid.unitDest, ctx))
			{
				curThrows.qid.printPos(ctx, "throws only allowed for throwable types");
				return false;
			}
			curThrows.qid.unitDest.modifier |= Modifier.MA_ACCSSD;
			curThrows = curThrows.nextQualID;
		}
		//everything done
		return true;
	}
	
	public boolean resolve(Context ctx)
	{
		ExCall call = null;
		Stmt cs;
		StSync sy;
		StBlock sb;
		Param mPar;
		FilledParam cPar;
		ExVar ep;
		boolean invType = false;
		int addr;
		VrblList vrblList;
		FilledAnno a;
		
		if (isResolved)
			return true;
		
		//check modifiers and markers
		if (ctx.noInlineMthdObj && (marker & Marks.K_FINL) != 0 && (modifier & (Modifier.M_OVERLD | Modifier.MA_INTFMD)) != 0)
		{
			printPos(ctx, "can not force inlining of overloaded/interface method");
			return false;
		}
		//check annotations
		a = anno;
		while (a != null)
		{
			if (a.name.equals(""))
			{
				if (a.keys == null || !a.keys.str.equals("enterCodeAddr") || a.keys.next != null)
				{
					a.printPos(ctx, "only enterCodeAddr as SJC-JMthd-Annotation allowed");
					return false;
				}
				if (!a.values.expr.resolve(owner, this, Expr.RF_CHECKREAD, null, ctx))
					return false;
				if (a.values.expr.calcConstantType(ctx) != StdTypes.T_INT)
				{
					a.printPos(ctx, "need constant integer value for enterCodeAddr");
					return false;
				}
				addr = a.values.expr.getConstIntValue(ctx);
				if (!ctx.mem.checkMemoryLocation(null, addr, ctx.arch.relocBytes))
				{
					a.printPos(ctx, "invalid destination address in enterCodeAddr of ");
					ctx.out.print(name);
					return false;
				}
				enterCodeRef(null, addr, ctx);
				modifier |= Modifier.M_NDCODE | Modifier.MA_ACCSSD;
			}
			else
			{
				a.printPos(ctx, "unknown SJC-JMthd-Annotation ");
				ctx.out.print(a.name);
				return false;
			}
			a = a.nextAnno;
		}
		//for native methods: check if corresponding variable exists and check param types
		if ((modifier & Modifier.M_NAT) != 0)
		{
			if ((modifier & Modifier.M_SYNC) != 0)
			{
				printPos(ctx, "can not synchronize native method");
				return false;
			}
			if ((nativeAddress = owner.searchVariable(name, ctx)) == null)
			{
				printPos(ctx, "native method ");
				ctx.out.print(name);
				ctx.out.print(" requires destination variable");
				return false;
			}
			if (!nativeAddress.type.isStructType())
				switch (nativeAddress.type.baseType)
				{
					case StdTypes.T_BYTE:
						invType = ctx.arch.relocBytes != 1;
						break;
					case StdTypes.T_SHRT:
						invType = ctx.arch.relocBytes != 2;
						break;
					case StdTypes.T_INT:
						break; //always allowed
					case StdTypes.T_LONG:
						invType = ctx.arch.relocBytes != 8;
						break;
					default:
						invType = true; //all other types are not valid
				}
			if (invType || nativeAddress.type.arrDim != 0 || nativeAddress.type.isObjType() || nativeAddress.type.isIntfType())
			{
				nativeAddress.printPos(ctx, "destination variable for ");
				ctx.out.print(name);
				ctx.out.print(" has invalid type");
				return false;
			}
			if ((nativeAddress.modifier & Modifier.M_STAT) == 0)
			{
				nativeAddress.printPos(ctx, "destination variable for native call needs to be static");
				return false;
			}
			mPar = param;
			while (mPar != null)
			{
				if ((mPar.type.baseType == StdTypes.T_QID && !mPar.type.isStructType()) || mPar.type.arrDim != 0)
				{
					mPar.type.printPos(ctx, "invalid parameter for native method ");
					printNamePar(ctx.out);
					return false;
				}
				mPar = mPar.nextParam;
			}
		}
		//for constructors: check if call to super is there
		if (isConstructor)
		{
			//reset MF_WRITTEN flag for non-static final variables in unit and enter unwrittenVars
			setWriteCheckState(false);
		}
		//build synchronize frame if requested
		if ((modifier & Modifier.M_SYNC) != 0)
		{
			if (isConstructor)
			{
				printPos(ctx, "can not synchronize constructor");
				return false;
			}
			sb = new StBlock(null, null, fileID, line, col);
			sy = new StSync(sb, (modifier & Modifier.M_STAT) != 0 ? StSync.SYNC_CLSS : StSync.SYNC_INST, fileID, line, col);
			sy.syncBlock = block;
			sb.stmts = sy;
			block = sb;
			ctx.syncUsed = true;
		}
		//resolve block (also sets varSize and MF_WRITTEN-flag)
		if (block != null)
		{
			if (((block.innerResolve(0, isConstructor, owner, this, ctx)) & Stmt.FA_ERROR) != 0)
			{
				ctx.out.print(" in method ");
				printNamePar(ctx.out);
				if (isConstructor)
					setWriteCheckState(true);
				return false;
			}
		}
		//check for optimizations and combination validity
		if (isConstructor)
		{
			//check if all variables that need initialization are initialized
			setWriteCheckState(true);
			//check explicit initialization of inline array
			if (owner.inlArr != null && (modifier & Modifier.M_EXINIT) == 0)
			{
				printPos(ctx, "need explicit instance assignment, use MAGIC.useAsThis(.) in method ");
				printNamePar(ctx.out);
				return false;
			}
			//check initialization of to be written variables
			vrblList = owner.writeCheckFinalVars;
			while (vrblList != null)
			{
				if ((vrblList.vrbl.modifier & Modifier.MF_ISWRITTEN) == 0)
				{
					printPos(ctx, "final variable ");
					ctx.out.print(vrblList.vrbl.name);
					ctx.out.print(" needs initialization in constructor ");
					printNamePar(ctx.out);
					return false;
				}
				vrblList = vrblList.next;
			}
			//check if redirection is possible: if block starts with super call, may be reduced only if super has explicit constructor
			call = (ExCall) ((StExpr) block.stmts).ex;
			if (call.id.equals(SJava.KEY_SUPER) && owner.instInitVars == null && call.dest != null && call.dest.owner.explConstr)
			{
				//first statement is the super call
				cs = block.stmts.nextStmt;
				//check if following statements are empty (at least one is generated by the parser)
				while (cs instanceof StEmpty)
					cs = cs.nextStmt;
				if (cs == null)
				{ //nothing more than the super-call and empty statements
					mPar = param;
					cPar = call.par;
					while (mPar != null)
					{
						if (cPar == null)
							break; //parameters in call are less than in ourself
						if (cPar.expr instanceof ExVar)
						{
							ep = (ExVar) cPar.expr;
							if (ep.dest != mPar)
								break; //parameter in call is not corresponding to our own one
						}
						else
							break;
						mPar = mPar.nextParam;
						cPar = cPar.nextParam;
					}
					//if all parameters are identical, redirection is possible, try to redirect until end
					if (mPar == null && cPar == null)
					{
						redirect = call.dest;
						while (redirect.redirect != null)
							redirect = redirect.redirect;
					}
				}
			}
		}
		else
		{ //check if inlining is possible and wanted
			if (stmtCnt <= ctx.maxStmtAutoInline && (modifier & (Modifier.M_ABSTR | Modifier.M_OVERLD | Modifier.M_HSCALL)) == 0 && (marker & Marks.K_NINL) == 0 && (owner.modifier & Modifier.M_INDIR) == 0)
			{
				if (ctx.verbose)
				{
					printPos(ctx, "auto-inline ");
					printSig(ctx.out);
					ctx.out.print(" in ");
					if (owner.pack != null)
					{
						owner.pack.printFullQID(ctx.out);
						ctx.out.print('.');
					}
					ctx.out.print(owner.name);
					ctx.out.print(" with stmtCnt==");
					ctx.out.println(stmtCnt);
				}
				marker |= Marks.K_FINL;
			}
		}
		//check if profiling is wanted
		if (ctx.globalProfiling || (marker & Marks.K_PROF) != 0)
		{
			ctx.profilerUsed = tryProfiling = true; //perhaps K_NPRF-marker is set later - has to be checked just in time
			if (ctx.dynaMem)
				runtimeClass = owner.getRefUnit(ctx.rteDynamicRuntime, true);
		}
		//check if stack extreme pointer should be checked
		if (ctx.globalStackExtreme || (marker & Marks.K_SEPC) != 0)
		{
			ctx.stackExtremeUsed = tryStackExtreme = true; //perhaps K_NSPC-marker is set later - has to be checked just in time
			if (ctx.dynaMem && runtimeClass == null)
				runtimeClass = owner.getRefUnit(ctx.rteDynamicRuntime, true);
		}
		//enter register of return value
		if (retType != null)
			retRegType = retType.getRegType(ctx);
		else if (isConstructor && (modifier & Modifier.M_EXINIT) != 0)
			retRegType = StdTypes.T_PTR;
		else
			retRegType = 0;
		//everything done
		isResolved = true;
		return true;
	}
	
	public void printCode(CodePrinter prnt)
	{
		if (block != null)
			block.printToken(prnt);
	}
	
	private void setWriteCheckState(boolean state)
	{
		VrblList vrblList = owner.writeCheckFinalVars;
		checkInitVars = state ? null : owner.writeCheckFinalVars;
		while (vrblList != null)
		{
			if (state)
				vrblList.vrbl.modifier |= Modifier.MF_ISWRITTEN;
			else
				vrblList.vrbl.modifier &= ~(Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN);
			vrblList = vrblList.next;
		}
	}
	
	public boolean handlesThrowable(Token whom, Unit thrown, Context ctx)
	{
		TryCaFiContainer trb = curTryFrame;
		
		while (trb != null)
		{
			if (trb.stTryCaFi.handlesThrowable(thrown, ctx))
				return true;
			trb = trb.nextTryCaFiBlock;
		}
		return super.handlesThrowable(whom, thrown, ctx);
	}
	
	public void genOutput(Context ctx)
	{
		int mthdID = 0;
		VrblList inits;
		Object obj;
		
		if (redirect != null)
		{
			if ((obj = redirect.outputLocation) == null)
			{ //redirection active
				ctx.out.println("destination of redirected method not built yet");
				ctx.err = true;
				return;
			}
		}
		else
		{ //generate code
			if (inGenOutput)
			{
				ctx.out.println("can not recursively generate output");
				ctx.err = true;
				return;
			}
			inGenOutput = true;
			ctx.arch.prepareMethodCoding(this);
			if (tryStackExtreme && (marker & (Marks.K_NSPC | Marks.K_INTR)) == 0)
				genStackExtremeCheck(ctx);
			ctx.arch.codeProlog();
			if (tryProfiling && (marker & Marks.K_NPRF) == 0)
				genProfilingCall(mthdID = ctx.mem.getCurrentAllocAmountHint() + ctx.mem.getBaseAddress() + ctx.rteSMthdBlock.instRelocTableEntries * ctx.arch.relocBytes, 0, ctx); //generate call and remember method ID
			if (isConstructor)
			{ //to be initialized instance variables
				inits = owner.instInitVars;
				while (inits != null)
				{
					inits.vrbl.genInitCode(false, ctx); //zero variables already initialized by runtime environment
					inits = inits.next;
				}
			}
			if (block != null)
				block.genOutput(ctx, isConstructor && (modifier & Modifier.M_EXINIT) != 0);
			if (ctx.err)
				return;
			if (isConstructor && (modifier & Modifier.M_EXINIT) != 0)
			{
				ctx.arch.genMoveToPrimary(ctx.arch.regInst, StdTypes.T_PTR);
				ctx.arch.genRestInstContext(); //restore saved instance context (done in block.genOutput)
			}
			if (tryProfiling && (marker & Marks.K_NPRF) == 0)
			{
				if (retRegType != 0)
					ctx.arch.genSavePrimary(retRegType);
				genProfilingCall(mthdID, 1, ctx); //generate call with already generated method id
				if (retRegType != 0)
					ctx.arch.genRestPrimary(retRegType);
			}
			ctx.arch.codeEpilog(null);
			ctx.arch.finalizeMethodCoding();
			codeSize = ctx.arch.getMethodSize();
			//allocate space for method-block
			if ((obj = ctx.mem.allocate(ctx.rteSMthdBlock.instScalarTableSize + codeSize, 0, ctx.rteSMthdBlock.instRelocTableEntries, ctx.leanRTE ? null : ctx.rteSMthdBlock.outputLocation)) == null)
			{
				ctx.out.println("error while allocating memory for method code");
				ctx.err = true;
				return;
			}
			ctx.mem.allocationDebugHint(this);
			//copy generated code and cleanup tokens
			ctx.arch.copyMethodCode(this, obj, ctx.codeStart);
			inGenOutput = false;
		}
		//everything done, check if we were referenced and enter valid output address
		enterOutputAddr(obj, ctx);
	}
	
	public void genInlineOutput(Context ctx)
	{
		Mthd outline;
		
		if (redirect != null)
			redirect.genInlineOutput(ctx);
		else
		{
			if (inGenOutput)
			{
				ctx.out.println("can not recursively inline");
				ctx.err = true;
				return;
			}
			inGenOutput = true;
			outline = ctx.arch.prepareMethodCoding(this);
			ctx.arch.codeProlog();
			if (block != null)
				block.genOutput(ctx);
			if (ctx.err)
				return;
			ctx.arch.codeEpilog(outline);
			inGenOutput = false;
		}
	}
	
	private void genProfilingCall(int id, int mode, Context ctx)
	{
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPushConstVal(id, StdTypes.T_INT);
		ctx.arch.genPushConstVal(mode, StdTypes.T_BYTE);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff); //load class context of runtime system
			ctx.arch.genCall(ctx.rteDRProfilerMd.relOff, ctx.arch.regClss, ctx.rteDRProfilerMd.parSize); //call runtime system
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteDRProfilerMd, ctx.rteDRProfilerMd.parSize);
	}
	
	private void genStackExtremeCheck(Context ctx)
	{
		Instruction success = ctx.arch.getUnlinkedInstruction();
		int value, regUnit;
		
		if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
		{
			compErr(ctx, REGNOTFREE);
			return;
		}
		value = ctx.arch.allocReg();
		if (ctx.dynaMem)
		{
			if (ctx.arch.prepareFreeReg(0, 0, value, StdTypes.T_PTR) != 0)
			{
				compErr(ctx, REGNOTFREE);
				return;
			}
			regUnit = ctx.arch.allocReg();
			ctx.arch.genLoadUnitContext(regUnit, runtimeClass.relOff);
			ctx.arch.genLoadVarVal(value, regUnit, null, ctx.rteStackExtreme, StdTypes.T_PTR);
			ctx.arch.deallocRestoreReg(regUnit, value, 0);
		}
		else
			ctx.arch.genLoadVarVal(value, 0, !ctx.embedded ? ctx.rteDynamicRuntime.outputLocation : ctx.ramLoc, ctx.rteStackExtreme, StdTypes.T_PTR);
		ctx.arch.genCheckStackExtreme(value, success);
		ctx.arch.deallocRestoreReg(value, 0, 0);
		if (ctx.dynaMem)
		{
			ctx.arch.genSaveUnitContext();
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff); //load class context of runtime system
			ctx.arch.genCall(ctx.rteDRStackExtremeErrMd.relOff, ctx.arch.regClss, ctx.rteDRStackExtremeErrMd.parSize); //call runtime system
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteDRStackExtremeErrMd, ctx.rteDRStackExtremeErrMd.parSize);
		ctx.arch.appendInstruction(success);
	}
}
