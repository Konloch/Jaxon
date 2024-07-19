/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Stefan Frenz
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

package sjc.backend.ssa;

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;

/**
 * SSA: backend for artificial machine with "write once" register semantic
 *
 * @author S. Frenz
 * @version 110127 added I_ASSIGNcmplx and I_LOADcmplx instructions used by SSA-optimizer
 * version 101210 adopted changed Architecture
 * version 101102 fixed maximum size of instruction after extending I_DEREF
 * version 101101 adopted changed Architecture (extended I_DEREF)
 * version 101031 fixed instruction print for I_MAGC
 * version 100927 fixed unsignedness of chars
 * version 100922 removed optimization for genLoadVarVal if base address is R_BASE
 * version 100823 implemented insertZeroHint
 * version 100427 added printCode functions
 * version 100115 adopted changed error reporting
 * version 100114 removed unused method
 * version 091004 fixed inlineVarOffset after memory interface change
 * version 091001 adopted changed memory interface
 * version 090717 added support for stack extreme check and adopted changed Architecture
 * version 090626 adopted changed Architecture
 * version 090319 added check for optimizable case in genLoadVarVal
 * version 090310 fixed internal error detection in fixJDest and finalizeInstructions
 * version 090219 adopted changed Architecture (genCopyInstContext<->getClssInstReg)
 * version 090208 removed genClearMem
 * version 090207 added copyright notice and removed genLoadNullAddr as it is no longer used
 * version 081021 adopted changes in Architecture
 * version 080622 adopted changed naming of inlineVarOffset-parameter (no effect for SSA)
 * version 080616 added support for language throwables, changed allocation of instructions to enable full sized inline instructions
 * version 080525 adopted changed genCondJmp signature, removed genMarker
 * version 080518 moved createNewInstruction to LittleEndian
 * version 080508 added insertFlowHint
 * version 080207 changed semantics of I_IVOF to hold all parameters
 * version 080203 added support for method inlining, added prepareFinalization to support SSAToNative, changed float/double handling
 * version 080123 added seetting of mthd-object for enter- and leave-instruction
 * version 080122 fixed linefeed in error-message
 * version 080105 added genSavePrimary and genRestPrimary
 * version 070913 added new instruction for inlineVarOffset, genClearMem
 * version 070809 prepared for float and double
 * version 070628 added allocClearBits
 * version 070615 removed no longer needed getRef
 * version 070606 shortened internal error messages
 * version 070601 optimized codeInstruction
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070505 changed naming of Clss to Unit, changed OutputObject to int
 * version 070501 optimized insPatched
 * version 070114 added parSize in genCall, removed never called methods, reduced access level where possible
 * version 070113 adopted change of genCheckNull to genCompPtrToNull
 * version 070101 fixed fixJump, adopted change in genCall
 * version 061231 fixed invalid jump elimination
 * version 061229 removed access to firstInstr
 * version 061225 adopted change in codeProlog
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061112 bugfix in genUnaOp
 * version 061111 adopted change of Architecture.codeEpilog
 * version 061109 added support for Ops.C_BO
 * version 060628 added support for static compilation
 * version 060620 added kill-on-jump
 * version 060619 added length of method after leave-instruction
 * version 060616 inserted genCopyInstContext
 * version 060611 added register chain to support ensureFreeRegs
 * version 060610 changed jump conditions to SSAdef
 * version 060608 fixed long-constant
 * version 060607 initial version
 */

public class SSA extends Architecture
{
	//method dependant variables
	protected int regCnt;
	private int nextAllocReg;
	private SSAReg fReg, lReg, eReg; //first / last allocated and first empty register
	private int blockInMthdID;
	private Mthd mthdContainer;
	
	//initialization
	public SSA(int iRB)
	{
		relocBytes = iRB; //save one reloc on the stack
		allocClearBits = stackClearBits = relocBytes - 1;
		maxInstrCodeSize = 24;
		throwFrameSize = relocBytes * 7;
		throwFrameExcOff = relocBytes * 6;
		regClss = SSADef.R_CLSS;
		regInst = SSADef.R_INST;
		regBase = SSADef.R_BASE;
	}
	
	public Instruction createNewInstruction()
	{
		return new Instruction(maxInstrCodeSize + 2); //for MAGIC.inline two additional bytes are required
	}
	
	//references are treated as normal integers
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	//register allocation and de-allocation
	public int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2)
	{
		int cnt = 0, sc;
		SSAReg reg = lReg, lSaved = null;
		
		while (reg != null)
		{
			if (reg.saveStopper > 0)
				break;
			if (reg.nr != ignoreReg1 && reg.nr != ignoreReg2)
			{
				sc = 1;
				reg.saved++;
			}
			else
			{
				if (reg.saved != 0)
					fatalError("SSA: can not ignore already saved register");
				sc = 0;
			}
			ins(SSADef.I_SAVE, reg.nr, 0, 0, reg.size, 0l, sc);
			cnt++;
			lSaved = reg;
			reg = reg.prev;
		}
		if (lSaved != null)
			lSaved.saveStopper++;
		return cnt;
	}
	
	public int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type)
	{
		SSAReg reg;
		int typeID = getTypeID(type);
		
		//never reuse a register, so there is never a conflict: avoidReg1/2 not relevant
		nextAllocReg = regCnt++;
		ins(SSADef.I_ALLOCREG, nextAllocReg, 0, 0, typeID);
		//enter register in used register chain
		if (eReg != null)
		{
			reg = eReg;
			reg.prev = reg.next = null;
			eReg = eReg.next;
		}
		else
			reg = new SSAReg();
		reg.nr = nextAllocReg;
		reg.size = typeID;
		reg.saveStopper = reg.saved = 0;
		if (lReg == null)
			fReg = lReg = reg;
		else
		{
			lReg.next = reg;
			reg.prev = lReg;
			lReg = reg;
		}
		//return: nothing to restore
		return 0; //nothing has to be freed
	}
	
	public int allocReg()
	{
		return nextAllocReg;
	}
	
	public void deallocRestoreReg(int deallocRegs, int keepRegs, int restore)
	{
		int rc;
		SSAReg cmp;
		
		//restore register
		if (restore != 0)
		{
			cmp = lReg;
			//search saveStopper
			while (cmp != null && cmp.saveStopper == 0)
				cmp = cmp.prev;
			if (cmp == null)
				fatalError("SSA: invalid saveStopper (null)");
			else
			{ //restore registers
				cmp.saveStopper--;
				while (cmp != null)
				{
					if (cmp.saved > 0)
					{
						rc = 1;
						cmp.saved--;
					}
					else
						rc = 0;
					ins(SSADef.I_REST, cmp.nr, 0, 0, cmp.size, 0l, rc);
					restore--;
					cmp = cmp.next;
				}
				if (restore != 0)
					fatalError("SSA: invalid saveStopper (restore)");
			}
		}
		//release killed register
		if (keepRegs != deallocRegs && deallocRegs != 0)
		{
			ins(SSADef.I_KILLREG, deallocRegs);
			//free register
			if (lReg.nr != deallocRegs)
				fatalError("SSA: not last register in dealloc");
			else if (lReg.saved > 0 || lReg.saveStopper > 0)
				fatalError("SSA: dealloc for saved/saveStopper register");
			else
			{
				cmp = lReg;
				lReg = lReg.prev;
				if (lReg != null)
					lReg.next = null;
				else
					fReg = null; //no last register => no first register
				cmp.next = eReg;
				eReg = cmp;
			}
		}
	}
	
	public void insertKillHint(int deallocRegs)
	{
		if (deallocRegs != 0)
			ins(SSADef.I_KILLOJMP, deallocRegs);
	}
	
	public int insertFlowHint(int hint, int id)
	{
		if (hint == F_BLOCKSTART)
			id = ++blockInMthdID;
		ins(SSADef.I_FLOWHINT, 0, 0, 0, hint, 0l, id);
		return blockInMthdID;
	}
	
	public void insertZeroHint(int objReg, int offset, int type)
	{
		int resAddr, addr, resVal, val;
		
		resAddr = prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = allocReg();
		genLoadVarAddr(addr, objReg, null, offset);
		resVal = prepareFreeReg(addr, 0, 0, type);
		val = allocReg();
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_FLT:
			case StdTypes.T_PTR:
			case StdTypes.T_DPTR:
			case StdTypes.T_BOOL:
				genLoadConstVal(val, 0, type);
				break;
			case StdTypes.T_LONG:
				genLoadConstDoubleOrLongVal(val, 0l, false);
				break;
			case StdTypes.T_DBL:
				genLoadConstDoubleOrLongVal(val, 0l, true);
				break;
			default:
				fatalError("unsupported type for insertZeroHint");
				return;
		}
		genAssign(addr, val, type);
		deallocRestoreReg(val, 0, resVal);
		deallocRestoreReg(addr, 0, resAddr);
	}
	
	private int getTypeID(int type)
	{
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				return 1;
			case StdTypes.T_SHRT:
				return 2;
			case StdTypes.T_CHAR:
				return 3;
			case StdTypes.T_INT:
				return 4;
			case StdTypes.T_FLT:
				return 5;
			case StdTypes.T_LONG:
				return 8;
			case StdTypes.T_DBL:
				return 9;
			case StdTypes.T_PTR:
				return -1; //return relocBytes;
			case StdTypes.T_DPTR:
				return -2; //return relocBytes*2;
			case StdTypes.T_NULL:
				fatalError("unresolved null-type in getSize");
				return 0;
		}
		fatalError("unsupported type for getSize");
		return 0;
	}
	
	protected void optimize(Instruction first)
	{
	}
	
	public void prepareFinalization(Instruction first)
	{
		Instruction now;
		
		//enter jumpdestinations
		now = first;
		while (now != null)
		{ //isDest is initialized to false
			if (now.type == SSADef.I_JUMP)
				now.jDest.isDest = true;
			now = now.next;
		}
		optimize(first);
	}
	
	public void finalizeInstructions(Instruction first)
	{
		Instruction now;
		int diff = 0;
		
		//prepare list
		prepareFinalization(first);
		//create the instructions
		now = first;
		while (now != null)
		{
			codeInstruction(now);
			now = now.next;
		}
		//fix jumps and mthd-refs
		now = first;
		while (now != null)
		{
			switch (now.type)
			{
				case SSADef.I_JUMP:
				case SSADef.I_TFBUILD:
				case SSADef.I_TFUPD1:
				case SSADef.I_TFUPD2:
					fixJDest(now);
					break;
				case SSADef.I_CALLim_p:
					addToCodeRefFixupList(now, now.size - 8);
					break;
			}
			now = now.next;
		}
		//fixup marker after leave
		now = first;
		while (now != null && now.type != SSADef.I_MARKER)
			now = now.next; //search first marker
		while (now != null && (now.type != SSADef.I_LEAVE || now.next.type != SSADef.I_MARKER))
		{ //add all instructions until we reach a leave
			diff += now.size;
			now = now.next;
		}
		if (now == null)
		{
			fatalError("SSA-method without ENTER/LEAVE+MARKER");
			return;
		}
		diff += now.size; //add size of leave
		now = now.next;
		now.iPar1 = diff + now.size; //recode marker
		codeInstruction(now);
		//print code if requested
		if (ctx.printCode || ((mthdContainer.marker & Marks.K_PRCD) != 0))
			printCode(first);
	}
	
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		Mthd oldMthd = curMthd;
		if (oldMthd == null)
		{
			regCnt = SSADef.R_GPRS;
			ins(SSADef.I_MARKER, 0, 0, 0, mthd.marker);
			ins(SSADef.I_NFREG);
			curInlineLevel = blockInMthdID = 0;
			mthdContainer = mthd;
			
		}
		else
			curInlineLevel++;
		curMthd = mthd;
		return oldMthd;
	}
	
	
	public void codeProlog()
	{
		ins(curInlineLevel == 0 ? SSADef.I_ENTER : SSADef.I_ENTERINL, 0, 0, 0, curMthd.varSize, 0l, curMthd.parSize).refMthd = curMthd;
	}
	
	public void codeEpilog(Mthd outline)
	{
		if ((curMthd.marker & Marks.K_THRW) != 0)
			insertFlowHint(F_THRWMTHD, SSADef.F_DOTHROW);
		if (outline == null)
		{
			ins(SSADef.I_LEAVE, 0, 0, 0, curMthd.varSize, 0l, curMthd.parSize);
			ins(SSADef.I_MARKER, 0, 0, 0, -1); //value will be set in method coding
		}
		else
		{
			ins(SSADef.I_LEAVEINL, 0, 0, 0, curMthd.varSize, 0l, curMthd.parSize).refMthd = outline;
			curInlineLevel--;
		}
		curMthd = outline;
	}
	
	//general purpose instructions
	public void genLoadConstVal(int dst, int val, int type)
	{
		switch (type)
		{
			case StdTypes.T_PTR:
				if (val == 0)
					ins(SSADef.I_LOADnp, dst, 0, 0, getTypeID(StdTypes.T_PTR));
				else
					ins(SSADef.I_LOADim_p, dst, 0, 0, 0, 0l, val);
				break;
			case StdTypes.T_DPTR:
				if (val != 0)
				{
					fatalError("genLoadConstVal for T_DPTR with val!=0");
					return;
				}
				ins(SSADef.I_LOADnp, dst, 0, 0, getTypeID(StdTypes.T_DPTR));
				break;
			default:
				ins(SSADef.I_LOADim_i, dst, 0, 0, getTypeID(type), 0l, val);
		}
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		ins(SSADef.I_LOADim_l, dst, 0, 0, asDouble ? getTypeID(StdTypes.T_DBL) : getTypeID(StdTypes.T_LONG), val);
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int iPos, restore, pos = mem.getAddrAsInt(loc, off);
		
		if (src == 0)
		{ //absolute addressing mode
			restore = prepareFreeReg(0, 0, 0, StdTypes.T_INT);
			iPos = allocReg();
			genLoadConstVal(iPos, pos, StdTypes.T_INT);
			genConvertVal(dst, iPos, StdTypes.T_PTR, StdTypes.T_INT);
			deallocRestoreReg(iPos, 0, restore);
			return;
		}
		ins(SSADef.I_LOADaddr, dst, src, 0, pos);
	}
	
	public void genLoadVarVal(int dst, int src, Object loc, int off, int type)
	{
		int restore, addr, pos = mem.getAddrAsInt(loc, off);
		if (pos == 0 && src != SSADef.R_BASE)
			ins(SSADef.I_LOADval, dst, src, 0, getTypeID(type)); //address already existing and not in stack frame
		else
		{ //calculate address, may be it will be used
			restore = prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
			addr = allocReg();
			genLoadVarAddr(addr, src, loc, off);
			ins(SSADef.I_LOADval, dst, addr, 0, getTypeID(type));
			deallocRestoreReg(addr, 0, restore);
		}
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		if (toType == StdTypes.T_DPTR || fromType == StdTypes.T_DPTR)
		{
			fatalError("double pointers are not supported in genConvertVal");
		}
		else
			ins(SSADef.I_CONV, dst, src, 0, getTypeID(toType), 0l, getTypeID(fromType));
	}
	
	public void genDup(int dst, int src, int type)
	{
		ins(SSADef.I_COPY, dst, src, 0, getTypeID(type));
	}
	
	public void genPushConstVal(int val, int type)
	{
		if (type == StdTypes.T_PTR || type == StdTypes.T_DPTR)
		{
			if (val != 0)
				fatalError("genPushNull with invalid type");
			else
			{
				ins(SSADef.I_PUSHnp);
				if (type == StdTypes.T_DPTR)
					ins(SSADef.I_PUSHnp);
			}
		}
		else
			ins(SSADef.I_PUSHim_i, 0, 0, 0, getTypeID(type), 0l, val);
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		ins(SSADef.I_PUSHim_l, 0, 0, 0, 0, val);
	}
	
	public void genPush(int src, int type)
	{
		ins(SSADef.I_PUSH, src, 0, 0, getTypeID(type));
	}
	
	public void genPop(int dst, int type)
	{
		ins(SSADef.I_POP, dst, 0, 0, getTypeID(type));
	}
	
	public void genAssign(int dst, int src, int type)
	{
		ins(SSADef.I_ASSIGN, dst, src, 0, getTypeID(type));
	}
	
	public void genBinOp(int dst, int src1, int src2, int op, int type)
	{ //may destroy src2R
		int opPar = op & 0xFFFF, opType, typeID;
		
		if (type == StdTypes.T_BOOL)
		{
			opType = op >>> 16;
			if (opType != Ops.S_ARI || !(opPar == Ops.A_AND || opPar == Ops.A_OR || opPar == Ops.A_XOR))
			{
				fatalError("unsupported operation for bool-genBinOp");
				return;
			}
		}
		typeID = getTypeID(type);
		switch (opPar)
		{
			case Ops.A_AND:
				ins(SSADef.I_AND, dst, src1, src2, typeID);
				return;
			case Ops.A_XOR:
				ins(SSADef.I_XOR, dst, src1, src2, typeID);
				return;
			case Ops.A_OR:
				ins(SSADef.I_OR, dst, src1, src2, typeID);
				return;
			case Ops.A_PLUS:
				ins(SSADef.I_ADD, dst, src1, src2, typeID);
				return;
			case Ops.A_MINUS:
				ins(SSADef.I_SUB, dst, src1, src2, typeID);
				return;
			case Ops.A_MUL:
				ins(SSADef.I_MUL, dst, src1, src2, typeID);
				return;
			case Ops.A_DIV:
				ins(SSADef.I_DIV, dst, src1, src2, typeID);
				return;
			case Ops.A_MOD:
				ins(SSADef.I_MOD, dst, src1, src2, typeID);
				return;
			case Ops.B_SHL:
				ins(SSADef.I_SHL, dst, src1, src2, typeID);
				return;
			case Ops.B_SHRL:
				ins(SSADef.I_SHRL, dst, src1, src2, typeID);
				return;
			case Ops.B_SHRA:
				ins(SSADef.I_SHRA, dst, src1, src2, typeID);
				return;
		}
		fatalError("unsupported operation for genBinOp");
	}
	
	public void genUnaOp(int dst, int src, int op, int type)
	{
		int opPar = op & 0xFFFF, typeID;
		
		if (type == StdTypes.T_BOOL)
		{
			if (opPar == Ops.L_NOT)
			{ //do not use I_NOT, would change 0x01 to 0xFE instead of 0x00
				ins(SSADef.I_BINV, dst, src);
				return;
			}
			else
			{
				fatalError("unsupported bool-operation for genUnaOp");
				return;
			}
		}
		typeID = getTypeID(type);
		switch (opPar)
		{
			case Ops.A_CPL:
				ins(SSADef.I_NOT, dst, src, 0, typeID);
				return;
			case Ops.A_MINUS:
				ins(SSADef.I_NEG, dst, src, 0, typeID);
				return;
			case Ops.A_PLUS:
				ins(SSADef.I_COPY, dst, src, 0, typeID);
				return;
		}
		fatalError("unsupported operation for genUnaOp");
	}
	
	public void genIncMem(int dst, int type)
	{
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
				ins(SSADef.I_INCmem, dst, 0, 0, getTypeID(type));
				return;
		}
		fatalError("unsupported type for genIncMem");
	}
	
	public void genDecMem(int dst, int type)
	{
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
				ins(SSADef.I_DECmem, dst, 0, 0, getTypeID(type));
				return;
		}
		fatalError("unsupported type for genDecMem");
	}
	
	public void genSaveUnitContext()
	{
		ins(SSADef.I_PUSH, SSADef.R_CLSS, 0, 0, getTypeID(StdTypes.T_PTR));
	}
	
	public void genRestUnitContext()
	{
		ins(SSADef.I_POP, SSADef.R_CLSS, 0, 0, getTypeID(StdTypes.T_PTR));
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		int restore, addr;
		restore = prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = allocReg();
		genLoadVarAddr(addr, SSADef.R_CLSS, null, off);
		ins(SSADef.I_LOADval, dst, addr, 0, getTypeID(StdTypes.T_PTR));
		deallocRestoreReg(addr, 0, restore);
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		ins(SSADef.I_LOADim_p, dst, 0, 0, mem.getAddrAsInt(unitLoc, 0));
	}
	
	public void genSaveInstContext()
	{
		ins(SSADef.I_PUSH, SSADef.R_INST, 0, 0, getTypeID(StdTypes.T_PTR));
		ins(SSADef.I_PUSH, SSADef.R_CLSS, 0, 0, getTypeID(StdTypes.T_PTR));
	}
	
	public void genRestInstContext()
	{
		ins(SSADef.I_POP, SSADef.R_CLSS, 0, 0, getTypeID(StdTypes.T_PTR));
		ins(SSADef.I_POP, SSADef.R_INST, 0, 0, getTypeID(StdTypes.T_PTR));
	}
	
	public void genLoadInstContext(int src)
	{
		int restore, addr;
		ins(SSADef.I_COPY, SSADef.R_INST, src, 0, getTypeID(StdTypes.T_PTR));
		restore = prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = allocReg();
		genLoadVarAddr(addr, src, null, -relocBytes);
		ins(SSADef.I_LOADval, SSADef.R_CLSS, addr, 0, getTypeID(StdTypes.T_PTR));
		deallocRestoreReg(addr, 0, restore);
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		ins(SSADef.I_CALL, clssReg, 0, 0, off, 0l, parSize);
		if (lReg != null)
			ins(SSADef.I_REGRANGE, fReg.nr, lReg.nr);
		else
			ins(SSADef.I_REGRANGE, 0, 0);
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		ins(SSADef.I_CALLind, intfReg, 0, 0, off, 0l, parSize);
		if (lReg != null)
			ins(SSADef.I_REGRANGE, fReg.nr, lReg.nr);
		else
			ins(SSADef.I_REGRANGE, 0, 0);
	}
	
	public void genCallConst(Mthd obj, int parSize)
	{
		insPatchedCall(obj, parSize);
		if (lReg != null)
			ins(SSADef.I_REGRANGE, fReg.nr, lReg.nr);
		else
			ins(SSADef.I_REGRANGE, 0, 0);
	}
	
	public void genJmp(Instruction dest)
	{
		insJump(dest, SSADef.CC_AL);
	}
	
	public void genCondJmp(Instruction dest, int cond)
	{
		switch (cond)
		{
			case Ops.C_LW:
				insJump(dest, SSADef.CC_LW);
				return;
			case Ops.C_LE:
				insJump(dest, SSADef.CC_LE);
				return;
			case Ops.C_EQ:
				insJump(dest, SSADef.CC_EQ);
				return;
			case Ops.C_GE:
				insJump(dest, SSADef.CC_GE);
				return;
			case Ops.C_GT:
				insJump(dest, SSADef.CC_GT);
				return;
			case Ops.C_NE:
				insJump(dest, SSADef.CC_NE);
				return;
			case Ops.C_BO:
				insJump(dest, SSADef.CC_BO);
				return;
		}
		fatalError("invalid jump condition in genCondJmp");
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		ins(SSADef.I_CMP, src1, src2, 0, getTypeID(type), 0l, cond);
		return cond;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		int reg2, restore;
		restore = prepareFreeReg(0, 0, 0, type);
		reg2 = allocReg();
		genLoadConstVal(reg2, val, type);
		cond = genComp(src, reg2, type, cond);
		deallocRestoreReg(reg2, 0, restore);
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		int reg2, restore, type = asDouble ? StdTypes.T_DBL : StdTypes.T_LONG;
		restore = prepareFreeReg(0, 0, 0, type);
		reg2 = allocReg();
		genLoadConstDoubleOrLongVal(reg2, val, asDouble);
		cond = genComp(src, reg2, type, cond);
		deallocRestoreReg(reg2, 0, restore);
		return cond;
	}
	
	public void genWriteIO(int dst, int src, int type, int memLoc)
	{
		ins(SSADef.I_OUT, dst, src, 0, getTypeID(type), 0l, memLoc);
	}
	
	public void genReadIO(int dst, int src, int type, int memLoc)
	{
		ins(SSADef.I_IN, dst, src, 0, getTypeID(type), 0l, memLoc);
	}
	
	public void genCheckBounds(int addr, int off, int checkToOffset, Instruction onSuccess)
	{
		ins(SSADef.I_BOUND, addr, off, 0, checkToOffset);
		insJump(onSuccess, SSADef.CC_BO);
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		ins(SSADef.I_STKCHK, maxValueReg);
		insJump(onSuccess, SSADef.CC_BO);
	}
	
	public void genNativeBoundException()
	{
		ins(SSADef.I_EXCEPT, 0, 0, 0, SSADef.E_BOUND);
	}
	
	public int genCompPtrToNull(int reg, int cond)
	{
		int nr, restore;
		restore = prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		nr = allocReg();
		genLoadConstVal(nr, 0, StdTypes.T_PTR);
		cond = genComp(reg, nr, StdTypes.T_PTR, cond);
		deallocRestoreReg(nr, 0, restore);
		return cond;
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		ins(SSADef.I_DEREF, destReg, objReg, indReg, baseOffset, 0l, entrySize);
	}
	
	public void genMoveToPrimary(int src, int type)
	{
		ins(SSADef.I_COPY, SSADef.R_PRIR, src, 0, getTypeID(type));
	}
	
	public void genMoveFromPrimary(int dst, int type)
	{
		ins(SSADef.I_COPY, dst, SSADef.R_PRIR, 0, getTypeID(type));
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		ins(SSADef.I_MOVEMAP, dst);
	}
	
	public void genSavePrimary(int type)
	{
		ins(SSADef.I_PUSH, SSADef.R_PRIR, 0, 0, getTypeID(type));
	}
	
	public void genRestPrimary(int type)
	{
		ins(SSADef.I_POP, SSADef.R_PRIR, 0, 0, getTypeID(type));
	}
	
	public void inlineVarOffset(int inlineMode, int mode, Object loc, int offset, int baseValue)
	{
		ins(SSADef.I_IVOF, 0, 0, 0, mem.getAddrAsInt(loc, offset), ((long) inlineMode & 0xFFFFFFFFl) | ((long) mode << 32), baseValue);
	}
	
	public void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset)
	{
		insPatchedThrowFrameInstruction(SSADef.I_TFBUILD, globalAddrReg, dest, throwBlockOffset);
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		insPatchedThrowFrameInstruction(SSADef.I_TFUPD1, 0, oldDest, throwBlockOffset);
		insPatchedThrowFrameInstruction(SSADef.I_TFUPD2, 0, newDest, throwBlockOffset);
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		ins(SSADef.I_TFRESET, globalAddrReg, 0, 0, throwBlockOffset);
	}
	
	//here the internal "coding" of the pseudo-instructions takes place
	private void insJump(Instruction dest, int cond)
	{
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = SSADef.I_JUMP;
		i.iPar1 = cond;
		i.jDest = dest;
	}
	
	private void insPatchedCall(Mthd mthd, int iPar2)
	{
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = SSADef.I_CALLim_p;
		i.iPar1 = 0;
		i.iPar2 = iPar2;
		i.lPar = 0l;
		i.refMthd = mthd;
	}
	
	private void insPatchedThrowFrameInstruction(int type, int reg0, Instruction dest, int tfo)
	{
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = type;
		i.reg0 = reg0;
		i.jDest = dest;
		i.iPar1 = tfo;
	}
	
	private Instruction ins(int type)
	{
		return ins(type, 0, 0, 0, 0, 0l, 0);
	}
	
	private Instruction ins(int type, int reg0)
	{
		return ins(type, reg0, 0, 0, 0, 0l, 0);
	}
	
	private Instruction ins(int type, int reg0, int reg1)
	{
		return ins(type, reg0, reg1, 0, 0, 0l, 0);
	}
	
	private Instruction ins(int type, int reg0, int reg1, int reg2, int iPar1)
	{
		return ins(type, reg0, reg1, reg2, iPar1, 0l, 0);
	}
	
	private Instruction ins(int type, int reg0, int reg1, int reg2, int iPar1, long lPar)
	{
		return ins(type, reg0, reg1, reg2, iPar1, lPar, 0);
	}
	
	private Instruction ins(int type, int reg0, int reg1, int reg2, int iPar1, long lPar, int iPar2)
	{
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = type;
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.reg2 = reg2;
		i.iPar1 = iPar1;
		i.iPar2 = iPar2;
		i.lPar = lPar;
		//i.isDest is already initialized by Architecture
		//i.refObj is already initialized by Architecture
		return i;
	}
	
	private void codeInstruction(Instruction i)
	{
		int opts = i.type & ~SSADef.ICODEMASK, pre, pos;
		
		switch (i.type)
		{
			case I_NONE: //nop-instruction
				i.size = 0;
				if (i.isDest)
				{ //just a dummy, move jump-destination
					while (i.type == I_NONE)
					{
						i = i.next;
						if (i == null)
						{
							fatalError("jump-dest behind last instruction");
							return;
						}
					}
					i.isDest = true;
				}
				return;
			case SSADef.I_JUMP: //jump-instruction
				pre = SSADef.IPJMPCOND; //already shifted parameter
				if (i.isDest)
					pre |= SSADef.IPJMPDEST;
				i.code[0] = (byte) pre;
				i.code[1] = (byte) SSADef.I_JUMP;
				i.code[2] = (byte) i.iPar1; //enter condition
				i.size = 7; //32-bit-relative will be added in fixJump
				return;
			case I_MAGC: //magic instruction
				for (pos = i.size - 1; pos >= 0; pos--)
					i.code[pos + 2] = i.code[pos];
				i.code[0] = (byte) i.size;
				i.code[1] = (byte) SSADef.I_INLINE;
				i.size += 2;
				return;
			case SSADef.I_NFREG: //special instruction to get the next free register
				i.iPar1 = regCnt;
				break;
		}
		//nothing special, code normal instruction
		if (i.type < 0)
		{
			fatalError("invalid code-type");
			return;
		}
		if (i.isDest)
			i.code[0] = (byte) ((opts >>> SSADef.IPOPTSHFT) | SSADef.IPJMPDEST);
		else
			i.code[0] = (byte) (opts >>> SSADef.IPOPTSHFT);
		i.code[1] = (byte) (i.type & SSADef.ICODEMASK);
		pos = 2;
		if ((opts & SSADef.IP_reg0) != 0)
		{
			i.replaceInt(pos, i.reg0);
			pos += 4;
		}
		if ((opts & SSADef.IP_reg1) != 0)
		{
			i.replaceInt(pos, i.reg1);
			pos += 4;
		}
		if ((opts & SSADef.IP_reg2) != 0)
		{
			i.replaceInt(pos, i.reg2);
			pos += 4;
		}
		switch (opts & SSADef.IP_iPar1)
		{
			case 0:
				break; //no iPar1
			case SSADef.IP_size: //byte parameter: size of operand
				i.code[pos++] = (byte) i.iPar1;
				break;
			case SSADef.IP_im_i: //int parameter: depending on opcode
				i.replaceInt(pos, i.iPar1);
				pos += 4;
				break;
			default:
				fatalError("invalid combination of IP_size/IP_im_i");
				return;
		}
		if ((opts & SSADef.IP_para) != 0)
		{
			i.replaceInt(pos, i.iPar2);
			pos += 4;
		}
		if ((opts & SSADef.IP_im_l) != 0)
		{
			i.replaceLong(pos, i.lPar);
			pos += 8;
		}
		i.size = pos;
	}
	
	private void fixJDest(Instruction me)
	{
		int relative = 0;
		Instruction dummy, dest;
		
		dest = me.jDest;
		if (dest == null)
		{
			fatalError("jump to uninitialized destination");
			return;
		}
		//get offset
		if (me.instrNr >= dest.instrNr)
		{ //get destination before us
			relative -= (dummy = me).size;
			while (dummy != dest)
			{
				if ((dummy = dummy.prev) == null)
				{
					fatalError("jump destination before not found");
					return;
				}
				relative -= dummy.size;
			}
		}
		else
		{ //get destination behind us
			dummy = me.next;
			while (dummy != dest)
			{
				relative += dummy.size;
				if ((dummy = dummy.next) == null)
				{
					fatalError("jump destination behind not found");
					return;
				}
			}
		}
		//insert the relative jump destination
		me.replaceInt(me.size - 4, relative);
		return;
	}
	
	protected void printCode(Instruction first)
	{
		Instruction now;
		int insCnt = 0;
		mthdContainer.owner.printNameWithOuter(ctx.out);
		ctx.out.print('.');
		mthdContainer.printNamePar(ctx.out);
		ctx.out.println(':');
		now = first;
		while (now != null)
		{
			if (now.token != null)
				ctx.printSourceHint(now.token);
			if (now.type != I_NONE)
			{
				insCnt += print(now);
				ctx.out.println();
			}
			now = now.next;
		}
		ctx.out.print("//instruction count: ");
		ctx.out.println(insCnt);
	}
	
	public int print(Instruction i)
	{
		if (i.type == I_NONE)
			return 0;
		ctx.out.print(i.instrNr);
		ctx.out.print(": ");
		switch (i.type)
		{
			case I_MAGC:
				ctx.out.print("MAGIC");
				for (int cnt = 2; cnt < i.size; cnt++)
				{
					ctx.out.print(" 0x");
					ctx.out.printHexFix(i.code[cnt], 2);
				}
				return 1;
			case SSADef.I_JUMP:
				ctx.out.print("JUMP ");
				switch (i.iPar1)
				{
					case SSADef.CC_AL:
						ctx.out.print("always j");
						break;
					case SSADef.CC_LW:
						ctx.out.print("ifless j");
						break; //jump if less
					case SSADef.CC_LE:
						ctx.out.print("ifleeq j");
						break; //jump if less or equal
					case SSADef.CC_EQ:
						ctx.out.print("ifequa j");
						break; //jump if equal
					case SSADef.CC_GE:
						ctx.out.print("ifeqgr j");
						break; //jump if greater or equal (==not less)
					case SSADef.CC_GT:
						ctx.out.print("ifgrea j");
						break; //jump if greater (==not less or equal)
					case SSADef.CC_NE:
						ctx.out.print("ifuneq j");
						break; //jump if not equal
					case SSADef.CC_BO:
						ctx.out.print("ifbdok j");
						break; //jump if bound ok
					default:
						ctx.out.print("###invcond j");
				}
				ctx.out.print(i.jDest.instrNr);
				break;
			case SSADef.I_MARKER: //0x01 |                               IP_im_i;                     //marker inside the current method
				ctx.out.print("MARKER");
				break;
			case SSADef.I_ENTER: //0x02 |                               IP_im_i | IP_para;           //enter method
				ctx.out.print("ENTER");
				break;
			case SSADef.I_ENTERINL: //0x03 |                               IP_im_i | IP_para;           //enter inline method
				ctx.out.print("ENTERINL");
				break;
			case SSADef.I_NFREG: //0x04 |                               IP_im_i;                     //hint containing the next free register
				ctx.out.print("NFREG");
				break;
			case SSADef.I_LEAVE: //0x05 |                               IP_im_i | IP_para;           //leave method
				ctx.out.print("LEAVE");
				break;
			case SSADef.I_LEAVEINL: //0x06 |                               IP_im_i | IP_para;           //leave inline method
				ctx.out.print("LEAVEINL");
				break;
			case SSADef.I_LOADim_i: //0x07 | IP_reg0 |                     IP_size | IP_para;           //load 8/16/32 bit immediate value
				ctx.out.print("LOADim_i");
				break;
			case SSADef.I_LOADim_l: //0x08 | IP_reg0 |                     IP_size |           IP_im_l; //load 64 bit immediate value
				ctx.out.print("LOADim_l");
				break;
			case SSADef.I_LOADim_p: //0x09 | IP_reg0 |                     IP_im_i;                     //load 32 bit immediate pointer
				ctx.out.print("LOADim_p");
				break;
			case SSADef.I_LOADnp: //0x0A | IP_reg0 |                     IP_size;                     //load null pointer
				ctx.out.print("LOADnp");
				break;
			case SSADef.I_LOADaddr: //0x0B | IP_reg0 | IP_reg1 |           IP_rela;                     //load pointer from memory with optional offset
				ctx.out.print("LOADaddr");
				break;
			case SSADef.I_LOADval: //0x0C | IP_reg0 | IP_reg1 |           IP_size;                     //load value from memory
				ctx.out.print("LOADval");
				break;
			case SSADef.I_CONV: //0x0D | IP_reg0 | IP_reg1 |           IP_size | IP_para;           //convert reg1/para to size and store in reg0
				ctx.out.print("CONV");
				break;
			case SSADef.I_COPY: //0x0E | IP_reg0 | IP_reg1 |           IP_size;                     //copy register
				ctx.out.print("COPY");
				break;
			case SSADef.I_PUSHim_i: //0x0F |                               IP_size | IP_para;           //push 8/16/32 bit immediate
				ctx.out.print("PUSHim_i");
				break;
			case SSADef.I_PUSHim_l: //0x10 |                                                   IP_im_l; //push 64 bit immediate
				ctx.out.print("PUSHim_l");
				break;
			case SSADef.I_PUSHnp: //0x11;                                                             //push null pointer
				ctx.out.print("PUSHnp");
				break;
			case SSADef.I_PUSH: //0x12 | IP_reg0 |                     IP_size;                     //push register
				ctx.out.print("PUSH");
				break;
			case SSADef.I_POP: //0x13 | IP_reg0 |                     IP_size;                     //pop to register
				ctx.out.print("POP");
				break;
			case SSADef.I_SAVE: //0x14 | IP_reg0 |                     IP_size | IP_para;           //save register for later use (para==0 if register is not written yet)
				ctx.out.print("SAVE");
				break;
			case SSADef.I_REST: //0x15 | IP_reg0 |                     IP_size | IP_para;           //restore register (para==0 if register to be restore was not written yet)
				ctx.out.print("REST");
				break;
			case SSADef.I_ASSIGN: //0x16 | IP_reg0 | IP_reg1 |           IP_size;                     //write reg1 to memory location pointed to by reg0
				ctx.out.print("ASSIGN");
				break;
			case SSADef.I_AND: //0x17 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "and" operation
				ctx.out.print("AND");
				break;
			case SSADef.I_XOR: //0x18 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "xor" operation
				ctx.out.print("XOR");
				break;
			case SSADef.I_OR: //0x19 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "or" operation
				ctx.out.print("OR");
				break;
			case SSADef.I_ADD: //0x1A | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "add" operation
				ctx.out.print("ADD");
				break;
			case SSADef.I_SUB: //0x1B | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "subtract" operation
				ctx.out.print("SUB");
				break;
			case SSADef.I_MUL: //0x1C | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "multiply" operation
				ctx.out.print("MUL");
				break;
			case SSADef.I_DIV: //0x1D | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "divide" operation
				ctx.out.print("DIV");
				break;
			case SSADef.I_MOD: //0x1E | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "modulo" operation
				ctx.out.print("MOD");
				break;
			case SSADef.I_SHL: //0x1F | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift left" operation
				ctx.out.print("SHL");
				break;
			case SSADef.I_SHRL: //0x20 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift right logically" operation
				ctx.out.print("SHRL");
				break;
			case SSADef.I_SHRA: //0x21 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift right arithmetically" operation
				ctx.out.print("SHRA");
				break;
			case SSADef.I_NOT: //0x22 | IP_reg0 | IP_reg1 |           IP_size;                     //unary "not" operation
				ctx.out.print("NOT");
				break;
			case SSADef.I_NEG: //0x23 | IP_reg0 | IP_reg1 |           IP_size;                     //unary "negate" operation
				ctx.out.print("NEG");
				break;
			case SSADef.I_BINV: //0x24 | IP_reg0 | IP_reg1;                                         //unary "bool invert" operation
				ctx.out.print("BINV");
				break;
			case SSADef.I_INCmem: //0x25 | IP_reg0 |                     IP_size;                     //increment memory value pointed to by reg0
				ctx.out.print("INCmem");
				break;
			case SSADef.I_DECmem: //0x26 | IP_reg0 |                     IP_size;                     //decrement memory value pointed to by reg0
				ctx.out.print("DECmem");
				break;
			case SSADef.I_CALL: //0x27 | IP_reg0 |                     IP_rela | IP_para;           //call method directly through register
				ctx.out.print("CALL");
				break;
			case SSADef.I_CALLind: //0x28 | IP_reg0 |                     IP_rela | IP_para;           //call method indirectly through register/memory
				ctx.out.print("CALLind");
				break;
			case SSADef.I_CALLim_p: //0x29 |                               IP_im_i | IP_para;           //call method at constant address
				ctx.out.print("CALLim_p ");
				i.refMthd.owner.printNameWithOuter(ctx.out);
				ctx.out.print('.');
				i.refMthd.printNamePar(ctx.out);
				return 1;
			case SSADef.I_CMP: //0x2A | IP_reg0 | IP_reg1 |           IP_size | IP_para;           //compare value of registers and set condition flags
				ctx.out.print("CMP");
				break;
			case SSADef.I_OUT: //0x2B | IP_reg0 | IP_reg1 |           IP_size | IP_para;           //write reg1 to special memory location pointed to by reg0 (may be handled differently depending on target architecture)
				ctx.out.print("OUT");
				break;
			case SSADef.I_IN: //0x2C | IP_reg0 | IP_reg1 |           IP_size | IP_para;           //read reg0 from special memory location pointerd to by reg1 (may be handled differently depending on target architecture)
				ctx.out.print("IN");
				break;
			case SSADef.I_BOUND: //0x2D | IP_reg0 | IP_reg1 |           IP_rela;                     //do bound check
				ctx.out.print("BOUND");
				break;
			case SSADef.I_DEREF: //0x2E | IP_reg0 | IP_reg1 | IP_reg2 | IP_rela | IP_para;           //calculate address of value inside array (base-pointer, offset, element number, element size)
				ctx.out.print("DEREF");
				break;
			case SSADef.I_MOVEMAP: //0x2F | IP_reg0;                                                   //move interface map from primary result register to upper part of double pointer
				ctx.out.print("MOVEMAP");
				break;
			case SSADef.I_ALLOCREG: //0x30 | IP_reg0 |                     IP_size;                     //allocate register
				ctx.out.print("ALLOCREG");
				break;
			case SSADef.I_KILLREG: //0x31 | IP_reg0;                                                   //de-allocate/kill a register
				ctx.out.print("KILLREG");
				break;
			case SSADef.I_KILLOJMP: //0x32 | IP_reg0;                                                   //automatically de-allocate/kill a register on next jump (for conditional jumps: only if jump is done)
				ctx.out.print("KILLOJMP");
				break;
			case SSADef.I_REGRANGE: //0x33 | IP_reg0 | IP_reg1;                                         //hint containing the used register range (lower/upper bound)
				ctx.out.print("REGRANGE");
				break;
			case SSADef.I_EXCEPT: //0x34 |                               IP_im_i;                     //execute a native exception of type im_i (see below)
				ctx.out.print("EXCEPT");
				break;
			case SSADef.I_IVOF: //0x35 |                               IP_im_i | IP_para;           //inline offset of variable (may be handled differently depending on target architecture)
				ctx.out.print("IVOF");
				break;
			case SSADef.I_FLOWHINT: //0x36 |                               IP_im_i | IP_para;           //flow control hint (see Architecture.F_*)
				ctx.out.print("FLOWHINT");
				break;
			case SSADef.I_TFBUILD: //0x37 | IP_reg0 |                     IP_rela | IP_para;           //build and set up a try-catch-finally-block
				ctx.out.print("TFBUILD");
				break;
			case SSADef.I_TFUPD1: //0x38 |                               IP_rela | IP_para;           //update an already existing try-catch-finally-block (part 1), always followed by I_TFUPD2
				ctx.out.print("TFUPD1");
				break;
			case SSADef.I_TFUPD2: //0x39 |                               IP_rela | IP_para;           //update an already existing try-catch-finally-block (part 2), always following I_TFUPD1
				ctx.out.print("TFUPD2");
				break;
			case SSADef.I_TFRESET: //0x3A | IP_reg0 |                     IP_rela;                     //reset a valid try-catch-finally-block to it's original state
				ctx.out.print("TFRESET");
				break;
			case SSADef.I_STKCHK: //0x3B | IP_reg0;                                                   //insert special stack extreme check instructions, followed by CC_BO conditional jump
				ctx.out.print("STKCHK");
				break;
			case SSADef.I_ASSIGNcmplx: //complex assign instruction generated by optimizer
				ctx.out.print("ASSIGNcmplx");
				break;
			case SSADef.I_LOADcmplx: //complex assign instruction generated by optimizer
				ctx.out.print("LOADcmplx");
				break;
			default:
				ctx.out.print("unknown SSA instruction");
				return 1;
		}
		int opts = i.type & ~SSADef.ICODEMASK;
		if ((opts & SSADef.IP_iPar1) == SSADef.IP_size)
		{ //byte parameter: size of operand
			ctx.out.print(' ');
			switch (i.iPar1)
			{
				case -2:
					ctx.out.print("dptr");
					break;
				case -1:
					ctx.out.print("ptr");
					break;
				case 1:
					ctx.out.print("byte");
					break;
				case 2:
					ctx.out.print("short");
					break;
				case 4:
					ctx.out.print("int");
					break;
				case 5:
					ctx.out.print("float");
					break;
				case 8:
					ctx.out.print("long");
					break;
				case 9:
					ctx.out.print("double");
					break;
				default:
					ctx.out.print("us");
					ctx.out.print(i.iPar1);
			}
		}
		if ((opts & SSADef.IP_reg0) != 0)
		{
			ctx.out.print(" r");
			ctx.out.print(i.reg0);
		}
		if ((opts & SSADef.IP_reg1) != 0)
		{
			ctx.out.print(" r");
			ctx.out.print(i.reg1);
		}
		if ((opts & SSADef.IP_reg2) != 0)
		{
			ctx.out.print(" r");
			ctx.out.print(i.reg2);
		}
		switch (opts & SSADef.IP_iPar1)
		{
			case 0:
				break; //no iPar1
			case SSADef.IP_size: //already done
				break;
			case SSADef.IP_im_i: //int parameter: depending on opcode
				ctx.out.print(' ');
				ctx.out.print(i.iPar1);
				break;
			default:
				ctx.out.print("invalid combination of IP_size/IP_im_i");
				return 0;
		}
		if ((opts & SSADef.IP_para) != 0)
		{
			ctx.out.print(' ');
			ctx.out.print(i.iPar2);
		}
		if ((opts & SSADef.IP_im_l) != 0)
		{
			ctx.out.print(' ');
			ctx.out.print(i.lPar);
		}
		return 1;
	}
}
