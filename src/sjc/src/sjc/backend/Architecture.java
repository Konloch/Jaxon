/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2015, 2016 Stefan Frenz
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

package sjc.backend;

import sjc.compbase.*;
import sjc.memory.MemoryImage;
import sjc.osio.TextPrinter;
import sjc.real.Real;

/**
 * Architecture: abstract class to handle backend access
 *
 * @author S. Frenz
 * @version 160818 added supportsAsmTextInline
 * version 151031 adopted changed buildAssemblerText concept
 * version 151026 added buildAssemblerText
 * version 150922 added inlineCodeOffset
 * version 150919 extended finalizeInstructionAddresses
 * version 150915 added support for finalizeInstructionAddresses
 * version 101222 added needsAlignedVrbls
 * version 101210 simplified genCall
 * version 101101 changed genLoadDerefAddr to support write once semantic
 * version 101018 added support for appending instruction lists
 * version 100923 added disposeMethodCode, reorganized finalizeMethodCoding
 * version 100922 added F_LOOPEND
 * version 100913 fixed instruction list resetting
 * version 100823 added F_CONDEND, added var init method insertZeroHint for backends that do not zero local variables
 * version 100805 moved instruction cleanup functionality to Instruction
 * version 100619 added setHeaderLength
 * version 100505 made use of newly added MemoryImage.putByteArray
 * version 100504 removed no longer used genNativeReturnMissing
 * version 100312 added nextValInFlash
 * version 100127 renamed ariCall into binAriCall, added unaAriCall
 * version 100115 replaced err-variable by fatalError-method, moved codeStart to Context
 * version 100114 removed unused method getClssInstReg
 * version 091104 added support for In-Symbol source code hinting
 * version 091103 removed unused variables/methods
 * version 091102 added source hint
 * version 091026 added genBinOpConst*
 * version 091013 cleaned up method signature of genStore*
 * version 091005 removed unneeded methods
 * version 091004 removed getSpecialValue, fixed inlineOffset after memory interface change
 * version 091001 adopted changed memory interface
 * version 090717 cleaned up stack extreme check, therefore inserted prepareMethodeCoding
 * version 090626 added support for stack extreme check
 * version 090619 added r* for standardized register access, removed M_* constants and adopted genLoadVarAddr, genLoadVarVal etc.
 * version 090430 added genNativeReturnMissingException
 * version 090409 added getSpecialValue
 * version 090219 removed genCopyInstContext, added getClssInstReg instead
 * version 090215 optimized copyMethodCode
 * version 090208 removed genClearMem
 * version 090207 added copyright notice and clarified comments for genStoreVar*, removed genPushNull and genLoadNullAddr
 * version 090206 added genStoreVar*
 * version 090113 added support for killed instructions even if in fixupCodeRefList
 * version 081231 made ctx public
 * version 081230 added printStatistics
 * version 081229 removed I_JUMP as this is no longar an architecture independent instruction
 * version 081021 added memLoc to gen*IO, added cond to genComp* with return value for genCondJmp
 * version 081016 added resetting iPar* and lPar on getUnlinkedInstruction and kill
 * version 080622 changed parameter naming of inlineVarOffset (only semantic may have changed), removed dummy-implementation for genThrowFrame*
 * version 080615 replaced relocOnStackBytes by throwFrame*
 * version 080604 added support for exceptions
 * version 080525 changed signature of genCondJmp, removed genMarker
 * version 080517 changed ariLongCall to ariCall-array to support calls for all basic types
 * version 080508 added insertFlowHint
 * version 080123 removed not needed cast of Instruction.refMthd, added resetMethodCode
 * version 080122 added getFirstInstr to support SSAToNative
 * version 080105 added genSavePrimary and genRestPrimary to support profiling
 * version 071001 added interface for native methods with default error handling
 * version 070913 added inlineVarOffset and reuseLocals, added clearMem
 * version 070815 added setParameter
 * version 070812 removed ariFloatDoubleCalls as there are no plans to implement all float/double rte calls
 * version 070808 added support for float and double
 * version 070706 fixed comment
 * version 070701 added codePrologDone
 * version 070628 added allocClearBits
 * version 070615 adopted removal of Instruction.realAddr, removed get* and prepareMethodCoding
 * version 070531 removed genLoadFromMem as it is replacable by genLoadVarVal with mode==M_RESU
 * version 070528 moved method statistics to Context
 * version 070504 changed naming of Clss to Unit, changed OutputObject to Unit
 * version 070501 removed no longer needed reference fixup list
 * version 070114 removed never called methods, reduced access level where possible
 * version 070113 changed genCheckNull to genCompPtrToNull
 * version 070101 added parSize in genCall
 * version 061231 moved isUntilNextRealIns hereto
 * version 061229 removed access to firstInstr
 * version 061225 added parameter "inline" in codeProlog
 * version 061211 removed insertInstruction as it is never used, moved debugCode to Context
 * version 061202 changed signature of genCall
 * version 061111 added retType to codeEpilog to support intelligent optimizer
 * version 060902 split off memory functionality (slower, but supports in-system compilation)
 * version 060808 added support for ImageContainer
 * version 060807 added flag-container for ari-call
 * version 060714 changed bound check to enable runtime handler
 * version 060706 added check for image size
 * version 060628 added support for static compilation
 * version 060616 inserted genCopyInstContext
 * version 060607 initial version
 */

public abstract class Architecture
{
	public final static int I_NONE = 0; //unused instruction
	public final static int I_MAGC = -1; //used for MAGIC
	
	public final static int F_BLOCKSTART = 1; //start of block, will return a method-unique id
	public final static int F_BLOCKEND = 2; //end of block
	public final static int F_CONDSTART = 3; //start of loop/jump-condition
	public final static int F_CONDEND = 4; //end of loop/jump-condition
	public final static int F_LOOPSTART = 5; //start of loop statements
	public final static int F_LOOPEND = 6; //end of loop statements
	public final static int F_TRUESTART = 7; //start of statements in matching if-block
	public final static int F_ELSESTART = 8; //start of statements in else-block
	public final static int F_SWSBSTART = 9; //start of non-conditional statements in switch-block
	public final static int F_TRYSTART = 10; //start of try-block
	public final static int F_CAFISTART = 11; //start of catch-/finally-block
	public final static int F_THRWMTHD = 12; //end of the rte-method to throw a language throwable
	
	public final static String INVALID_IO_LOC = "method contains invalid I/O location";
	
	public int relocBytes, stackClearBits, allocClearBits;
	public int maxInstrCodeSize;
	public int throwFrameSize, throwFrameExcOff; //size of throwFrame and offset of exception-variable inside throw-Frame
	public int regClss, regInst, regBase; //register for class, instance and stack frame, special "register" 0 for absolute addressing
	public Context ctx;
	protected MemoryImage mem;
	public Real real;
	public boolean needsAlignedVrbls;
	public boolean supportsAsmTextInline;
	
	//call runtime method on given arithmetic operations
	//  if used, highest bit will be set by user (resulting in ariCall[.]<0 for a specific baseType)
	public int[] binAriCall = new int[StdTypes.MAXBASETYPE + 1];
	public int[] unaAriCall = new int[StdTypes.MAXBASETYPE + 1];
	
	protected Mthd curMthd;
	protected int curInlineLevel;
	private int curMthdSize, maxInlineLevels;
	private Instruction firstInstr, lastInstr, nextFreeInstr;
	
	private InstrList fixupCodeRefList, emptyList;
	private boolean printedInlineExceed;
	private int[] sourceLineOffsetTempArray;
	
	//interface to pass parameters to special architectures
	public boolean setParameter(String parm, TextPrinter v)
	{
		v.println("no parameter required for selected architecture");
		return false;
	}
	
	//additional informations
	public void setHeaderLength(int length)
	{
	}
	
	//each architecture has to correct the pointer in size and construction
	public abstract void putRef(Object loc, int offset, Object ptr, int ptrOff);
	
	public abstract void putCodeRef(Object loc, int offset, Object ptr, int ptrOff);
	
	//each architecture has to implement code generation depending on the architecture
	//creation of instructions and method prolog/epilog
	public abstract void finalizeInstructions(Instruction first);
	
	public abstract Mthd prepareMethodCoding(Mthd mthd);
	
	public abstract void codeProlog();
	
	public abstract void codeEpilog(Mthd outline);
	
	//register allocation and de-allocation, some architectures may internally "or" regs bitwise together
	public abstract int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2);
	
	public abstract int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type); //returns registers to store before
	
	public abstract int allocReg(); //must follow ensureFreeReg, returns usable regs
	
	public abstract void deallocRestoreReg(int deallocRegs, int keepRegs, int restoreRegs); //release allocated register
	
	//general purpose instructions
	public abstract void genLoadConstVal(int dst, int val, int type); //may also be used for pointer
	
	public abstract void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble);
	
	public abstract void genLoadVarAddr(int dstReg, int srcReg, Object loc, int off);
	
	public abstract void genLoadVarVal(int dstReg, int srcReg, Object loc, int off, int type);
	
	public abstract void genConvertVal(int dst, int src, int toType, int fromType);
	
	public abstract void genDup(int dst, int src, int type);
	
	public abstract void genPushConstVal(int val, int type); //may also be used for pointer
	
	public abstract void genPushConstDoubleOrLongVal(long val, boolean asDouble);
	
	public abstract void genPush(int src, int type);
	
	public abstract void genPop(int dst, int type);
	
	public abstract void genAssign(int dst, int src, int type);
	
	public abstract void genBinOp(int dst, int src1, int src2, int op, int type); //may destroy src1/2
	
	public abstract void genUnaOp(int dst, int src, int op, int type);
	
	public abstract void genIncMem(int dst, int type);
	
	public abstract void genDecMem(int dst, int type);
	
	public abstract void genSaveUnitContext();
	
	public abstract void genRestUnitContext();
	
	public abstract void genLoadUnitContext(int dst, int off);
	
	public abstract void genLoadConstUnitContext(int dst, Object unitLoc);
	
	public abstract void genSaveInstContext();
	
	public abstract void genRestInstContext();
	
	public abstract void genLoadInstContext(int src);
	
	public abstract void genCall(int off, int clssReg, int parSize);
	
	public abstract void genCallIndexed(int intfReg, int off, int parSize);
	
	public abstract void genCallConst(Mthd obj, int parSize);
	
	public abstract void genJmp(Instruction dest);
	
	public abstract void genCondJmp(Instruction dest, int condHnd);
	
	//compares return a condition handle for a following genCondJmp
	public abstract int genComp(int src1, int src2, int type, int cond);
	
	public abstract int genCompValToConstVal(int src, int val, int type, int cond);
	
	public abstract int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond);
	
	public abstract int genCompPtrToNull(int reg, int cond);
	
	//instructions for i/o-access
	public abstract void genWriteIO(int dst, int src, int type, int memLoc);
	
	public abstract void genReadIO(int dst, int src, int type, int memLoc);
	
	//instructions to handle results and interface-pointers
	public abstract void genMoveToPrimary(int src, int type);
	
	public abstract void genMoveFromPrimary(int dst, int type);
	
	public abstract void genMoveIntfMapFromPrimary(int dst);
	
	public abstract void genSavePrimary(int type);
	
	public abstract void genRestPrimary(int type);
	
	//instructions to handle arrays, stack- and pointer-checks
	public abstract void genCheckBounds(int addrReg, int offReg, int checkToOffset, Instruction onSuccess);
	
	public abstract void genCheckStackExtreme(int maxValueReg, Instruction onSuccess);
	
	public abstract void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize);
	
	//native instructions to show up with an internal exception, not all have to be implemented
	public void genNativeBoundException()
	{
		fatalError("Please use programmed bound exception or disable checks for this architecture");
	}
	
	//instructions to support exceptions (not implemented by all architectures)
	//typical space reserved on stack requiring 7*relocOnStackBytes:
	//  excOffset + 6*relocOnStackBytes: current throwable thrown
	//  excOffset + 5*relocOnStackBytes: stack pointer for current try-block
	//  excOffset + 4*relocOnStackBytes: stack frame pointer for current try-block
	//  excOffset + 3*relocOnStackBytes: instance context of current try-block
	//  excOffset + 2*relocOnStackBytes: unit context of current try-block
	//  excOffset + 1*relocOnStackBytes: code-byte to jump to if exception is thrown
	//  excOffset                      : pointer to last excStackFrame
	public abstract void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset);
	
	public abstract void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset);
	
	public abstract void genThrowFrameReset(int globalAddrReg, int throwBlockOffset);
	
	//optionally overwritten by a better implementation, default implementation is straight forward
	public void genStoreVarVal(int objReg, Object loc, int off, int valReg, int type)
	{
		int addr, restore;
		restore = prepareFreeReg(valReg, objReg, 0, StdTypes.T_PTR);
		addr = allocReg();
		genLoadVarAddr(addr, objReg, loc, off);
		genAssign(addr, valReg, type);
		deallocRestoreReg(addr, 0, restore);
	}
	
	public void genStoreVarConstVal(int objReg, Object loc, int off, int val, int type)
	{
		int valReg, restore;
		restore = prepareFreeReg(objReg, 0, 0, type);
		valReg = allocReg();
		genLoadConstVal(valReg, val, type);
		genStoreVarVal(objReg, loc, off, valReg, type);
		deallocRestoreReg(valReg, 0, restore);
	}
	
	public void genStoreVarConstDoubleOrLongVal(int objReg, Object loc, int off, long val, boolean asDouble)
	{
		int valReg, restore, type = asDouble ? StdTypes.T_DBL : StdTypes.T_LONG;
		restore = prepareFreeReg(objReg, 0, 0, type);
		valReg = allocReg();
		genLoadConstDoubleOrLongVal(valReg, val, asDouble);
		genStoreVarVal(objReg, loc, off, valReg, type);
		deallocRestoreReg(valReg, 0, restore);
	}
	
	public void genBinOpConstRi(int dst, int src1, int val, int op, int type)
	{ //may destroy src1
		int src2, restore, valType = ((op >>> 16) == Ops.S_BSH) ? StdTypes.T_INT : type;
		restore = prepareFreeReg(dst, src1, 0, valType);
		src2 = allocReg();
		genLoadConstVal(src2, val, valType);
		genBinOp(dst, src1, src2, op, type);
		deallocRestoreReg(src2, 0, restore);
	}
	
	public void genBinOpConstDoubleOrLongRi(int dst, int src1, long val, int op, boolean asDouble)
	{
		int src2, restore, type = asDouble ? StdTypes.T_DBL : StdTypes.T_LONG;
		restore = prepareFreeReg(dst, src1, 0, type);
		src2 = allocReg();
		genLoadConstDoubleOrLongVal(src2, val, asDouble);
		genBinOp(dst, src1, src2, op, type);
		deallocRestoreReg(src2, 0, restore);
	}
	
	//special instructions for better inline coding support
	public abstract void inlineVarOffset(int inlineMode, int objReg, Object loc, int offset, int baseValue);
	
	public void inlineCodeAddress(boolean define, int offset)
	{
		fatalError("inlineCodeAddress not supported");
	}
	
	//special call for native methods (not implemented by all architectures)
	public void genCallNative(Object loc, int off, boolean relative, int parSize, boolean noCleanUp)
	{
		fatalError("genCallNative not supported");
	}
	
	public void genReserveNativeStack(int size)
	{
		fatalError("genReserveNativeStack not supported");
	}
	
	public void genStoreNativeParameter(int offset, int src, int type)
	{
		fatalError("genStoreNativeParameter not supported");
	}
	
	//modify next instruction
	public void nextValInFlash()
	{
		fatalError("nextValInFlash not supported");
	}
	
	//inserting hints
	public void insertKillHint(int deallocRegs)
	{
	} //needed only for emulated architectures
	
	public int insertFlowHint(int hint, int id)
	{
		return 0;
	} //needed only for emulated architectures
	
	public void insertSourceHint(Token token)
	{ //insert nop with hint to token
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.token = token;
	}
	
	public void insertZeroHint(int objReg, int offset, int type)
	{
	} //needed only for architectures that do not zero local variables
	
	//called at end of compilation, implementing classes may report some statistics there
	public void printStatistics()
	{
	}
	
	//optionally try to build assembler output
	public String checkBuildAssembler(Context preInitCtx)
	{
		preInitCtx.out.println("assembler output not supported");
		preInitCtx.err = true;
		return null;
	}
	
	protected void attachMethodAssemblerText(Mthd generatingMthd, Instruction first)
	{
	}
	
	public void init(MemoryImage imem, int ilev, Context ictx)
	{
		mem = imem;
		maxInlineLevels = ilev;
		ctx = ictx;
		lastInstr = firstInstr = getUnlinkedInstruction();
	}
	
	public void fatalError(String msg)
	{
		ctx.out.print("fatal arch error ");
		if (msg != null)
			ctx.out.print(msg);
		ctx.out.println();
		ctx.err = true;
	}
	
	//generic common methods
	public boolean mayInline()
	{ //may be overwritten if architecture does not support method inling
		if (curInlineLevel < maxInlineLevels)
			return true;
		if (ctx.verbose)
		{
			if (!printedInlineExceed)
				ctx.out.println("warning: exceeded inline level maximum");
			printedInlineExceed = false;
		}
		return false;
	}
	
	//default instruction factory
	public Instruction createNewInstruction()
	{
		return new Instruction(maxInstrCodeSize);
	}
	
	//generic code-generation methods
	public Instruction getUnlinkedInstruction()
	{
		Instruction res;
		
		if ((res = nextFreeInstr) == null)
			res = createNewInstruction();
		else
			nextFreeInstr = nextFreeInstr.next;
		res.cleanup();
		return res;
	}
	
	public void appendInstruction(Instruction who)
	{
		who.prev = lastInstr;
		lastInstr.next = who;
		lastInstr = who;
		while (lastInstr.next != null)
			lastInstr = lastInstr.next; //support for appending list of instructions
	}
	
	public void finalizeMethodCoding()
	{
		enumerateInstructions();
		finalizeInstructions(firstInstr.next);
		detectFinalMethodSize();
	}
	
	public void finalizeInstructionAddresses(Mthd generatingMthd, Instruction first, Object loc, int offset)
	{
		//default: nothing to do
	}
	
	public int getMethodSize()
	{
		return curMthdSize;
	}
	
	public void copyMethodCode(Mthd generatingMthd, Object loc, int offset)
	{
		Instruction ins = firstInstr.next, last;
		InstrList temp;
		int i, lastOffset = -1, lastLine = 0, nextFreeIndex = 0;
		boolean slhi = false;
		int[] tmpArray;
		
		//report destination if architecture needs absolute calls inside itself
		finalizeInstructionAddresses(generatingMthd, firstInstr.next, loc, offset);
		//attach assembler text to method if requested
		if (ctx.buildAssemblerText)
			attachMethodAssemblerText(generatingMthd, firstInstr.next);
		//insert the bytes of all instructions
		if (ctx.globalSourceLineHints || (generatingMthd.marker & Marks.K_SLHI) != 0)
		{
			slhi = true;
			lastLine = generatingMthd.line;
			if (sourceLineOffsetTempArray == null)
				sourceLineOffsetTempArray = new int[60];
		}
		while (ins != null)
		{
			if (slhi && ins.token != null)
			{ //add source hint
				if (lastLine != ins.token.line)
				{
					if (lastOffset == offset && nextFreeIndex >= 2)
						nextFreeIndex -= 2; //last hint has no code, overwrite it
					else if (sourceLineOffsetTempArray.length < nextFreeIndex + 2)
					{ //array is too small
						tmpArray = new int[sourceLineOffsetTempArray.length * 2];
						for (i = 0; i < nextFreeIndex; i++)
							tmpArray[i] = sourceLineOffsetTempArray[i];
						sourceLineOffsetTempArray = tmpArray;
					}
					sourceLineOffsetTempArray[nextFreeIndex++] = lastOffset = offset; //remember offset
					sourceLineOffsetTempArray[nextFreeIndex++] = lastLine = ins.token.line; //remember line
				}
			}
			ins.iPar1 = offset; //iPar1 will not be used anymore, recycle it
			mem.putByteArray(loc, offset, ins.code, ins.size); //copy code of current instruction
			offset += ins.size; //increase offset by size of last instruction
			ins = (last = ins).next; //get next instruction
			last.next = nextFreeInstr; //recycle instruction
			nextFreeInstr = last;
		}
		if (slhi && nextFreeIndex > 0)
		{ //copy temporary array into method-array
			generatingMthd.lineInCodeOffset = tmpArray = new int[nextFreeIndex];
			for (i = 0; i < nextFreeIndex; i++)
				tmpArray[i] = sourceLineOffsetTempArray[i];
		}
		//reset lastInstr for next method
		(lastInstr = firstInstr).next = null;
		//replace the to be fixed up code-references
		while (fixupCodeRefList != null)
		{
			//enter the reference if instruction is still in use
			if (fixupCodeRefList.instr.size > 0)
				fixupCodeRefList.instr.refMthd.enterCodeRef(loc, fixupCodeRefList.instr.iPar1 + fixupCodeRefList.offset, ctx);
			//recycle the list element, as it is used no more
			temp = fixupCodeRefList.next;
			fixupCodeRefList.next = emptyList;
			emptyList = fixupCodeRefList;
			fixupCodeRefList = temp;
		}
	}
	
	public void disposeMethodCode()
	{ //needed only by multi-stage-optimizer
		Instruction ins = firstInstr.next, last;
		
		while (ins != null)
		{
			ins = (last = ins).next; //get next instruction
			last.next = nextFreeInstr; //recycle instruction
			nextFreeInstr = last;
		}
		lastInstr = firstInstr;
	}
	
	protected void detectFinalMethodSize()
	{
		int size = 0;
		Instruction ins = firstInstr;
		
		while (ins != null)
		{
			size += ins.size;
			ins = ins.next;
		}
		ctx.mthdCodeSize += curMthdSize = size;
		ctx.mthdCount++;
	}
	
	protected void enumerateInstructions()
	{
		int nr = 0;
		Instruction ins = firstInstr;
		
		while (ins != null)
		{
			ins.instrNr = nr++;
			ins = ins.next;
		}
	}
	
	protected void addToCodeRefFixupList(Instruction i, int offset)
	{
		InstrList dest;
		
		if (emptyList == null)
			dest = new InstrList();
		else
		{
			dest = emptyList;
			emptyList = emptyList.next;
		}
		dest.instr = i;
		dest.offset = offset;
		dest.next = fixupCodeRefList;
		fixupCodeRefList = dest;
	}
	
	protected boolean isUntilNextRealIns(Instruction i, Instruction until)
	{
		while (i != until)
		{
			if (i == null || i.type != I_NONE)
				return false;
			i = i.next;
		}
		return true;
	}
	
	public void kill(Instruction i)
	{
		if (i.refMthd != null)
		{
			fatalError("instruction to be killed has refMthd!=null");
			return;
		}
		i.type = I_NONE;
		i.size = i.reg0 = i.reg1 = i.reg2 = 0;
		i.iPar1 = i.iPar2 = i.iPar3 = 0;
		i.lPar = 0l;
	}
}
