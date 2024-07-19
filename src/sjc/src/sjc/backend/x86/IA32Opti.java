/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2015, 2019 Stefan Frenz
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

package sjc.backend.x86;

import sjc.backend.Instruction;
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.StdTypes;

/**
 * IA32Opti: architecture implementation for optimized 32 bit protected mode IA32 processors
 *
 * @author S. Frenz
 * @version 190322 fixed multi-lea-movmemreg offset calculation
 * version 151026 adopted changed X86Base
 * version 101224 added MOVZXregreg
 * version 100115 adopted changed error reporting
 * version 091109 added support for POPdummy
 * version 091013 added optimization for calculatable address for PUSHmem
 * version 090629 added support for stack extreme check
 * version 090430 added support for native "return missing" hint
 * version 090307 optimized jump optimization, limiting too deep analysis in hasReadAfter to avoid "hanging machine"
 * version 090207 added copyright notice
 * version 081209 added support for method printing
 * version 081015 added support for K_NOPT mark
 * version 080607 added support for instructions required for language throwables
 * version 071228 added optimization to remove unnecessary MOVregreg
 * version 070915 added FWAIT
 * version 070816 added FCOMIP
 * version 070815 added new float/double instructions
 * version 070812 added support for float/double instructions, first optimizations
 * version 070624 fixed too optimistic optimization for LEA-MOVSX
 * version 070623 added LEAregmem-MOVregmem-optimization
 * version 070611 fixed too optimistic optimization in hasReadAfter
 * version 070601 adopted change of boolean register usage in IA32
 * version 070514 added jump following flow control hasReadAfter, added several optimization for MOVSXregmem
 * version 070513 fixed insPatchedCall, added optimization for ADD/SUB/AND/XOR/OR-regmem
 * version 070504 changed naming of Clss to Unit, changed OutputObject to Unit
 * version 070501 adopted optimization of insPatched in IA32
 * version 070108 added debug-basics, added optimization for SHL/SHR-regimm
 * version 070104 added push-call-pop- and movimm-push-optimization
 * version 070101 added optimization for MOVSX and MOVZX
 * version 061231 added support for AND/XOR/OR-regimm and -memimm
 * version 061230 added optimization for INCmem/DECmem and memory-ADD/SUB
 * version 061229 removed access to firstInstr
 * version 061227 added several optimizing routines
 * version 061225 adopted change in access to method information
 * version 061221 bugfix for push after mov/xor optimization
 * version 061111 bugfixes and extensions initiated by invention of structs
 * version 060723 extension of IA32 to reduce codesize
 * version 060720 initial version
 */

public class IA32Opti extends IA32
{
	public void finalizeInstructions(Instruction first)
	{
		Instruction now;
		boolean redo;
		
		//optimize
		optimize(first); //will also print the code if requested
		//code instructions
		now = first;
		while (now != null)
		{
			switch (now.type)
			{
				case I_JUMP:
				case I_ADDpatched:
				case I_IHELPER:
					break;
				case I_STEXreg:
					fixStackExtremeAdd(now);
					break;
				default:
					code(now, now.type, now.reg0, now.reg1, now.iPar1, now.iPar2, now.iPar3);
			}
			now = now.next;
		}
		//fix jumps
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
		//print final disassembly if requested
		if ((mthdContainer.marker & Marks.K_PRCD) != 0 || ctx.printCode)
			printCode(ctx.out, first, "postOpt", false);
	}
	
	public void insPatchedCall(Mthd refMthd, int par)
	{
		Instruction i;
		
		//get a new instruction and insert it
		i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = I_CALLimm;
		i.refMthd = refMthd;
		//code instruction
		i.putByte(0xE8);
		i.putInt(0);
		addToCodeRefFixupList(i, 1);
		i.reg2 = writeReg(R_EAX) | writeReg(R_EDX) | writeReg(R_EBX) | writeReg(R_ECX) | readReg(R_ESI) | readReg(R_EDI) | readReg(R_ESP) | readReg(R_EBP);
		i.iPar3 = par;
	}
	
	private int readReg(int r)
	{
		if (r == 0)
			return 0;
		if ((r & RS_E) == RS_H)
			r -= 0x40;
		return 0x00000001 << (r >>> 4);
	}
	
	private int writeReg(int r)
	{
		if (r == 0)
			return 0;
		if ((r & RS_E) == RS_H)
			r -= 0x40;
		return 0x00010000 << (r >>> 4);
	}
	
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm, int par)
	{
		Instruction i;
		
		//get a new instruction and insert it
		i = getUnlinkedInstruction();
		appendInstruction(i);
		set(i, type, reg0, reg1, disp, imm, par);
		return i;
	}
	
	private boolean isRead(Instruction i, int r)
	{
		if (r == 0)
			return false;
		return (i.reg2 & (0x00000001 << (r >>> 4))) != 0;
	}
	
	private boolean isWrite(Instruction i, int r)
	{
		if (r == 0)
			return false;
		return (i.reg2 & (0x00010000 << (r >>> 4))) != 0;
	}
	
	private boolean isRegisterFixed(Instruction i)
	{
		switch (i.type)
		{
			case I_BSRregreg:
			case I_OUTreg:
			case I_SHLmemimm:
			case I_SHRmemimm:
			case I_SARmemimm:
			case I_CALLreg:
			case I_CALLmem:
			case I_IMULregreg:
			case I_SHLregimm:
			case I_SHRregimm:
			case I_SARregimm:
			case I_ROLregimm:
			case I_RORregimm:
			case I_RCRregimm:
			case I_MULreg:
			case I_IMULreg:
			case I_DIVreg:
			case I_IDIVreg:
			case I_SHLregreg:
			case I_SHRregreg:
			case I_SHLDregreg:
			case I_SHRDregreg:
			case I_SARregreg:
			case I_CDQ:
			case I_INreg:
			case I_SAHF:
			case I_ADDpatched:
			case I_STEXreg:
				return true;
		}
		return false;
	}
	
	private void set(Instruction i, int type, int reg0, int reg1, int disp, int imm, int par)
	{
		if (i.refMthd != null)
		{
			fatalError("instruction to be set has refMthd!=null");
			return;
		}
		switch (i.type = type)
		{
			//standard instructions
			case I_MOVregreg:
			case I_MOVregmem:
			case I_MOVSXregreg:
			case I_MOVSXregmem:
			case I_MOVZXregmem:
			case I_MOVZXregreg:
			case I_LEAregmem:
			case I_BSRregreg:
				i.reg2 = writeReg(reg0) | readReg(reg1);
				break;
			case I_MOVmemreg:
			case I_CMPregreg:
			case I_CMPregmem:
			case I_TESTregreg:
				i.reg2 = readReg(reg0) | readReg(reg1);
				break;
			case I_OUTreg:
				i.reg2 = readReg(R_EDX) | readReg(R_EAX);
				break;
			case I_MOVregimm:
				i.reg2 = writeReg(reg0);
				break;
			case I_MOVmemimm:
			case I_PUSHmem:
			case I_ADDmemimm:
			case I_SUBmemimm:
			case I_ANDmemimm:
			case I_XORmemimm:
			case I_ORmemimm:
			case I_SHLmemimm:
			case I_SHRmemimm:
			case I_SARmemimm:
			case I_INCmem:
			case I_DECmem:
			case I_CMPregimm:
			case I_CMPmemimm:
				i.reg2 = readReg(reg0);
				break;
			case I_CALLreg:
			case I_CALLmem:
				i.reg2 = writeReg(R_EAX) | writeReg(R_EDX) | writeReg(R_EBX) | writeReg(R_ECX) | readReg(reg0) | readReg(R_ESI) | readReg(R_EDI) | readReg(R_ESP) | readReg(R_EBP);
				break;
			case I_PUSHreg:
				i.reg2 = readReg(reg0) | writeReg(R_ESP) | readReg(R_ESP);
				break;
			case I_PUSHimm:
			case I_PUSHip:
				i.reg2 = writeReg(R_ESP) | readReg(R_ESP);
				break;
			case I_POPdummy:
			case I_POPreg:
				i.reg2 = writeReg(reg0) | readReg(R_ESP) | writeReg(R_ESP);
				break;
			case I_PUSHA:
			case I_POPA:
				i.reg2 = -1;
				break;
			case I_RETimm:
				switch (curMthd.retRegType)
				{
					case 0:
						i.reg2 = 0;
						break;
					case StdTypes.T_BOOL:
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
					case StdTypes.T_PTR:
						i.reg2 = readReg(R_EAX);
						break;
					case StdTypes.T_LONG:
					case StdTypes.T_DPTR:
						i.reg2 = readReg(R_EAX) | readReg(R_EDX);
						break;
					case StdTypes.T_FLT:
					case StdTypes.T_DBL:
						i.reg2 = 0; //result is on fpu-stack
						break;
					default:
						fatalError("invalid return-type");
						break;
				}
				i.reg2 |= readReg(R_ESP) | writeReg(R_ESP);
				break;
			case I_IRET:
				i.reg2 = readReg(R_ESP) | writeReg(R_ESP);
				break;
			case I_ADDregreg:
			case I_ADCregreg:
			case I_SUBregreg:
			case I_SBBregreg:
			case I_ANDregreg:
			case I_ORregreg:
			case I_IMULregreg:
			case I_LEAarray: //will result in lea reg0,[reg0+reg1*par+disp] (only for pointers!)
			case I_MOVindexed: //will result in mov reg0,[reg0+reg1] (only for pointers!)
				i.reg2 = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				break;
			case I_XORregreg:
				if (reg0 == reg1)
					i.reg2 = writeReg(reg0);
				else
					i.reg2 = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				break;
			case I_ADDregimm:
			case I_SUBregimm:
			case I_SBBregimm:
			case I_ANDregimm:
			case I_XORregimm:
			case I_ORregimm:
			case I_SHLregimm:
			case I_SHRregimm:
			case I_SARregimm:
			case I_ROLregimm:
			case I_RORregimm:
			case I_RCRregimm:
			case I_INCreg:
			case I_DECreg:
			case I_NEGreg:
			case I_NOTreg:
				i.reg2 = writeReg(reg0) | readReg(reg0);
				break;
			case I_ADDregmem:
			case I_SUBregmem:
			case I_ANDregmem:
			case I_XORregmem:
			case I_ORregmem:
				i.reg2 = writeReg(reg0) | readReg(reg0) | readReg(reg1);
				break;
			case I_MULreg:
			case I_IMULreg:
				i.reg2 = writeReg(R_EAX) | writeReg(R_EDX) | readReg(R_EAX) | readReg(reg0);
				break;
			case I_DIVreg:
			case I_IDIVreg:
				i.reg2 = writeReg(R_EAX) | writeReg(R_EDX) | readReg(R_EAX) | readReg(R_EDX) | readReg(reg0);
				break;
			case I_XCHGregreg:
				i.reg2 = writeReg(reg0) | writeReg(reg1) | readReg(reg0) | readReg(reg1);
				break;
			case I_SHLregreg:
			case I_SHRregreg:
			case I_SHLDregreg:
			case I_SHRDregreg:
			case I_SARregreg:
				i.reg2 = writeReg(reg0) | readReg(reg0) | readReg(R_ECX);
				break;
			case I_CDQ:
				i.reg2 = writeReg(R_EDX) | readReg(R_EAX);
				break;
			case I_INreg:
				i.reg2 = readReg(R_EDX) | readReg(R_EAX);
				break;
			case I_BOUNDEXC:
			case I_RETMSEXC:
			case I_MARKER:
				i.reg2 = 0;
				break;
			case I_SAHF:
				i.reg2 = readReg(R_EAX);
				break;
			//specials
			case I_ADDpatched:
				i.reg2 = i.iPar1 == 0 ? readReg(R_EBX) | writeReg(R_EBX) : readReg(R_EBP);
				break;
			case I_IHELPER:
				i.reg2 = 0;
				break;
			//floating point instructions
			case I_FLDmem:
			case I_FSTPmem:
			case I_FILDmem:
			case I_FISTPmem:
			case I_FISTTPmem:
			case I_FNSTCWmem:
			case I_FLDCWmem:
				if (i.reg0 == R_ESP && i.iPar1 == 0 && i.prev != null && i.prev.type == I_PUSHreg)
				{
					//previous push is inserted just to move the stack pointer
					i.prev.reg2 = writeReg(R_ESP) | readReg(R_ESP); //remove reading of reg0
				}
				i.reg2 = readReg(reg0);
				break;
			case I_FCHS:
			case I_FADDP:
			case I_FSUBP:
			case I_FMULP:
			case I_FDIVP:
			case I_FDUP:
			case I_FXCH:
			case I_FFREE:
			case I_FINCSTP:
			case I_FCOMPP:
			case I_FCOMIP:
			case I_FWAIT:
				i.reg2 = 0;
				break;
			case I_FSTSW:
				i.reg2 = writeReg(R_EAX);
				break;
			case I_STEXreg:
				i.reg2 = readReg(R_EAX) | writeReg(R_EAX);
				break;
			default:
				fatalError("invalid instruction type for ins(.)");
				return;
		}
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = disp;
		i.iPar2 = imm;
		i.iPar3 = par;
	}
	
	//code optimization
	private void optimize(Instruction first)
	{
		Instruction now, tmp, tmp2;
		int size, loopCnt = 0;
		boolean redo = true, flag;
		
		if ((mthdContainer.marker & Marks.K_NOPT) != 0)
			return; //optimizations prohibited
		
		while (redo)
		{
			//print disassembly if wanted
			if ((mthdContainer.marker & Marks.K_PRCD) != 0 || ctx.printCode)
			{
				ctx.out.print("//preOpt before optimizing loop ");
				ctx.out.println(++loopCnt);
				printCode(ctx.out, first, "preOpt", false);
			}
			
			redo = false;
			//try to eliminate jumps and reset jump destination marks
			now = first;
			while (now != null)
			{
				if (now.type == I_JUMP && isUntilNextRealIns(now.next, now.jDest))
				{ //do not jump to next instruction
					kill(now);
					redo = true;
				}
				now.isDest = false;
				now = now.next;
			}
			//locate jump-destinations
			now = first;
			while (now != null)
			{
				if (now.type == I_JUMP)
					now.jDest.isDest = true;
				now = now.next;
			}
			//try to eliminate effectless opcodes
			now = first;
			while (now != null)
			{
				if (now.type != I_NONE)
				{
					if (effectlessWrite(now))
					{
						kill(now);
						redo = true;
					}
					else if (now.type == I_POPreg && (tmp = searchCorrespPush(now)) != null && tryReplacePushPop(tmp, now))
						redo = true;
				}
				now = now.next;
			}
			//try to merge opcodes
			now = first;
			while (now != null)
			{
				switch (now.type)
				{
					case I_MOVregimm:
						if ((tmp = searchCorrespReadAfter(now, now.reg0, false)) == null || hasReadAfter(tmp, now.reg0))
							break;
						switch (tmp.type)
						{
							case I_MOVmemreg:
								if ((tmp.reg1 & ~RS_E) == (now.reg0 & ~RS_E))
								{
									if ((tmp.reg1 & RS_E) == RS_E)
										size = 4;
									else if ((tmp.reg1 & RS_E) == RS_X)
										size = 2;
									else
										size = 1;
									set(tmp, I_MOVmemimm, tmp.reg0, 0, tmp.iPar1, now.iPar2, size);
									kill(now);
									redo = true;
								}
								else if (tmp.reg0 == now.reg0)
								{
									set(tmp, I_MOVmemreg, 0, tmp.reg1, tmp.iPar1 + now.iPar2, 0, 0);
									kill(now);
									redo = true;
								}
								break;
							case I_MOVregreg:
							case I_ADDregreg:
							case I_SUBregreg:
							case I_ANDregreg:
							case I_XORregreg:
							case I_ORregreg:
								if (tmp.reg1 != now.reg0)
									break;
								set(tmp, (tmp.type & ~IM_P1) | I_imm1, tmp.reg0, 0, 0, now.iPar2, 0);
								kill(now);
								break;
							case I_SHLregreg:
							case I_SHRregreg:
							case I_SARregreg:
								if (now.reg0 != R_ECX)
									break;
								set(tmp, (tmp.type & ~IM_P1) | I_imm1, tmp.reg0, 0, 0, now.iPar2, 0);
								kill(now);
								break;
							case I_PUSHreg:
								set(tmp, I_PUSHimm, 0, 0, 0, now.iPar2, 0);
								kill(now);
								redo = true;
								break;
						}
						break;
					case I_MOVregmem:
						if ((now.reg0 & RS_E) == RS_E && searchCorrespReadAfter(now, now.reg1, false) == null && (tmp = searchCorrespImmWriteBefore(now, now.reg1)) != null && tmp.type == I_MOVregreg)
						{
							set(now, I_MOVregmem, now.reg0, tmp.reg1, now.iPar1, 0, 0);
							redo = true;
						}
						break;
					case I_MOVregreg:
						if ((now.reg0 & RS_E) == RS_E)
						{
							if (now.reg0 == now.reg1)
							{
								kill(now);
								redo = true;
							}
							else if ((tmp = searchCorrespImmWriteBefore(now, now.reg1)) != null)
								switch (tmp.type)
								{
									case I_MOVregreg:
									case I_MOVregmem:
									case I_MOVregimm:
										//tmp is mov reg,*
										if ((tmp2 = searchCorrespReadAfter(now, now.reg1, true)) == null)
										{
											//no read after now => replace destination of tmp and remove now
											set(tmp, tmp.type, now.reg0, tmp.reg1, tmp.iPar1, tmp.iPar2, tmp.iPar3);
											kill(now);
											redo = true;
										}
										else if (!tmp2.isDest && tmp2.type != I_JUMP && tmp2.type != I_MAGC && !isRegisterFixed(tmp2) && searchCorrespReadAfter(tmp2, now.reg1, true) == null && searchCorrespWriteBefore(tmp2, now.reg0) == now)
										{
											//tmp is mov reg1,*; now is mov reg2,reg1; tmp2 is read of reg1
											//after tmp2 there is no access to reg1
											//=> replace destination of tmp, remove now and change register of tmp2
											if (tmp2.reg0 == tmp.reg0)
												tmp2.reg0 = now.reg0;
											if (tmp2.reg1 == tmp.reg0)
												tmp2.reg1 = now.reg0;
											set(tmp, tmp.type, now.reg0, tmp.reg1, tmp.iPar1, tmp.iPar2, tmp.iPar3);
											set(tmp2, tmp2.type, tmp2.reg0, tmp2.reg1, tmp2.iPar1, tmp2.iPar2, tmp2.iPar3);
											kill(now);
											redo = true;
										}
										//else: no optimization strategy available
										break;
								}
						}
						break;
					case I_MOVSXregreg:
					case I_MOVZXregreg:
						if ((tmp = searchCorrespReadAfter(now, now.reg0, false)) == null || tmp.type != I_MOVmemreg || hasReadAfter(tmp, now.reg0) || (tmp.reg1 & RS_E) != (now.reg1 & RS_E))
							break;
						if (readReg(now.reg0) == readReg(now.reg1))
						{
							set(tmp, I_MOVmemreg, tmp.reg0, now.reg1, tmp.iPar1, tmp.iPar2, tmp.iPar3);
						}
						kill(now);
						redo = true;
						break;
					case I_MOVSXregmem:
						if ((tmp = searchCorrespReadAfter(now, now.reg0, false)) == null)
							break;
						switch (tmp.type)
						{
							case I_MOVmemreg:
								if (!hasReadAfter(tmp, now.reg0) && (tmp.reg1 & RS_E) == (now.iPar3 == 1 ? RS_L : RS_X))
								{
									set(now, I_MOVregmem, (now.reg0 & ~RS_E) | (tmp.reg1 & RS_E), now.reg1, now.iPar1, 0, 0);
									redo = true;
								}
								break;
							case I_MOVSXregreg:
								if ((tmp.reg0 & ~RS_E) == (tmp.reg1 & ~RS_E))
								{
									if (tmp.iPar3 == 1)
										set(now, I_MOVSXregmem, now.reg0, now.reg1, now.iPar1, 0, 1);
									kill(tmp);
									redo = true;
								}
								break;
						}
						break;
					case I_PUSHreg:
						if ((now.reg0 & RS_E) != RS_E || (tmp = searchCorrespImmWriteBefore(now, now.reg0)) == null || hasReadAfter(now, now.reg0))
							break;
						switch (tmp.type)
						{
							case I_POPreg:
								kill(now);
								kill(tmp);
								redo = true;
								break;
							case I_MOVregimm:
								set(tmp, I_PUSHimm, 0, 0, 0, tmp.iPar2, 0);
								kill(now);
								redo = true;
								break;
							case I_MOVregmem:
								set(now, I_PUSHmem, tmp.reg1, 0, tmp.iPar1, 0, 0);
								kill(tmp);
								redo = true;
								break;
							case I_XORregreg:
								set(now, I_PUSHimm, 0, 0, 0, 0, 0);
								kill(tmp);
								redo = true;
								break;
						}
						break;
					case I_PUSHmem:
						if ((now.reg0 & RS_E) != RS_E || (tmp = searchCorrespImmWriteBefore(now, now.reg0)) == null || hasReadAfter(now, now.reg0))
							break;
						if (tmp.type == I_MOVregimm)
						{
							set(now, I_PUSHmem, 0, 0, now.iPar1 + tmp.iPar2, 0, 0);
							kill(tmp);
							redo = true;
						}
						break;
					case I_INCmem:
					case I_DECmem:
						if ((tmp = searchCorrespImmWriteBefore(now, now.reg0)) == null || tmp.type != I_LEAregmem || hasReadAfter(now, now.reg0))
							break;
						set(now, now.type, tmp.reg1, 0, tmp.iPar1, 0, now.iPar3);
						kill(tmp);
						redo = true;
						break;
					case I_ADDregimm:
					case I_SUBregimm:
					case I_XORregimm:
					case I_ORregimm:
					case I_SARregimm:
					case I_SHRregimm:
					case I_SHLregimm:
						if (tryMergeAriImm(now))
							redo = true;
						break;
					case I_ADDregreg:
					case I_SUBregreg:
					case I_XORregreg:
					case I_ANDregreg:
						if (tryMergeAriReg(now))
							redo = true;
						break;
					case I_ANDregimm:
						if (tryMergeAriImm(now))
							redo = true;
						else
						{
							if ((flag = now.iPar2 != 0xFF) && now.iPar2 != 0xFFFF)
								break;
							if ((tmp = searchCorrespImmWriteBefore(now, now.reg0)) == null)
								break;
							if (tmp.type == I_MOVSXregmem)
							{ //need closer look
								if (flag && tmp.iPar2 == 1)
									break; //break if source is 8 bit and dest 16 bit
							}
							else if (tmp.type != I_MOVregmem)
								break;
							if (flag)
								set(now, I_MOVZXregmem, now.reg0, tmp.reg1, tmp.iPar1, 0, 2); //16 bit
							else
								set(now, I_MOVZXregmem, now.reg0, tmp.reg1, tmp.iPar1, 0, 1); //8 bit
							kill(tmp);
							redo = true;
						}
						break;
					case I_ORregreg:
						if (now.reg0 != now.reg1)
						{
							if (tryMergeAriReg(now))
								redo = true;
						}
						else
						{
							if ((tmp = searchCorrespImmWriteBefore(now, now.reg0)) != null)
								switch (tmp.type & IM_OP)
								{
									case I_AND:
									case I_OR:
									case I_XOR:
									case I_ADD:
									case I_SUB:
										//flag setting already done, just kill opcode before
										kill(now);
										redo = true;
										break;
									default:
										if (now.next.type != I_JUMP || hasReadAfter(now.next, now.reg0))
											break;
										switch (tmp.type)
										{
											case I_MOVregmem:
												set(tmp, I_CMPmemimm, tmp.reg1, 0, tmp.iPar1, 0, (tmp.reg0 & RS_E) == RS_E ? 4 : 1);
												kill(now);
												redo = true;
												break;
											case I_MOVSXregmem:
												set(tmp, I_CMPmemimm, tmp.reg1, 0, tmp.iPar1, 0, tmp.iPar3);
												kill(now);
												redo = true;
												break;
										}
								}
						}
						break;
					case I_LEAregmem:
						if ((tmp = searchCorrespReadAfter(now, now.reg0, false)) == null || mayBeWritten(now, tmp, now.reg1) != null || hasReadAfter(tmp, now.reg0))
							break;
						switch (tmp.type)
						{
							case I_MOVmemreg:
								if (tmp.reg0 == now.reg0)
								{
									set(tmp, I_MOVmemreg, now.reg1, tmp.reg1, now.iPar1 + tmp.iPar1, 0, 0);
									kill(now);
									redo = true;
								}
								break;
							case I_MOVregmem:
								if (tmp.reg1 == now.reg0)
								{
									set(tmp, tmp.type, tmp.reg0, now.reg1, now.iPar1 + tmp.iPar1, 0, 0);
									kill(now);
									redo = true;
								}
								break;
							case I_MOVmemimm:
							case I_ADDmemimm:
							case I_SUBmemimm:
							case I_ANDmemimm:
							case I_XORmemimm:
							case I_ORmemimm:
								set(tmp, tmp.type, now.reg1, 0, now.iPar1 + tmp.iPar1, tmp.iPar2, tmp.iPar3);
								kill(now);
								redo = true;
								break;
							case I_LEAregmem:
								if (tmp.reg0 == now.reg0 && tmp.reg1 == now.reg1)
								{
									set(tmp, I_LEAregmem, now.reg0, now.reg1, now.iPar1 + tmp.iPar1, 0, 0);
									kill(now);
									redo = true;
								}
								break;
							case I_FLDmem:
							case I_FILDmem:
							case I_FSTPmem:
							case I_FISTTPmem:
								set(tmp, tmp.type, now.reg1, 0, now.iPar1 + tmp.iPar1, 0, tmp.iPar3);
								kill(now);
								redo = true;
								break;
						}
						break;
					case I_FILDmem:
						if (now.reg0 == R_ESP && now.iPar1 == 0 && now.iPar3 == FPU32 && (tmp = now.prev) != null && now.prev.type == I_PUSHmem && now.next != null && now.next.type == I_POPreg)
						{
							//found sequence: push [mem] ; fild [ESP] ; pop reg0
							//convert to: nop ; fild [mem] ; nop
							set(now, I_FILDmem, tmp.reg0, 0, tmp.iPar1, 0, now.iPar3);
							kill(tmp);
							kill(now.next);
							redo = true;
						}
						break;
					case I_FXCH:
						if (now.prev != null && now.prev.type == I_FLDmem && (tmp = now.prev.prev) != null && tmp.type == I_FLDmem)
						{
							//found sequence: fld(1) ; fld(2) ; fxch
							//convert to: nop ; fld(2) ; fld(1)
							set(now, I_FLDmem, tmp.reg0, 0, tmp.iPar1, 0, tmp.iPar3);
							kill(tmp);
							redo = true;
						}
						break;
				}
				now = now.next;
			}
		}
	}
	
	private boolean tryMergeAriReg(Instruction now)
	{
		Instruction tmp;
		int destType = (now.type & ~IM_P1) | I_mem1;
		
		if ((tmp = searchCorrespImmWriteBefore(now, now.reg1)) == null || tmp.type != I_MOVregmem || hasReadAfter(now, now.reg1))
			return false;
		set(now, destType, now.reg0, tmp.reg1, tmp.iPar1, tmp.iPar2, tmp.iPar3);
		kill(tmp);
		return true;
	}
	
	private boolean tryMergeAriImm(Instruction now)
	{
		Instruction tmp1, tmp2;
		int destType = (now.type & ~IM_P0) | I_mem0;
		boolean flag;
		
		if ((tmp1 = searchCorrespImmWriteBefore(now, now.reg0)) == null || tmp1.type != I_MOVregmem || (tmp2 = searchCorrespReadAfter(now, now.reg0, false)) == null)
			return false;
		flag = hasReadAfter(tmp2, now.reg0);
		if (tmp2.type != I_MOVmemreg || tmp1.reg0 != tmp2.reg1 || tmp1.reg0 != now.reg0 || tmp1.reg1 != tmp2.reg0 || tmp1.iPar1 != tmp2.iPar1)
			return false;
		set(now, destType, tmp1.reg1, 0, tmp1.iPar1, now.iPar2, 4);
		if (flag)
			set(tmp2, tmp1.type, tmp1.reg0, tmp1.reg1, tmp1.iPar1, tmp1.iPar2, tmp1.iPar3);
		else
			kill(tmp2);
		kill(tmp1);
		return true;
	}
	
	private Instruction searchCorrespPush(Instruction now)
	{
		int popCnt = 0;
		
		while (now != null)
		{
			if (now.isDest || now.type < 0)
				return null;
			switch (now.type)
			{
				case I_CALLimm:
				case I_CALLreg:
				case I_CALLmem:
					popCnt += now.iPar3 >>> 2;
					break;
				case I_PUSHreg:
				case I_PUSHimm:
				case I_PUSHmem:
				case I_PUSHip:
					if (--popCnt <= 0)
					{
						if (popCnt == 0)
							return now;
						return null;
					}
					break;
				case I_POPreg:
					popCnt++;
					break;
				default:
					if (isRead(now, R_ESP) || isWrite(now, R_ESP))
						return null;
			}
			now = now.prev;
		}
		return null;
	}
	
	private boolean tryReplacePushPop(Instruction push, Instruction pop)
	{
		Instruction writer;
		
		switch (push.type)
		{
			case I_PUSHreg:
				if (push.reg0 != pop.reg0)
					break;
				if ((writer = mayBeWritten(push, pop, pop.reg0)) == null)
				{ //no writer between push and pop
					kill(pop);
					kill(push);
					return true;
				}
				switch (writer.type)
				{ //try to replace
					case I_CALLreg:
					case I_CALLmem:
					case I_CALLimm:
						if (hasReadAfter(writer, pop.reg0) || (writer = searchCorrespWriteBefore(push, push.reg0)) == null || writer.type != I_LEAregmem || writer.reg0 != push.reg0 || (writer.reg1 != R_EBP && writer.reg1 != R_ESI && writer.reg1 != R_EDI))
							break;
						kill(push);
						set(pop, I_LEAregmem, writer.reg0, writer.reg1, writer.iPar1, writer.iPar2, writer.iPar3);
						//lea will be removed in next optimize-run if not needed anymore
						return true;
				}
				break;
			case I_PUSHimm:
				set(pop, I_MOVregimm, pop.reg0, 0, 0, push.iPar2, 0);
				kill(push);
				return true;
		}
		return false;
	}
	
	private Instruction searchCorrespImmWriteBefore(Instruction i, int reg)
	{
		if (i.isDest || i.type < 0)
			return null;
		i = i.prev;
		while (i != null)
		{
			if (i.isDest || i.type < 0)
				return null;
			switch (i.type)
			{
				case I_NONE:
					break;
				case I_CALLimm:
				case I_CALLreg:
				case I_CALLmem:
					return null;
				default:
					if (isWrite(i, reg))
						return i;
					return null;
			}
			i = i.prev;
		}
		return null;
	}
	
	private Instruction searchCorrespWriteBefore(Instruction i, int reg)
	{
		if (i.isDest || i.type < 0)
			return null;
		i = i.prev;
		while (i != null)
		{
			if (i.isDest || i.type < 0)
				return null;
			switch (i.type)
			{
				case I_NONE:
					break;
				case I_CALLimm:
				case I_CALLreg:
				case I_CALLmem:
					return null;
				default:
					if (isWrite(i, reg))
						return i;
			}
			i = i.prev;
		}
		return null;
	}
	
	private Instruction searchCorrespReadAfter(Instruction i, int reg, boolean objOnBreaker)
	{
		i = i.next;
		while (i != null)
		{
			if (i.isDest)
				return objOnBreaker ? i : null;
			switch (i.type)
			{
				case I_NONE:
					break;
				case I_JUMP: //call not needed as instruction after call has isDest==true
				case I_MAGC:
					return objOnBreaker ? i : null;
				default:
					if (isRead(i, reg))
						return i;
					if (isWrite(i, reg))
						return null;
			}
			i = i.next;
		}
		return null;
	}
	
	private int hasReadAfterCalls;
	
	private boolean hasReadAfter(Instruction i, int reg)
	{
		hasReadAfterCalls = 0;
		return hasReadAfterRecursive(i, reg);
	}
	
	private boolean hasReadAfterRecursive(Instruction i, int reg)
	{
		if (++hasReadAfterCalls > 100)
			return true; //limit too deep analysis
		i = i.next;
		while (i != null)
		{
			switch (i.type)
			{
				case I_JUMP:
					//don't follow backward jumps, on conditional jumps also check following instructions
					if (i.jDest.instrNr <= i.instrNr || (i.iPar1 != 0 && hasReadAfterRecursive(i, reg)))
						return true;
					//follow the jump
					i = i.jDest;
					break;
				case I_MAGC:
					//treat magic instructions as "reading all registers"
					return true;
				default:
					//check read/write
					if (isRead(i, reg))
						return true;
					if (isWrite(i, reg))
						return false;
					//check next instruction
					i = i.next;
			}
		}
		return false;
	}
	
	private boolean effectlessWrite(Instruction now)
	{
		int regMask;
		
		regMask = now.reg2 >>> 16;
		if (now.isDest || now.type < 0 || regMask == 0)
			return false;
		switch (now.type)
		{
			case I_CALLimm:
			case I_CALLreg:
			case I_CALLmem:
			case I_RETimm:
			case I_IRET:
				return false;
		}
		now = now.next;
		while (now != null)
		{
			if ((now.reg2 & regMask) != 0 || now.isDest || now.type < 0)
				return false;
			switch (now.type)
			{
				case I_CALLimm:
				case I_CALLreg:
				case I_CALLmem:
					return true;
				case I_RETimm:
				case I_IRET:
					return false;
			}
			regMask &= ~now.reg2 >>> 16;
			if (regMask == 0)
				return true;
			now = now.next;
		}
		return true;
	}
	
	private Instruction mayBeWritten(Instruction now, Instruction end, int reg)
	{
		if (now.isDest || now.type < 0)
			return now;
		switch (now.type)
		{
			case I_RETimm:
			case I_IRET:
				return now;
		}
		now = now.next;
		while (now != end)
		{
			if (isWrite(now, reg) || now.isDest || now.type < 0)
				return now;
			switch (now.type)
			{
				case I_RETimm:
				case I_IRET:
					return now;
			}
			now = now.next;
		}
		return null;
	}
}
