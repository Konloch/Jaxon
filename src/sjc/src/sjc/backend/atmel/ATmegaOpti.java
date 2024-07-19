/* Copyright (C) 2007, 2008, 2009, 2010 Stefan Frenz
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

package sjc.backend.atmel;

import sjc.backend.InstrList;
import sjc.backend.Instruction;
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;

/**
 * ATmegaOpti: optimizer for ATmega backend implementation
 *
 * @author S. Frenz
 * @version 100620 fixed bsh optimization
 * version 100502 added optimization for shift with imm. 1
 * version 100501 fixed ROL/LSR flag-reading information, fixed instruction kill after SBRC
 * version 100115 adopted changed error reporting
 * version 090702 added support for stack extreme check
 * version 090207 added copyright notice
 * version 090116 added support for SBRC instruction
 * version 090109 bugfix in SBI and CBI which is not allowed for destinations >=0x20
 * version 081123 added multiple optimization for constant pointer arithmetic
 * version 081109 bugfix in searchValueSetting
 * version 081108 added optimization for constant pop, unneccessary MOVW, push/pop-detection beyond calls, several bugfixes
 * version 081107 added flow analysis for noReadWriteUntil and searchCorrespPush
 * version 081106 added dead code elimination, andi/ori optimization including sbi/cbi reducement
 * version 081105 added support for constant parameter loading, prepared constant memory address access
 * version 081023 bugfix in searchPointerSetting
 * version 081022 several bugfixes, better I_MOVregreg optimization
 * version 081021 added more instruction optimizations and push-pop analysis
 * version 081020 added stack frame analysis and register renaming for I_MOVregreg elimination, IN/OUT-usage
 * version 081019 added flag handling
 * version 081018 added basic ValueInfo utilization
 * version 081017 fixed CPI-optimization
 * version 081016 added support for ctx.printCode
 * version 081015 redesign with flow analysis
 * version 080615 first optimizations
 * version 080613 better comments
 * version 080611 adopted changes for language throwables
 * version 080525 adopted changed jump conditioning, fixed jump-optimize-bug
 * version 070725 initial version, jump optimization
 */

public class ATmegaOpti extends ATmega
{
	protected final static int SE_READ_STACK = 0x0001;
	protected final static int SE_WRITE_STACK = 0x0002;
	protected final static int SE_READ_MEMORY = 0x0004;
	protected final static int SE_WRITE_MEMORY = 0x0008;
	protected final static int SE_READ_FLAG_ZERO = 0x0010;
	protected final static int SE_WRITE_FLAG_ZERO = 0x0020; //write has to be read<<1 (see jumps)
	protected final static int SE_READ_FLAG_CARRY = 0x0040;
	protected final static int SE_WRITE_FLAG_CARRY = 0x0080; //write has to be read<<1 (see jumps)
	protected final static int SE_READ_FLAG_SIGNED = 0x0100;
	protected final static int SE_WRITE_FLAG_SIGNED = 0x0200; //write has to be read<<1 (see jumps)
	protected final static int SE_NO_REMOVE = 0x1000;
	protected final static int SE_ONLY_UPPER_REGS = 0x2000;
	protected final static int SE_DEST_REGS = 0x4000;
	
	protected InstrList emptyInstrLists;
	protected ValueInfo emptyVInfos;
	
	private final static long CALLMASK = 0xCFFF07FF3000F800l;
	
	private long immediateBuffer;
	private int immediateBufferPos;
	
	public Instruction createNewInstruction()
	{
		return new ATmegaOptInstr(maxInstrCodeSize);
	}
	
	public void finalizeInstructions(Instruction first)
	{
		Instruction now, last;
		boolean redo;
		
		//mark all magic instructions
		last = now = first;
		while (now != null)
		{
			last = now;
			switch (now.type)
			{
				case I_MAGC:
					now.lPar = 0xCFFF07FF3000F800l; //treat magic instructions as read Y and defaults, write everything except Y and defaults
					now.iPar2 = SE_WRITE_MEMORY | SE_READ_STACK | SE_WRITE_STACK;
					break;
				case I_ADDpatched:
					now.lPar = 0x0C0300000C000000l; //reads and writes R_X, possibly writes 16/17
					now.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_SIGNED | SE_WRITE_FLAG_ZERO | SE_NO_REMOVE | SE_DEST_REGS;
					break;
			}
			now = now.next;
		}
		//optimize
		optimize(first, last);
		//code instructions
		now = first;
		while (now != null)
		{
			if (now.type > 0)
				code(now); //do not code jumps and special instructions here
			now = now.next;
		}
		//fix jumps and patched-adds
		do
		{
			now = first;
			redo = false;
			while (now != null)
			{
				if (now.type == I_JUMP)
					redo |= fixJump(now);
				else if (now.type == I_ADDpatched)
				{
					redo |= fixAdd(now);
					now = now.next; //skip I_IHELPER
				}
				now = now.next;
			}
		} while (redo);
		//print final disassembly if wanted
		if ((mthdContainer.marker & Marks.K_PRCD) != 0 || ctx.printCode)
			printCode(first, "postOpt");
	}
	
	protected boolean fixJump(Instruction jump)
	{
		int relative, nextRelative;
		Instruction next;
		
		if ((relative = getJumpRel(jump, jump.jDest)) == 0)
		{
			kill(jump);
			return true; //redo loop, jump has changed
		}
		if ((mthdContainer.marker & Marks.K_NOPT) == 0 //optimization permitted
				&& jump.iPar1 != 0 && (next = getNextRealIns(jump.next)) != null && next.type == I_JUMP && next.iPar1 == 0 && noJumpDestBetween(jump.next, next) && getNextRealIns(next.next) == getNextRealIns(jump.jDest) && (nextRelative = getJumpRel(next, next.jDest)) >= -64 && nextRelative < 63)
		{ //conditional jump over unconditional jump in small range
			jump.jDest = next.jDest;
			jump.iPar1 ^= Ops.INVCBIT;
			kill(next);
			return true; //redo loop, jump has changed
		}
		return codeJump(jump, relative);
	}
	
	protected void ins(int type, int reg0, int reg1, int imm)
	{
		Instruction i;
		
		appendInstruction(i = getUnlinkedInstruction());
		set(i, type, reg0, reg1, imm);
	}
	
	protected void ins(int type, int reg0, int reg1, int imm, Instruction dest)
	{
		Instruction i;
		
		appendInstruction(i = getUnlinkedInstruction());
		i.jDest = dest;
		set(i, type, reg0, reg1, imm);
	}
	
	protected Instruction insertInsAfter(Instruction now)
	{
		Instruction ret = getUnlinkedInstruction();
		ret.prev = now;
		ret.next = now.next;
		now.next = ret;
		ret.next.prev = ret;
		ret.instrNr = now.instrNr; //copy instruction number to ease debugging - for use with jumps re-enumerate instructions!
		return ret;
	}
	
	protected Instruction insPatchedCall(Mthd refMthd, int parSize)
	{
		Instruction i = super.insPatchedCall(refMthd, parSize);
		i.lPar = CALLMASK;
		i.iPar2 = SE_READ_STACK | SE_DEST_REGS;
		i.iPar3 = parSize;
		return i;
	}
	
	protected long readReg(int reg)
	{
		return (0x0000000000000001l << reg);
	}
	
	protected long writeReg(int reg)
	{
		return (0x0000000100000000l << reg);
	}
	
	protected boolean isRead(Instruction i, int reg)
	{
		return (i.lPar & (0x0000000000000001l << reg)) != 0l;
	}
	
	protected boolean isWrite(Instruction i, int reg)
	{
		return (i.lPar & (0x0000000100000000l << reg)) != 0l;
	}
	
	protected void set(Instruction i, int type, int reg0, int reg1, int imm)
	{
		i.type = type;
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = imm;
		switch (type)
		{
			case I_LDIregimm:
				i.lPar = writeReg(reg0);
				i.iPar2 = SE_ONLY_UPPER_REGS;
				return;
			case I_MOVregreg:
				i.lPar = writeReg(reg0) | readReg(reg1);
				i.iPar2 = 0;
				return;
			case I_MOVWregreg:
				i.lPar = writeReg(reg0 + 1) | writeReg(reg0) | readReg(reg1 + 1) | readReg(reg1);
				i.iPar2 = SE_DEST_REGS;
				return;
			case I_PUSHreg:
				i.lPar = readReg(reg0);
				i.iPar2 = SE_READ_STACK | SE_WRITE_STACK;
				return;
			case I_POPreg:
				i.lPar = writeReg(reg0);
				i.iPar2 = SE_READ_STACK | SE_WRITE_STACK;
				return;
			case I_ANDregreg:
			case I_ORregreg:
			case I_ADDregreg:
			case I_SUBregreg:
				i.lPar = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_ADCregreg:
			case I_SBCregreg:
				i.lPar = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				i.iPar2 = SE_READ_FLAG_CARRY | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_EORregreg:
				if (reg0 != reg1)
					i.lPar = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				else
					i.lPar = writeReg(reg0);
				i.iPar2 = SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_ANDIregimm:
			case I_ORIregimm:
			case I_SUBIregimm:
				i.lPar = writeReg(reg0) | readReg(reg0);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_ONLY_UPPER_REGS | SE_WRITE_FLAG_SIGNED;
				return;
			case I_COMreg:
			case I_LSLreg:
			case I_LSRreg:
			case I_NEGreg:
			case I_ASRreg:
			case I_INCreg:
			case I_DECreg:
				i.lPar = writeReg(reg0) | readReg(reg0);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_ROLreg:
			case I_RORreg:
				i.lPar = writeReg(reg0) | readReg(reg0);
				i.iPar2 = SE_READ_FLAG_CARRY | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_SIGNED;
				return;
			case I_SBCIregimm:
				i.lPar = writeReg(reg0) | readReg(reg0);
				i.iPar2 = SE_READ_FLAG_CARRY | SE_WRITE_FLAG_CARRY | SE_ONLY_UPPER_REGS | SE_WRITE_FLAG_SIGNED;
				return;
			case I_LDZreg:
				i.lPar = writeReg(reg0) | readReg(R_ZH) | readReg(R_ZL);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_STZ_INCreg:
			case I_STZ_DECreg:
				i.lPar = readReg(reg0) | writeReg(R_ZH) | writeReg(R_ZL) | readReg(R_ZH) | readReg(R_ZL);
				i.iPar2 = SE_WRITE_MEMORY;
				return;
			case I_LDZ_INCreg:
			case I_LDZ_DECreg:
			case I_LPM_INCreg:
				i.lPar = writeReg(reg0) | writeReg(R_ZH) | writeReg(R_ZL) | readReg(R_ZH) | readReg(R_ZL);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_INregimm:
			case I_LDSregimm:
				i.lPar = writeReg(reg0);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_OUTimmreg:
			case I_STSimmreg:
				i.lPar = readReg(reg0);
				i.iPar2 = SE_WRITE_MEMORY;
				return;
			case I_CPIregimm:
				i.lPar = readReg(reg0);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_ONLY_UPPER_REGS | SE_WRITE_FLAG_SIGNED;
				return;
			case I_CPregreg:
				i.lPar = readReg(reg0) | readReg(reg1);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_CPCregreg:
				i.lPar = readReg(reg0) | readReg(reg1);
				i.iPar2 = SE_READ_FLAG_CARRY | SE_READ_FLAG_ZERO | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_CALL:
				i.lPar = CALLMASK;
				i.iPar2 = SE_READ_STACK | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_ICALL:
				i.lPar = CALLMASK | readReg(R_ZL) | readReg(R_ZH);
				i.iPar2 = SE_READ_STACK | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED;
				return;
			case I_ADDIWregimm:
				switch (reg0)
				{
					case 24:
					case 26:
					case 28:
					case 30:
						i.lPar = writeReg(reg0 + 1) | writeReg(reg0) | readReg(reg0 + 1) | readReg(reg0);
						i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED | SE_DEST_REGS;
						return;
				}
				fatalError("invalid operand for ADDIWregimm");
				return;
			case I_MULregreg:
				i.lPar = writeReg(1) | writeReg(0) | readReg(reg0) | readReg(reg1);
				i.iPar2 = SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED | SE_DEST_REGS;
				return;
			case I_STX_DECreg:
				i.lPar = readReg(reg0) | writeReg(R_XH) | writeReg(R_XL) | readReg(R_XH) | readReg(R_XL);
				i.iPar2 = SE_WRITE_MEMORY;
				return;
			case I_LDX_INCreg:
				i.lPar = writeReg(reg0) | writeReg(R_XH) | writeReg(R_XL) | readReg(R_XH) | readReg(R_XL);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_RET:
				switch (curMthd.retRegType)
				{
					case 0:
						i.lPar = 0l;
						break;
					case StdTypes.T_LONG:
					case StdTypes.T_DBL:
						i.lPar = readReg(R_PRIM_START + 7) | readReg(R_PRIM_START + 6) | readReg(R_PRIM_START + 5) | readReg(R_PRIM_START + 4);
					case StdTypes.T_INT:
					case StdTypes.T_FLT:
					case StdTypes.T_DPTR:
						i.lPar |= readReg(R_PRIM_START + 3) | readReg(R_PRIM_START + 2);
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_PTR:
						i.lPar |= readReg(R_PRIM_START + 1);
					case StdTypes.T_BOOL:
					case StdTypes.T_BYTE:
						i.lPar |= readReg(R_PRIM_START);
						break;
					default:
						fatalError("invalid return-type");
						break;
				}
				i.iPar2 = SE_READ_STACK | SE_WRITE_STACK | SE_NO_REMOVE | SE_DEST_REGS;
				return;
			case I_RETI:
				i.lPar = 0l;
				i.iPar2 = SE_READ_STACK | SE_WRITE_STACK | SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_ZERO | SE_WRITE_FLAG_SIGNED | SE_NO_REMOVE | SE_DEST_REGS;
				return;
			case I_CLC:
				i.lPar = 0l;
				i.iPar2 = SE_WRITE_FLAG_CARRY;
				return;
			case I_SEC:
				i.lPar = 0l;
				i.iPar2 = SE_WRITE_FLAG_CARRY;
				return;
			case I_LPMreg:
				i.lPar = writeReg(reg0) | readReg(R_ZH) | readReg(R_ZL);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_LDY_DISPregimm:
				i.lPar = writeReg(reg0) | readReg(R_YH) | readReg(R_YL);
				i.iPar2 = SE_READ_MEMORY;
				return;
			case I_STY_DISPregimm:
				i.lPar = readReg(reg0) | readReg(R_YH) | readReg(R_YL);
				i.iPar2 = SE_WRITE_MEMORY;
				return;
			case I_CBIimm:
			case I_SBIimm:
				i.lPar = 0l;
				i.iPar2 = SE_WRITE_MEMORY;
				return;
			case I_SBRCregimm:
				if (i.jDest == null)
				{
					fatalError("SBRC needs jump destination");
					return;
				}
				i.lPar = readReg(reg0);
				i.iPar2 = SE_NO_REMOVE;
				return;
			case I_PUSHip:
				i.lPar = 0l;
				i.iPar2 = SE_WRITE_STACK;
				return;
			case I_CLI:
				i.lPar = 0l;
				i.iPar2 = SE_NO_REMOVE;
				return;
			case I_NONE:
			case I_BSHOPHINT:
				i.lPar = 0l;
				i.iPar2 = 0;
				return;
			case I_STEXreg:
				i.lPar = readReg(reg0) | writeReg(reg0);
				i.iPar2 = SE_DEST_REGS;
				return;
		}
		fatalError("invalid instruction type in set");
	}
	
	protected void optimize(Instruction first, Instruction last)
	{
		Instruction now, tmp, tmp2;
		ATmegaOptInstr iTmp;
		InstrList iList, iListTmp;
		ValueInfo vInfo, vInfo2;
		boolean redo, hasRead;
		int i, j, k, reg0, reg1, loopCnt = 0;
		
		if ((mthdContainer.marker & Marks.K_NOPT) != 0)
			return; //optimizations prohibited
		
		do
		{
			//print disassembly if wanted
			if ((mthdContainer.marker & Marks.K_PRCD) != 0 || ctx.printCode)
			{
				ctx.out.print("//preOpt before optimizing loop ");
				ctx.out.println(++loopCnt);
				printCode(first, "preOpt");
			}
			
			redo = false;
			//try to remove jumps and reset recursion marks
			now = first;
			while (now != null)
			{
				now.isDest = false;
				if (now.type == I_JUMP && getNextRealIns(now.next) == getNextRealIns(now.jDest))
				{
					kill(now);
					redo = true;
				}
				now = now.next;
			}
			//mark reachable code by setting isDest of all reachable instructions
			markReachableCode(first);
			//remove unmarked instructions, clear jump destination and reset recursion marks
			now = first;
			while (now != null)
			{
				if ((iList = (iTmp = ((ATmegaOptInstr) now)).jSource) != null)
				{
					//add all containers to our emptylist
					iListTmp = iList;
					while (iListTmp.next != null)
						iListTmp = iListTmp.next; //search end of current instrlist
					iListTmp.next = emptyInstrLists;
					emptyInstrLists = iList;
					iTmp.jSource = null;
				}
				if (now.type != I_NONE && !now.isDest)
				{
					//unreachable instruction
					now.refMthd = null; //ensure there is no reference
					kill(now);
					redo = true;
				}
				now.isDest = false; //reusage as recursion mark, ATmegaOpti uses real jump source as marks for jump destinations
				now = now.next;
			}
			//evaluate jump conditions and mark jump destinations
			now = first;
			while (now != null)
			{
				switch (now.type)
				{
					case I_JUMP:
						if (now.iPar1 == 0)
							now.iPar2 = 0; //unconditional jump
						else
						{ //conditional jump
							switch (now.iPar1)
							{
								case Ops.C_GE:
								case Ops.C_LW:
									now.iPar2 = SE_READ_FLAG_SIGNED;
									break;//jump if greater or equal / lower (signed)
								case Ops.C_BO:
									now.iPar2 = SE_READ_FLAG_CARRY;
									break; //jump if below (unsigned)
								case Ops.C_NE:
								case Ops.C_EQ:
									now.iPar2 = SE_READ_FLAG_ZERO;
									break; //jump if not equal / equal
								default:
									fatalError("invalid conditional jump type");
									return;
							}
						}
						handleJumpDest(now);
						break;
					case I_SBRCregimm:
						handleJumpDest(now);
						break;
				}
				now = now.next;
			}
			//try to remove instructions without effect
			now = first;
			while (now != null)
			{
				if (now.type > 0 && (now.iPar2 & (SE_WRITE_STACK | SE_WRITE_MEMORY | SE_NO_REMOVE)) == 0 && ((now.iPar2 & (SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_SIGNED | SE_WRITE_FLAG_ZERO)) == 0 || flagRelevance(now.next, now.iPar2) == 0))
				{ //normal instruction without side-effect
					hasRead = false;
					for (i = 0; i < 32; i++)
					{
						if (isWrite(now, i) && searchReadStartingAt(now.next, i) != null)
							hasRead = true;
					}
					if (!hasRead)
					{ //output of this instruction is never used
						kill(now);
						redo = true;
					}
				}
				now = now.next;
			}
			//try to optimize
			now = last; //backwards is faster usually
			while (now != null)
			{
				vInfo = null;
				vInfo2 = null;
				INSTR_SW:
				switch (now.type)
				{
					case I_JUMP:
						if (now.iPar1 != 0)
							switch (searchFlagSetting(now, now.iPar2 << 1))
							{ //write is read<<1
								case 0: //flag is cleared
									if ((now.iPar1 & Ops.INVCBIT) != 0)
										now.iPar1 = 0; //always jump
									else
									{
										kill(now); //never jump
										redo = true;
									}
									break;
								case 1: //flag is set
									if ((now.iPar1 & Ops.INVCBIT) == 0)
										now.iPar1 = 0; //always jump
									else
									{
										kill(now); //never jump
										redo = true;
									}
									break;
								//default: flag is unknown
							}
						break;
					case I_MOVregreg:
						if (now.reg0 == now.reg1 || tryReplaceRegisterReading(now.next, last, now.reg0, now.reg1))
						{
							kill(now);
							redo = true;
						}
						else if ((tmp = searchWriteBefore(now, now.reg1, false)) != null && tmp.type > 0 && !isRead(tmp, now.reg1) && (tmp.iPar2 & SE_DEST_REGS) == 0 && (now.reg0 >= 16 || (tmp.iPar2 & SE_ONLY_UPPER_REGS) == 0) && noReadWriteUntil(tmp.next, now, now.reg1, true, true) && noReadWriteUntil(tmp.next, now, now.reg0, true, true) && searchReadStartingAt(now.next, now.reg1) == null)
						{
							if (tmp.reg0 == now.reg1)
								tmp.reg0 = now.reg0;
							if (tmp.reg1 == now.reg1)
								tmp.reg1 = now.reg0;
							set(tmp, tmp.type, tmp.reg0, tmp.reg1, tmp.iPar1); //call "set" on tmp to build additional info
							kill(now);
							redo = true;
						}
						break;
					case I_MOVWregreg:
						if (now.reg0 == now.reg1)
						{
							kill(now);
							redo = true;
							break;
						}
						else if ((vInfo = searchPointerSetting(now, now.reg1)) != null && vInfo.type == ValueInfo.VI_MEMABS && now.reg0 >= 16)
						{
							set(now, I_LDIregimm, now.reg0, 0, vInfo.imm & 0xFF);
							tmp = insertInsAfter(now);
							set(tmp, I_LDIregimm, now.reg0 + 1, 0, vInfo.imm >>> 8);
							redo = true;
						}
						else if ((tmp = searchWriteBefore(now, now.reg1, false)) != null && tmp.type > 0 && !isRead(tmp, now.reg1) && (tmp.iPar2 & SE_DEST_REGS) == 0 && (now.reg0 >= 16 || (tmp.iPar2 & SE_ONLY_UPPER_REGS) == 0) && noReadWriteUntil(tmp.next, now.prev, now.reg1, true, true) && noReadWriteUntil(tmp.next, now.prev, now.reg0, true, true) && searchReadStartingAt(now.next, now.reg1) == null && (tmp2 = searchWriteBefore(now, now.reg1 + 1, false)) != null && tmp2.type > 0 && !isRead(tmp2, now.reg1 + 1) && (tmp2.iPar2 & SE_DEST_REGS) == 0 && (now.reg0 + 1 >= 16 || (tmp2.iPar2 & SE_ONLY_UPPER_REGS) == 0) && noReadWriteUntil(tmp2.next, now.prev, now.reg1 + 1, true, true) && noReadWriteUntil(tmp2.next, now.prev, now.reg0 + 1, true, true) && searchReadStartingAt(now.next, now.reg1 + 1) == null)
						{
							if (tmp.reg0 == now.reg1)
								tmp.reg0 = now.reg0;
							if (tmp.reg1 == now.reg1)
								tmp.reg1 = now.reg0;
							if (tmp2.reg0 == now.reg1 + 1)
								tmp2.reg0 = now.reg0 + 1;
							if (tmp2.reg1 == now.reg1 + 1)
								tmp2.reg1 = now.reg0 + 1;
							set(tmp, tmp.type, tmp.reg0, tmp.reg1, tmp.iPar1); //call "set" on tmp to build additional info
							set(tmp2, tmp2.type, tmp2.reg0, tmp2.reg1, tmp2.iPar1); //call "set" on tmp2 to build additional info
							kill(now);
							redo = true;
						}
						else if ((tmp = searchWriteBefore(now, now.reg1, false)) != null && tmp.type == I_ADDregreg && tmp.next.type == I_ADCregreg && tmp.reg1 == now.reg0 && tmp.next.reg1 == now.reg0 + 1 && searchReadStartingAt(now.next, now.reg1) == null && searchReadStartingAt(now.next, now.reg1 + 1) == null && noReadWriteUntil(tmp.next, now.prev, now.reg0, true, true) && noReadWriteUntil(tmp.next.next, now.prev, now.reg0 + 1, true, true))
						{
							//there is something like
							//  add rA,rB
							//  adc rA+1,rB+1
							//  movw rB,rA
							set(tmp, tmp.type, tmp.reg1, tmp.reg0, 0);
							set(tmp.next, tmp.next.type, tmp.next.reg1, tmp.next.reg0, 0);
							kill(now);
							redo = true;
						}
						break;
					case I_CLC:
						if ((tmp = getNextRealIns(now.next)) != null && tmp.type == I_CPCregreg && noJumpDestBetween(now, tmp))
						{
							kill(now);
							set(tmp, I_CPregreg, tmp.reg0, tmp.reg1, 0);
							redo = true;
						}
						break;
					case I_CPregreg:
						if ((vInfo = searchValueSetting(now, now.reg1)) != null)
						{
							if (now.reg1 != R_ZERO && vInfo.type == ValueInfo.VI_ABS)
							{
								if (vInfo.imm == 0)
								{ //replace 0-compare with special register r11
									set(now, I_CPregreg, now.reg0, R_ZERO, 0);
									redo = true;
								}
								else if (now.reg0 >= 16)
								{ //replace ldi,cp with cpi
									set(now, I_CPIregimm, now.reg0, 0, vInfo.imm);
									redo = true;
								}
							}
						}
						break;
					case I_EORregreg:
						if (now.reg0 == now.reg1 && flagRelevance(now.next, now.iPar2) == 0)
						{
							if ((vInfo = searchValueSetting(now, now.reg0)) != null)
							{
								if (vInfo.type == ValueInfo.VI_ABS && vInfo.imm == 0)
								{ //avoid clearing again
									kill(now);
									redo = true;
								}
							}
						}
						break;
					case I_LDIregimm:
						if (now.iPar1 == 0)
						{
							set(now, I_EORregreg, now.reg0, now.reg0, 0); //replace setting to zero with eor
							redo = true;
						}
						else if ((vInfo = searchValueSetting(now, now.reg0)) != null)
						{
							if (vInfo.type == ValueInfo.VI_ABS)
							{
								if (vInfo.imm == now.iPar1)
								{ //avoid setting the same value
									kill(now);
									redo = true;
								}
							}
						}
						break;
					case I_SUBregreg:
					case I_ADDregreg:
						if ((vInfo = searchValueSetting(now, now.reg1)) != null && vInfo.type == ValueInfo.VI_ABS)
						{
							switch (vInfo.imm)
							{
								case 0:
									switch (flagRelevance(now.next, now.iPar2))
									{
										case 0:
											kill(now);
											redo = true;
											break INSTR_SW;
										case SE_WRITE_FLAG_CARRY:
											set(now, I_CLC, 0, 0, 0);
											redo = true;
											break INSTR_SW;
									}
									break;
								case 1:
									if (flagRelevance(now.next, now.iPar2) == 0)
									{
										set(now, now.type == I_SUBregreg ? I_DECreg : I_INCreg, now.reg0, 0, 0);
										redo = true;
										break INSTR_SW;
									}
									break;
							}
							if (now.reg0 >= 16)
							{
								if (now.type == I_SUBregreg || flagRelevance(now.next, now.iPar2) == 0)
								{
									set(now, I_SUBIregimm, now.reg0, 0, now.type == I_SUBregreg ? vInfo.imm : (-vInfo.imm) & 0xFF);
									redo = true;
									break INSTR_SW;
								}
								//now.type==I_ADDregreg && flagRelevance!=0, try to replace multiple add+adc+adc... with constants
								immediateBuffer = vInfo.imm;
								immediateBufferPos = 0;
								if (tryReplaceRegImmADC(now.next))
								{
									set(now, I_SUBIregimm, now.reg0, 0, ((int) immediateBuffer) & 0xFF);
									redo = true;
									break INSTR_SW;
								}
							}
						}
						break;
					case I_SBCregreg:
						if (now.reg0 >= 16 && (vInfo = searchValueSetting(now, now.reg1)) != null && vInfo.type == ValueInfo.VI_ABS)
						{
							set(now, I_SBCIregimm, now.reg0, 0, vInfo.imm);
							redo = true;
						}
						break;
					case I_ADDIWregimm:
						if ((tmp = getNextRealIns(now.next)) != null && (tmp2 = getNextRealIns(tmp.next)) != null && tmp.type == I_SUBIregimm && tmp2.type == I_SBCIregimm && tmp.reg0 == now.reg0 && tmp2.reg0 == now.reg0 + 1 && flagRelevance(tmp2.next, SE_WRITE_FLAG_CARRY) == 0)
						{
							//found something like
							//  addiw rA,c
							//  subi rA,d
							//  sbci rA+1,e
							i = (tmp.iPar1 | (tmp2.iPar1 << 8)) - now.iPar1;
							set(tmp, I_SUBIregimm, tmp.reg0, 0, i & 0xFF);
							set(tmp2, I_SBCIregimm, tmp2.reg0, 0, (i >>> 8) & 0xFF);
							kill(now);
							redo = true;
						}
						break;
					case I_ANDregreg:
						if ((vInfo = searchValueSetting(now, now.reg1)) != null)
						{
							if (vInfo.type == ValueInfo.VI_ABS && flagRelevance(now.next, now.iPar2) == 0)
							{
								switch (vInfo.imm)
								{
									case 0x00:
										set(now, I_EORregreg, now.reg0, now.reg0, 0);
										redo = true;
										break;
									case 0xFF:
										kill(now);
										redo = true;
										break;
									default:
										if (now.reg0 >= 16)
										{
											set(now, I_ANDIregimm, now.reg0, 0, vInfo.imm);
											redo = true;
										}
								}
							}
						}
						break;
					case I_ORregreg:
						if ((vInfo = searchValueSetting(now, now.reg1)) != null)
						{
							if (vInfo.type == ValueInfo.VI_ABS && flagRelevance(now.next, now.iPar2) == 0)
							{
								switch (vInfo.imm)
								{
									case 0x00:
										kill(now);
										redo = true;
										break INSTR_SW;
									case 0xFF:
										if (now.reg0 >= 16)
										{
											set(now, I_LDIregimm, now.reg0, 0, 0xFF);
											redo = true;
										}
										break INSTR_SW;
									default:
										if (now.reg0 >= 16)
										{
											set(now, I_ORIregimm, now.reg0, 0, vInfo.imm);
											redo = true;
											break INSTR_SW;
										}
								}
							}
						}
						if ((vInfo2 = searchValueSetting(now, now.reg0)) != null)
						{
							if (vInfo2.type == ValueInfo.VI_ABS && flagRelevance(now.next, now.iPar2) == 0)
							{
								switch (vInfo2.imm)
								{
									case 0x00:
										set(now, I_MOVregreg, now.reg0, now.reg1, 0);
										redo = true;
										break INSTR_SW;
									case 0xFF:
										if (now.reg0 >= 16)
										{
											set(now, I_LDIregimm, now.reg1, 0, 0xFF);
											redo = true;
										}
										break INSTR_SW;
								}
							}
						}
						break;
					case I_STZ_DECreg:
					case I_STZ_INCreg:
					case I_LDZreg:
					case I_LDZ_DECreg:
					case I_LDZ_INCreg:
						if (searchReadStartingAt(now.next, R_ZL) != null)
							break; //changed Z is needed later
						if ((vInfo = searchPointerSetting(now, R_ZL)) != null)
						{
							if (vInfo.type == ValueInfo.VI_MEMREL && vInfo.memreg == R_YL && vInfo.imm >= 0 && vInfo.imm < 64)
							{
								//replace current Z-based addressing with Y-based addressing
								set(now, now.type == I_STZ_DECreg || now.type == I_STZ_INCreg ? I_STY_DISPregimm : I_LDY_DISPregimm, now.reg0, 0, now.type == I_STZ_DECreg || now.type == I_LDZ_DECreg ? vInfo.imm - 1 : vInfo.imm);
								redo = true;
							}
							else if (vInfo.type == ValueInfo.VI_MEMABS)
							{
								if (vInfo.imm >= 0x20 && vInfo.imm < 0x60)
								{
									//replace current Z-based addressing with direct IN/OUT
									set(now, now.type == I_STZ_DECreg || now.type == I_STZ_INCreg ? I_OUTimmreg : I_INregimm, now.reg0, 0, (now.type == I_STZ_DECreg || now.type == I_LDZ_DECreg ? vInfo.imm - 1 : vInfo.imm) - 0x20);
									redo = true;
								}
								//else if (vInfo1.imm<0x60) {
								//replace current Z-based addressing with direct LDS/STS
								//TODO check if direct addressing is cheaper, then replace
								//}
							}
						}
						break;
					case I_LDY_DISPregimm:
						if ((vInfo = searchLocalSetting(now, now.iPar1)) != null)
						{
							if (vInfo.type == ValueInfo.VI_ABS)
							{
								if (vInfo.imm == 0)
								{
									set(now, I_EORregreg, now.reg0, now.reg0, 0);
									redo = true;
									break;
								}
								if (now.reg0 >= 16)
								{
									set(now, I_LDIregimm, now.reg0, 0, vInfo.imm);
									redo = true;
									break;
								}
							}
						}
						if ((tmp = searchStackFrameWriteBefore(now, now.iPar1)) != null && isRead(tmp, tmp.reg0) && noReadWriteUntil(tmp.next, now.prev, tmp.reg0, false, true))
						{
							set(now, I_MOVregreg, now.reg0, tmp.reg0, 0);
							redo = true;
						}
						break;
					case I_STY_DISPregimm:
						if (searchStackFrameReadAfter(now, now.iPar1) == null)
						{
							kill(now);
							redo = true;
						}
						else if (now.reg0 != R_ZERO && (vInfo = searchValueSetting(now, now.reg0)) != null && vInfo.type == ValueInfo.VI_ABS && vInfo.imm == 0)
						{
							set(now, I_STY_DISPregimm, R_ZERO, 0, now.iPar1);
							redo = true;
						}
						break;
					case I_POPreg:
						if ((tmp = searchCorrespPush(now, 0)) != null)
						{
							if (tmp.reg0 == now.reg0 && noReadWriteUntil(tmp.next, now.prev, now.reg0, false, true))
							{
								kill(tmp);
								kill(now);
								redo = true;
								break;
							}
							if ((vInfo = searchValueSetting(tmp, tmp.reg0)) != null && vInfo.type == ValueInfo.VI_ABS && noReadWriteUntil(tmp.next, now.prev, now.reg0, true, false))
							{
								if (vInfo.imm == 0)
								{
									kill(tmp);
									set(now, I_EORregreg, now.reg0, now.reg0, 0);
									redo = true;
									break;
								}
								if (now.reg0 >= 16)
								{
									kill(tmp);
									set(now, I_LDIregimm, now.reg0, 0, vInfo.imm);
									redo = true;
									break;
								}
							}
							if (noReadWriteUntil(tmp.next, now.prev, now.reg0, false, true))
							{
								set(tmp, I_MOVregreg, now.reg0, tmp.reg0, 0);
								kill(now);
								redo = true;
								break;
							}
							if (noReadWriteUntil(tmp.next, now.prev, tmp.reg0, false, true))
							{
								set(now, I_MOVregreg, now.reg0, tmp.reg0, 0);
								kill(tmp);
								redo = true;
								break;
							}
						}
						break;
					case I_BSHOPHINT:
						switch (now.iPar1)
						{
							case Ops.B_SHL:
								break; //ok
							case Ops.B_SHRL:
								break; //ok
							case Ops.B_SHRA:
								break INSTR_SW; //TODO arithmetic shift right (not supported yet)
							default:
								ctx.out.println("invalid BSHOPHINT");
								return;
						}
						if ((vInfo = searchValueSetting(now, now.reg1)) != null)
						{
							if (vInfo.type == ValueInfo.VI_ABS && (vInfo.imm & 7) == 0 && (i = vInfo.imm >>> 3) >= 0 && i < 8)
							{ //get register shift
								//do complete recode as this shift can be coded as mov
								tmp = now.next;
								j = 0;
								while (tmp != now.jDest)
								{
									kill(tmp);
									tmp = tmp.next;
									j++;
								}
								if (i != 0)
								{ //not a no-shifter
									if (j < i)
									{ //not enough space?!?
										ctx.out.println("not enough space for re-coding BSHOPHINT");
										ctx.err = true;
										return;
									}
									j = getByteCount(now.iPar3);
									tmp = now.next;
									if (now.iPar1 == Ops.B_SHL)
									{
										while (j > i)
										{
											if ((reg0 = getReg(j, now.reg0, now.iPar3, false)) == -1 || (reg1 = getReg(j - i, now.reg0, now.iPar3, false)) == -1)
												return;
											set(tmp, I_MOVregreg, reg0, reg1, 0);
											tmp = tmp.next;
											j--;
										}
										while (j >= 1)
										{
											if ((reg0 = getReg(j, now.reg0, now.iPar3, false)) == -1)
												return;
											set(tmp, I_EORregreg, reg0, reg0, 0);
											tmp = tmp.next;
											j--;
										}
									}
									else
									{
										k = 1;
										while (k <= (j - i))
										{
											if ((reg0 = getReg(k, now.reg0, now.iPar3, false)) == -1 || (reg1 = getReg(k + i, now.reg0, now.iPar3, false)) == -1)
												return;
											set(tmp, I_MOVregreg, reg0, reg1, 0);
											tmp = tmp.next;
											k++;
										}
										while (k <= j)
										{
											if ((reg0 = getReg(k, now.reg0, now.iPar3, false)) == -1)
												return;
											set(tmp, I_EORregreg, reg0, reg0, 0);
											tmp = tmp.next;
											k++;
										}
									}
								}
								kill(now);
								redo = true;
							}
							else if (vInfo.type == ValueInfo.VI_ABS && vInfo.imm == 1)
							{ //get bit shift
								//remove loop instructions at end of binary shift as it is executed only once
								tmp = now.jDest;
								//scan back until we find the dec instruction, on the way only I_JUMP and I_NONE is allowed
								while (tmp.type != I_DECreg)
								{
									if ((tmp.type != I_NONE && tmp.type != I_JUMP) || tmp.prev == null)
										break INSTR_SW;
									tmp = tmp.prev;
								}
								tmp = tmp.next; //do not delete DEC as the target register may be used later on
								while (tmp != now.jDest)
								{ //delete all jumps of the binary shift after the dec
									kill(tmp);
									tmp = tmp.next;
								}
								kill(now);
								redo = true;
							}
						}
						break;
					case I_ANDIregimm:
					case I_ORIregimm:
						if ((tmp = searchWriteBefore(now, now.reg0, false)) != null && tmp.type == I_INregimm && (tmp2 = getNextRealIns(now.next)) != null && noJumpDestBetween(now, tmp2) && tmp2.type == I_OUTimmreg && tmp.iPar1 == tmp2.iPar1 && tmp.iPar1 < 0x20 && searchReadStartingAt(tmp2.next, now.reg0) == null)
						{
							//found in-and/or-out combination, try to replace with cbi/sbi
							k = now.type == I_ANDIregimm ? (~now.iPar1) & 0xFF : now.iPar1; //invert bit search for cbi
							j = -1;
							for (i = 0; i < 8; i++)
								if ((k & (1 << i)) != 0)
								{
									if (j == -1)
										j = i; //first bit set
									else
									{ //second bit set
										j = -1;
										break; //break for loop after invalidating j
									}
								}
							if (j != -1)
							{ //reducable, j contains number of bit to set/clear
								set(now, now.type == I_ANDIregimm ? I_CBIimm : I_SBIimm, 0, 0, (tmp.iPar1 << 3) | j);
								kill(tmp);
								kill(tmp2);
								redo = true;
								break INSTR_SW;
							}
						}
						else
							switch (now.iPar1)
							{
								case 0:
									if (now.type == I_ANDIregimm)
									{
										set(now, I_EORregreg, now.reg0, now.reg0, 0);
										redo = true;
										break INSTR_SW;
									}
									if (now.type == I_ORIregimm)
									{
										kill(now);
										redo = true;
										break INSTR_SW;
									}
									break;
								case 0xFF:
									if (now.type == I_ANDIregimm && flagRelevance(now.next, now.iPar2) == 0)
									{
										kill(now);
										redo = true;
										break INSTR_SW;
									}
									if (now.type == I_ORIregimm && flagRelevance(now.next, now.iPar2) == 0 && now.reg0 >= 16)
									{
										set(now, I_LDIregimm, now.reg0, 0, 0xFF);
										redo = true;
										break INSTR_SW;
									}
									break;
							}
						break;
					case I_SBRCregimm:
						if ((vInfo = searchValueSetting(now, now.reg0)) != null && vInfo.type == ValueInfo.VI_ABS)
						{
							if ((vInfo.imm & (1 << now.iPar1)) == 0)
							{ //always skip
								now.type = I_JUMP;
								now.iPar2 = now.iPar1 = 0;
								redo = true;
							}
							else
							{ //never skip
								kill(now);
								redo = true;
							}
						}
						break;
				}
				now = now.prev; //this loop works backwards
				if (vInfo != null)
					ungetValueInfo(vInfo);
				if (vInfo2 != null)
					ungetValueInfo(vInfo2);
			}
		} while (redo);
	}
	
	public void kill(Instruction i)
	{
		Instruction before = i.prev;
		while (before != null && before.prev != null && before.type == I_NONE)
			before = before.prev;
		if (before != null && before.type == I_SBRCregimm)
		{
			//we would kill the instruction after SBRC, so also kill SBRC
			super.kill(before);
		}
		super.kill(i);
	}
	
	private void markReachableCode(Instruction now)
	{
		while (now != null)
		{
			if (now.isDest)
				return; //already marked
			now.isDest = true;
			switch (now.type)
			{
				case I_JUMP:
					markReachableCode(now.jDest);
					if (now.iPar1 == 0)
						return; //do not continue after unconditional jump
					break;
				case I_ADDpatched:
					markReachableCode(now.next.jDest);
					break;
			}
			now = now.next;
		}
	}
	
	private void handleJumpDest(Instruction now)
	{
		ATmegaOptInstr iDest;
		InstrList iList;
		iDest = (ATmegaOptInstr) now.jDest;
		iList = iDest.jSource;
		while (iList != null)
		{
			if (iList.instr == now)
				break; //avoid inserting the same instruction again
			iList = iList.next;
		}
		if (iList == null)
		{ //insert instruction
			if (emptyInstrLists == null)
				iList = new InstrList();
			else
			{
				iList = emptyInstrLists;
				emptyInstrLists = emptyInstrLists.next;
			}
			iList.instr = now;
			iList.next = iDest.jSource;
			iDest.jSource = iList;
		}
	}
	
	private ValueInfo getEmptyValueInfo()
	{
		ValueInfo ret;
		
		if (emptyVInfos == null)
			return new ValueInfo();
		(ret = emptyVInfos).type = 0;
		ret.memreg = ret.imm = 0;
		emptyVInfos = emptyVInfos.next;
		return ret;
	}
	
	private void ungetValueInfo(ValueInfo vi)
	{
		vi.next = emptyVInfos;
		emptyVInfos = vi;
	}
	
	private int flagRelevance(Instruction i, int flagMask)
	{ //returns 0 for none, 1 for carry, 2 for carry/zero
		int relevantFlags = 0;
		
		flagMask &= SE_WRITE_FLAG_CARRY | SE_WRITE_FLAG_SIGNED | SE_WRITE_FLAG_ZERO;
		while (i != null)
		{
			if (i.isDest)
				break; //avoid endless loop, caller will reset isDest
			if ((flagMask & SE_WRITE_FLAG_ZERO) != 0 && (i.iPar2 & SE_READ_FLAG_ZERO) != 0)
				relevantFlags |= SE_WRITE_FLAG_ZERO;
			if ((flagMask & SE_WRITE_FLAG_CARRY) != 0 && (i.iPar2 & SE_READ_FLAG_CARRY) != 0)
				relevantFlags |= SE_WRITE_FLAG_CARRY;
			if ((flagMask & SE_WRITE_FLAG_SIGNED) != 0 && (i.iPar2 & SE_READ_FLAG_SIGNED) != 0)
				relevantFlags |= SE_WRITE_FLAG_SIGNED;
			if ((flagMask &= ~i.iPar2) == 0)
				break; //remove written flags, done if all flags written afterwards
			if (i.type == I_JUMP)
			{ //follow jump branch
				i.isDest = true;
				relevantFlags |= flagRelevance(i.jDest, flagMask);
				i.isDest = false;
				if (i.iPar1 == 0)
					break; //unconditional jump already checked
			}
			i = i.next;
		}
		return relevantFlags; //no following instruction reads flags
	}
	
	private boolean noReadWriteUntil(Instruction from, Instruction to, int reg, boolean noRead, boolean noWrite)
	{
		if (from.instrNr > to.instrNr)
			return true;
		while (true)
		{
			if (from == null || from.isDest)
				return true; //end of method or already looked here and nothing found
			if ((noRead && isRead(from, reg)) || (noWrite && isWrite(from, reg)))
				return false;
			if (from.type == I_JUMP)
			{
				from.isDest = true;
				if (!noReadWriteUntil(from.jDest, to, reg, noRead, noWrite))
				{
					from.isDest = false;
					return false;
				}
				from.isDest = false;
				if (from.iPar1 == 0)
					return true; //unconditional jump ends here
			}
			if (from == to)
				return true; //no read or write found
			from = from.next;
		}
	}
	
	private boolean tryReplaceRegisterReading(Instruction i, Instruction end, int oldReg, int newReg)
	{
		Instruction tmp;
		int old0, old1, new0, new1;
		long regAccMaskSave;
		
		if ((tmp = searchReadStartingAt(i, oldReg)) == null || tmp.instrNr > end.instrNr)
			return true; //no read or read is after end => done
		if ((tmp.iPar2 & SE_DEST_REGS) != 0 || (newReg < 16 && (tmp.iPar2 & SE_ONLY_UPPER_REGS) != 0) || !noReadWriteUntil(i, tmp.prev, oldReg, true, true) || !noReadWriteUntil(i, tmp.prev, newReg, false, true))
		{
			return false; //no replacement possible
		}
		//try to replace registers
		if ((old0 = tmp.reg0) == oldReg)
			new0 = newReg;
		else
			new0 = old0;
		if ((old1 = tmp.reg1) == oldReg)
			new1 = newReg;
		else
			new1 = old1;
		set(tmp, tmp.type, new0, new1, tmp.iPar1);
		if (isWrite(tmp, newReg) && searchReadStartingAt(tmp.next, newReg) != null)
		{
			//reg is read after writing instruction => copy of register is needed and optimization is not possible
			set(tmp, tmp.type, old0, old1, tmp.iPar1);
			return false;
		}
		regAccMaskSave = tmp.lPar;
		tmp.lPar = 0l; //ignore current instruction, it is altered and should not be taken into considerations
		if (!tryReplaceRegisterReading(i, end, oldReg, newReg))
		{ //register replace failed
			set(tmp, tmp.type, old0, old1, tmp.iPar1);
			return false;
		}
		tmp.lPar = regAccMaskSave;
		return true;
	}
	
	private boolean tryReplaceRegImmADC(Instruction i)
	{ //returns -1 on failure, 0 if carry cleared and 1 if carry set
		ValueInfo vInfo;
		boolean ret = false;
		
		if (((ATmegaOptInstr) i).jSource != null)
			return false;
		if (i.type != I_ADCregreg)
		{
			if (flagRelevance(i, SE_WRITE_FLAG_CARRY) != 0)
				return false;
			immediateBuffer = -immediateBuffer; //add+adc+adc+... is invertable to sub+sbc+sbc+... => invert sign
			return true;
		}
		if (i.reg0 < 16 || (vInfo = searchValueSetting(i, i.reg1)) == null)
			return false;
		if (vInfo.type != ValueInfo.VI_ABS)
		{
			ungetValueInfo(vInfo);
			return false;
		}
		immediateBuffer += (long) (vInfo.imm) << (immediateBufferPos += 8);
		if (tryReplaceRegImmADC(i.next))
		{
			set(i, I_SBCIregimm, i.reg0, 0, ((int) (immediateBuffer >>> immediateBufferPos)) & 0xFF);
			immediateBufferPos -= 8;
			ret = true;
		}
		ungetValueInfo(vInfo);
		return ret;
	}
	
	private ValueInfo searchPointerSetting(Instruction i, int reg)
	{
		Instruction now;
		ValueInfo ret = null, tmp;
		int value;
		
		//check normally
		if ((now = searchWriteBefore(i, reg, false)) == null)
			return null;
		switch (now.type)
		{
			case I_EORregreg:
				if (now.reg0 != now.reg1)
					break;
				//no break, do the following I_LDIregimm too
			case I_LDIregimm:
				if ((now.next.type != I_LDIregimm && (now.next.type != I_EORregreg || now.next.reg0 != now.next.reg1)) || now.next.reg0 != now.reg0 + 1)
					break;
				if (now.type == I_LDIregimm)
					value = now.iPar1;
				else
					value = 0;
				if (now.next.type == I_LDIregimm)
					value |= now.next.iPar1 << 8;
				(ret = getEmptyValueInfo()).set(ValueInfo.VI_MEMABS, value);
				break;
			case I_MOVWregreg:
				//check special registers
				if (now.reg1 == R_YL)
				{ //stack base pointer relative
					ret = getEmptyValueInfo().set(ValueInfo.VI_MEMREL, 0, R_YL);
				}
				else if ((ret = searchPointerSetting(now, now.reg1)) == null)
				{ //also not other register-pair used as pointer
					ret = searchValueSetting(now, now.reg1);
					tmp = searchValueSetting(now, now.reg1 + 1);
					if (ret != null && tmp != null && ret.type == ValueInfo.VI_ABS && tmp.type == ValueInfo.VI_ABS)
					{
						ret.set(ValueInfo.VI_MEMABS, ret.imm | (tmp.imm << 8));
						ungetValueInfo(tmp);
					}
					else
					{ //did not find setting of registers
						if (ret != null)
						{
							ungetValueInfo(ret);
							ret = null;
						}
						if (tmp != null)
						{
							ungetValueInfo(tmp);
							tmp = null;
						}
					}
				}
				break;
			case I_ADDIWregimm:
				if ((ret = searchPointerSetting(now, now.reg0)) != null)
				{
					ret.imm += now.iPar1;
				}
				break;
			case I_SUBIregimm:
				if (now.next == null || now.next.type != I_SBCIregimm || now.next.reg0 != now.reg0 + 1)
					break;
				if ((ret = searchPointerSetting(now, now.reg0)) != null)
				{
					ret.imm -= (short) (now.iPar1 | (now.next.iPar1 << 8));
				}
				break;
			case I_ADDregreg:
				if (now.next == null || now.next.type != I_ADCregreg || now.next.reg0 != now.reg0 + 1)
					break;
				ret = searchPointerSetting(now, now.reg0);
				tmp = searchPointerSetting(now, now.reg1);
				if (ret != null && ret.type == ValueInfo.VI_MEMABS && tmp != null && tmp.type == ValueInfo.VI_MEMABS)
				{
					ret.imm += tmp.imm;
					ungetValueInfo(tmp);
				}
				else
				{
					if (ret != null)
					{
						ungetValueInfo(ret);
						ret = null;
					}
					if (tmp != null)
					{
						ungetValueInfo(tmp);
						tmp = null;
					}
				}
				break;
			case I_LDZreg:
				if (reg != R_ZL || now.reg0 == R_ZL)
					break;
				ret = searchPointerSetting(now, R_ZL);
				break;
			case I_STZ_DECreg:
			case I_LDZ_DECreg:
				if (reg != R_ZL || now.reg0 == R_ZL)
					break;
				if ((ret = searchPointerSetting(now, R_ZL)) != null)
				{
					ret.imm--;
				}
				break;
			case I_STZ_INCreg:
			case I_LDZ_INCreg:
				if (reg != R_ZL || now.reg0 == R_ZL)
					break;
				if ((ret = searchPointerSetting(now, R_ZL)) != null)
				{
					ret.imm++;
				}
				break;
		}
		return ret;
	}
	
	private ValueInfo searchValueSetting(Instruction i, int reg)
	{
		Instruction now;
		ValueInfo ret = null, tmp1 = null, tmp2 = null;
		int value;
		
		//check special registers
		if (reg == R_ZERO)
		{ //zero register
			return getEmptyValueInfo().set(ValueInfo.VI_ABS, 0);
		}
		//check normally
		if ((now = searchWriteBefore(i, reg, false)) == null)
			return null;
		switch (now.type)
		{
			case I_LDIregimm:
				ret = getEmptyValueInfo().set(ValueInfo.VI_ABS, now.iPar1);
				break;
			case I_MOVregreg:
				ret = searchValueSetting(now, now.reg1);
				break;
			case I_MOVWregreg:
				if (reg == now.reg0)
					ret = searchValueSetting(now, now.reg1);
				else if (reg == now.reg0 + 1)
					ret = searchValueSetting(now, now.reg1 + 1);
				else
					fatalError("invalid searchValueSetting for I_MOVWregreg");
				break;
			case I_ANDregreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && (tmp2 = searchValueSetting(now, now.reg1)) != null && tmp1.type == ValueInfo.VI_ABS && tmp2.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, tmp1.imm & tmp2.imm);
				}
				break;
			case I_ANDIregimm:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, tmp1.imm & now.iPar1);
				}
				break;
			case I_ORregreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && (tmp2 = searchValueSetting(now, now.reg1)) != null && tmp1.type == ValueInfo.VI_ABS && tmp2.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, tmp1.imm | tmp2.imm);
				}
				break;
			case I_ORIregimm:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, tmp1.imm | now.iPar1);
				}
				break;
			case I_EORregreg:
				if (now.reg0 == now.reg1)
					ret = getEmptyValueInfo().set(ValueInfo.VI_ABS, 0);
				else if ((tmp1 = searchValueSetting(now, now.reg0)) != null && (tmp2 = searchValueSetting(now, now.reg1)) != null && tmp1.type == ValueInfo.VI_ABS && tmp2.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, tmp1.imm ^ tmp2.imm);
				}
				break;
			case I_INCreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, (tmp1.imm + 1) & 0xFF);
				}
				break;
			case I_DECreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, (tmp1.imm - 1) & 0xFF);
				}
				break;
			case I_COMreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, (~tmp1.imm) & 0xFF);
				}
				break;
			case I_NEGreg:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && tmp1.type == ValueInfo.VI_ABS)
				{
					ret = tmp1.set(ValueInfo.VI_ABS, (-tmp1.imm) & 0xFF);
				}
				break;
			case I_ADDIWregimm:
				if ((tmp1 = searchValueSetting(now, now.reg0)) != null && (tmp2 = searchValueSetting(now, now.reg0 + 1)) != null && tmp1.type == tmp2.type && tmp1.type == ValueInfo.VI_ABS)
				{
					value = (tmp1.imm | (tmp2.imm << 8) & 0xFFFF) + now.iPar1;
					ret = tmp1;
					if (reg == now.reg0)
						ret.set(ValueInfo.VI_ABS, value & 0xFF);
					else if (reg == now.reg0 + 1)
						ret.set(ValueInfo.VI_ABS, value >>> 8);
					else
						fatalError("invalid searchValueSetting for I_ADDIWregimm");
				}
				break;
			//TODO insert search of register for following instructions
        /*
      case I_ADDregreg:
      case I_SUBregreg:
      case I_ADCregreg:
      case I_SBCregreg:
      case I_SUBIregimm:
      case I_SBCIregimm:
      case I_POPreg:
      case I_LSRreg:
      case I_RORreg:
      case I_ROLreg:
      case I_LSLreg:
      case I_ASRreg:
      case I_MULregreg:
         */
		}
		//clean up
		if (ret != tmp1 && tmp1 != null)
			ungetValueInfo(tmp1);
		if (ret != tmp2 && tmp2 != null)
			ungetValueInfo(tmp2);
		return ret;
	}
	
	private int searchFlagSetting(Instruction i, int flagWriteMask)
	{
		ValueInfo tmp1 = null, tmp2 = null;
		int ret = -1;
		
		while (i != null)
		{
			if (((ATmegaOptInstr) i).jSource != null)
				return -1; //don't follow different branches at the moment
			if ((i.iPar2 & flagWriteMask) != 0)
			{ //instruction writes requested flag
				switch (i.type)
				{
					case I_CPregreg:
						if ((tmp1 = searchValueSetting(i, i.reg0)) != null && (tmp2 = searchValueSetting(i, i.reg1)) != null && tmp1.type == ValueInfo.VI_ABS && tmp2.type == ValueInfo.VI_ABS)
						{
							switch (flagWriteMask)
							{
								case SE_WRITE_FLAG_CARRY:
									ret = tmp2.imm > tmp1.imm ? 1 : 0;
									break;
								case SE_WRITE_FLAG_SIGNED:
									ret = ((tmp1.imm - tmp2.imm) & 0x80) != 0 ? 1 : 0;
									break;
								case SE_WRITE_FLAG_ZERO:
									ret = tmp1.imm == tmp2.imm ? 1 : 0;
									break;
							}
						}
						break;
				}
				if (tmp1 != null)
					ungetValueInfo(tmp1);
				if (tmp2 != null)
					ungetValueInfo(tmp2);
				return ret;
			}
			i = i.prev;
		}
		return -1;
	}
	
	private Instruction searchWriteBefore(Instruction i, int reg, boolean includeInstruction)
	{
		InstrList iList;
		Instruction sideReturn, lastSideReturn = null;
		boolean hadSideLine = false;
		
		while (i != null)
		{
			if (i.isDest)
				return null; //already looked here, nothing found so far
			if (includeInstruction && isWrite(i, reg))
				break;
			includeInstruction = true;
			if ((iList = ((ATmegaOptInstr) i).jSource) != null)
			{
				hadSideLine = true;
				i.isDest = true;
				while (iList != null)
				{
					if (((sideReturn = searchWriteBefore(iList.instr, reg, true)) == null) || (lastSideReturn != null && !sameInstruction(sideReturn, lastSideReturn)))
					{
						i.isDest = false;
						return null;
					}
					lastSideReturn = sideReturn;
					iList = iList.next;
				}
				i.isDest = false;
			}
			i = i.prev;
			if (i != null && i.type == I_JUMP && i.iPar1 == 0)
			{
				return null; //break search if unconditional jump found
			}
		}
		if (hadSideLine && (lastSideReturn == null || !sameInstruction(lastSideReturn, i)))
			return null;
		return i;
	}
	
	private ValueInfo searchLocalSetting(Instruction i, int imm)
	{
		int pushCnt = 0;
		boolean foundFrameStart = false;
		
		i = i.prev;
		while (i != null)
		{
			if (((ATmegaOptInstr) i).jSource != null)
				return null;
			switch (i.type)
			{
				case I_PUSHreg:
					if (i.reg0 == R_YL)
					{
						if (foundFrameStart)
							return null; //do not search over multiple frames
						foundFrameStart = true;
					}
					if (foundFrameStart && ++pushCnt == imm)
						return searchValueSetting(i, i.reg0);
					break;
				case I_POPreg:
					if (i.reg0 == R_YL)
						return null; //do not search over multiple inline frames
					break;
				case I_JUMP:
				case I_STZ_INCreg:
				case I_STZ_DECreg:
				case I_STX_DECreg:
					return null;
				case I_STY_DISPregimm:
					if (foundFrameStart)
						return null;
					if (i.iPar1 == imm)
						return searchValueSetting(i, i.reg0);
					break;
			}
			i = i.prev;
		}
		return null;
	}
	
	private Instruction searchStackFrameWriteBefore(Instruction i, int imm)
	{
		i = i.prev;
		while (i != null)
		{
			if (((ATmegaOptInstr) i).jSource != null)
				return null;
			switch (i.type)
			{
				case I_POPreg:
					if (i.reg0 == R_YL)
						return null; //do not search over multiple inline frames
					break;
				case I_JUMP:
				case I_STZ_INCreg:
				case I_STZ_DECreg:
				case I_STX_DECreg:
					return null;
				case I_STY_DISPregimm:
					if (i.iPar1 == imm)
						return i;
					break;
			}
			i = i.prev;
		}
		return null;
	}
	
	private Instruction searchStackFrameReadAfter(Instruction i, int imm)
	{
		Instruction ret;
		
		while (i != null)
		{
			if (i.isDest)
				return null; //already checked here
			switch (i.type)
			{
				case I_JUMP:
					i.isDest = true;
					if ((ret = searchStackFrameReadAfter(i.jDest, imm)) != null)
					{
						i.isDest = false;
						return ret;
					}
					i.isDest = false;
					if (i.iPar1 == 0)
						return null; //unconditional jump ends here
					break;
				case I_LDX_INCreg:
				case I_LDZreg:
				case I_LDZ_INCreg:
				case I_LDZ_DECreg:
					return i;
				case I_LDY_DISPregimm:
					if (i.iPar1 == imm)
						return i;
					break;
				default:
					if ((i.iPar2 & SE_READ_STACK) != 0)
						return i;
			}
			i = i.next;
		}
		return null;
	}
	
	private Instruction searchCorrespPush(Instruction i, int popCnt)
	{ //initially popCnt has to be 0
		Instruction lastSetter = null, setter;
		InstrList iList;
		
		while (i != null)
		{
			if (i.isDest)
				return null; //already looked here, nothing found so far
			if ((iList = ((ATmegaOptInstr) i).jSource) != null)
			{
				i.isDest = true;
				while (iList != null)
				{
					if (((setter = searchCorrespPush(iList.instr, popCnt)) == null) || (lastSetter != null && setter != lastSetter))
					{
						i.isDest = false;
						return null;
					}
					lastSetter = setter;
					iList = iList.next;
				}
				i.isDest = false;
			}
			switch (i.type)
			{
				case I_CALL:
				case I_ICALL:
				case I_CALLpatched:
					if (popCnt <= i.iPar3)
						return null; //can not reduce push/pop for call
					break;
				case I_OUTimmreg:
					if (i.iPar1 == 0x3D)
						return null; //writing stack pointer is not handled
					break;
				case I_PUSHip:
					if ((popCnt -= 2) <= 0)
						return null; //can not split PUSHip
					break;
				case I_PUSHreg:
					if (--popCnt <= 0)
					{
						if (popCnt == 0)
							return i;
						return null;
					}
					break;
				case I_POPreg:
					popCnt++;
					break;
				case I_JUMP: //ignore these instructions with type<0
				case I_ADDpatched:
				case I_IHELPER:
				case I_BSHOPHINT:
					break;
				default:
					if (i.type < 0 || (i.iPar2 & SE_WRITE_STACK) != 0 || isWrite(i, R_YL) || isWrite(i, R_YL))
						return null;
			}
			i = i.prev;
			if (i != null && i.type == I_JUMP && i.iPar1 == 0)
				return lastSetter; //unconditional jump ends linear search
		}
		return null;
	}
	
	private Instruction searchReadStartingAt(Instruction i, int reg)
	{
		Instruction tmp;
		
		while (i != null)
		{
			if (i.isDest)
				return null; //already looked here, nothing found so far
			if (i.type == I_JUMP)
			{
				i.isDest = true;
				if ((tmp = searchReadStartingAt(i.jDest, reg)) != null)
				{
					i.isDest = false;
					return tmp;
				}
				i.isDest = false;
				if (i.iPar1 == 0)
					return null; //unconditional jump with checked jDest
			}
			if (isRead(i, reg))
				return i;
			if (isWrite(i, reg))
				return null;
			i = i.next;
		}
		return null;
	}
	
	private boolean sameInstruction(Instruction a, Instruction b)
	{
		if (a == null || b == null)
			return false;
		return (a == b || (a.type != I_MAGC && a.type == b.type //implies b.type!=I_MAGC
				&& a.reg0 == b.reg0 && a.reg1 == b.reg1 && a.reg2 == b.reg2 && a.iPar1 == b.iPar1 && a.iPar2 == b.iPar2 && a.iPar3 == b.iPar3 && a.lPar == b.lPar && a.refMthd == b.refMthd && getNextRealIns(a.jDest) == getNextRealIns(b.jDest)));
	}
	
	private Instruction getNextRealIns(Instruction i)
	{
		while (i != null && i.type == I_NONE)
			i = i.next;
		return i;
	}
	
	private boolean noJumpDestBetween(Instruction i, Instruction end)
	{
		while (i != null)
		{
			if (((ATmegaOptInstr) i).jSource != null)
				return false;
			if (i == end)
				return true;
			i = i.next;
		}
		fatalError("cozld not find end instr");
		return false;
	}
}
