/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Stefan Frenz
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

package sjc.ui;

import sjc.backend.ssa.SSADef;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * DisSSA: disassemble created SSA-binaries to human-readable text
 *
 * @author S. Frenz
 * @version 101101 adopted change of genDeref
 * version 090717 added I_STKCHK
 * version 090207 added copyright notice
 * version 081021 adopted change of genComp, genReadIO and genWriteIO
 * version 080616 added I_TF*
 * version 080508 added I_FLOWHINT
 * version 080207 adopted changes in semantics of I_IVOF to hold all parameters
 * version 080203 added I_ENTERINL and I_LEAVEINL to support method inlining, changed float/double handling
 * version 070912 added new instruction for inlineVarOffset
 * version 070114 added parSize for calls
 * version 061202 adopted change of genCall
 * version 060714 changed bound semantics
 * version 060610 adapted changed in jump conditions
 * version 060607 initial version
 */

public class DisSSA
{
	private static int offset;
	private static FileInputStream fis;
	
	public static void main(String[] args)
	{
		int param, type;
		
		if (args == null || args.length < 1)
		{
			System.out.println("DisAssembler for pseudo-SSA-code generated by SC");
			System.out.println("Please give a file to translate as parameter");
			System.out.println("  any other given parameter will be printed");
			return;
		}
		for (param = 1; param < args.length; param++)
		{ //insert comment
			System.out.println(args[param]);
		}
		
		try
		{
			fis = new FileInputStream(args[0]);
		}
		catch (IOException e)
		{
			System.out.print("Could not open file: ");
			System.out.println(e.getMessage());
			return;
		}
		
		offset = 0;
		try
		{
			while (fis.available() > 0)
			{
				param = readByte();
				type = readByte();
				if ((param & SSADef.IPJMPDEST) != 0)
				{
					System.out.print("j");
					System.out.print(offset - 2); //2 to undo the two readByte for param and type
					System.out.println(":");
					param &= ~SSADef.IPJMPDEST;
				}
				System.out.print("    ");
				switch (type)
				{
					case 0xFF & SSADef.I_MARKER:
						printImmI("marker", param);
						break;
					case 0xFF & SSADef.I_ENTER:
						printMthdHint("enter", param);
						break;
					case 0xFF & SSADef.I_ENTERINL:
						printMthdHint("enterinl", param);
						break;
					case 0xFF & SSADef.I_NFREG:
						printImmI("nfreg", param);
						break;
					case 0xFF & SSADef.I_LEAVE:
						printMthdHint("leave", param);
						break;
					case 0xFF & SSADef.I_LEAVEINL:
						printMthdHint("leaveinl", param);
						break;
					case 0xFF & SSADef.I_LOADim_i:
						printRegSizePara("loadi", param);
						break;
					case 0xFF & SSADef.I_LOADim_l:
						printRegSizeImmL("loadl", param);
						break;
					case 0xFF & SSADef.I_LOADim_p:
						printRegImmI("loadp", param);
						break;
					case 0xFF & SSADef.I_LOADnp:
						printRegSize("loadn", param);
						break;
					case 0xFF & SSADef.I_LOADaddr:
						printRegDereg("loada", param);
						break;
					case 0xFF & SSADef.I_LOADval:
						printRegDeregSize("loadv", param);
						break;
					case 0xFF & SSADef.I_CONV:
						printRegRegSizeSize("conv", param);
						break;
					case 0xFF & SSADef.I_COPY:
						printRegRegSize("copy", param);
						break;
					case 0xFF & SSADef.I_PUSHim_i:
						printSizePara("pushi", param);
						break;
					case 0xFF & SSADef.I_PUSHim_l:
						printImmL("pushl", param);
						break;
					case 0xFF & SSADef.I_PUSHnp:
						printNoPar("pushn", param);
						break;
					case 0xFF & SSADef.I_PUSH:
						printRegSize("push", param);
						break;
					case 0xFF & SSADef.I_POP:
						printRegSize("pop", param);
						break;
					case 0xFF & SSADef.I_SAVE:
						printRegSizePara("save", param);
						break;
					case 0xFF & SSADef.I_REST:
						printRegSizePara("rest", param);
						break;
					case 0xFF & SSADef.I_ASSIGN:
						printAddrRegSize("assign", param);
						break;
					case 0xFF & SSADef.I_AND:
						printRegRegRegSize("and", param);
						break;
					case 0xFF & SSADef.I_XOR:
						printRegRegRegSize("xor", param);
						break;
					case 0xFF & SSADef.I_OR:
						printRegRegRegSize("or", param);
						break;
					case 0xFF & SSADef.I_ADD:
						printRegRegRegSize("add", param);
						break;
					case 0xFF & SSADef.I_SUB:
						printRegRegRegSize("sub", param);
						break;
					case 0xFF & SSADef.I_MUL:
						printRegRegRegSize("mul", param);
						break;
					case 0xFF & SSADef.I_DIV:
						printRegRegRegSize("div", param);
						break;
					case 0xFF & SSADef.I_MOD:
						printRegRegRegSize("mod", param);
						break;
					case 0xFF & SSADef.I_SHL:
						printRegRegRegSize("shl", param);
						break;
					case 0xFF & SSADef.I_SHRL:
						printRegRegRegSize("shrl", param);
						break;
					case 0xFF & SSADef.I_SHRA:
						printRegRegRegSize("shra", param);
						break;
					case 0xFF & SSADef.I_NOT:
						printRegRegSize("not", param);
						break;
					case 0xFF & SSADef.I_NEG:
						printRegRegSize("neg", param);
						break;
					case 0xFF & SSADef.I_BINV:
						printRegReg("binv", param);
						break;
					case 0xFF & SSADef.I_INCmem:
						printAddrSize("incm", param);
						break;
					case 0xFF & SSADef.I_DECmem:
						printAddrSize("decm", param);
						break;
					case 0xFF & SSADef.I_CALL:
						printRegImmIPar("call", param);
						break;
					case 0xFF & SSADef.I_CALLind:
						printRegImmIPar("calli", param);
						break;
					case 0xFF & SSADef.I_CALLim_p:
						printImmIPar("callp", param);
						break;
					case 0xFF & SSADef.I_CMP:
						printRegRegSizePara("cmp", param);
						break;
					case 0xFF & SSADef.I_OUT:
						printRegRegSizePara("out", param);
						break;
					case 0xFF & SSADef.I_IN:
						printRegRegSizePara("io", param);
						break;
					case 0xFF & SSADef.I_BOUND:
						printRegRegBound("bound", param);
						break;
					case 0xFF & SSADef.I_DEREF:
						printRegRegDeregPara("deref", param);
						break;
					case 0xFF & SSADef.I_MOVEMAP:
						printReg("movemap", param);
						break;
					case 0xFF & SSADef.I_ALLOCREG:
						printRegSize("alloc", param);
						break;
					case 0xFF & SSADef.I_KILLREG:
						printReg("kill", param);
						break;
					case 0xFF & SSADef.I_KILLOJMP:
						printReg("killonjump", param);
						break;
					case 0xFF & SSADef.I_REGRANGE:
						printRegReg("regrange", param);
						break;
					case 0xFF & SSADef.I_EXCEPT:
						printImmI("exception", param);
						break;
					case 0xFF & SSADef.I_IVOF:
						printInlineVarOffset(param);
						break;
					case 0xFF & SSADef.I_FLOWHINT:
						printImmIPar("flowhint", param);
						break;
					case 0xFF & SSADef.I_TFBUILD:
						printRegImmIPar("tfbuild", param);
						break;
					case 0xFF & SSADef.I_TFUPD1:
						printImmIPar("tfupd1", param);
						break;
					case 0xFF & SSADef.I_TFUPD2:
						printImmIPar("tfupd1", param);
						break;
					case 0xFF & SSADef.I_TFRESET:
						printRegImmI("tfreset", param);
						break;
					case 0xFF & SSADef.I_STKCHK:
						printReg("stkchk", param);
						break;
					case 0xFF & SSADef.I_JUMP:
						printJump(param);
						break;
					case 0xFF & SSADef.I_INLINE:
						printInline(param);
						break;
					default:
						System.out.println("###unknown instruction param/type: " + param + "/" + type);
						fis.close();
						return;
				}
				System.out.println();
			}
			fis.close();
		}
		catch (IOException e)
		{
			System.out.println("Error in processing file: " + e.getMessage());
			return;
		}
	}
	
	private static int readByte() throws IOException
	{
		offset++;
		return fis.read() & 0xFF;
	}
	
	private static int readInt() throws IOException
	{
		return readByte() | (readByte() << 8) | (readByte() << 16) | (readByte() << 24);
	}
	
	private static long readLong() throws IOException
	{
		long l0, l1;
		l0 = readInt() & 0xFFFFFFFF;
		l1 = readInt() & 0xFFFFFFFF;
		return l0 | (l1 << 32);
	}
	
	private static void printJump(int param) throws IOException
	{
		int par, rel;
		System.out.print("jump ");
		if (param != SSADef.IPJMPCOND)
			throw new IOException("invalid param for printJump");
		par = readByte();
		switch (par)
		{
			case SSADef.CC_AL:
				System.out.print("always j");
				break;
			case SSADef.CC_LW:
				System.out.print("ifless j");
				break; //jump if less
			case SSADef.CC_LE:
				System.out.print("ifleeq j");
				break; //jump if less or equal
			case SSADef.CC_EQ:
				System.out.print("ifequa j");
				break; //jump if equal
			case SSADef.CC_GE:
				System.out.print("ifeqgr j");
				break; //jump if greater or equal (==not less)
			case SSADef.CC_GT:
				System.out.print("ifgrea j");
				break; //jump if greater (==not less or equal)
			case SSADef.CC_NE:
				System.out.print("ifuneq j");
				break; //jump if not equal
			case SSADef.CC_BO:
				System.out.print("ifbdok j");
				break; //jump if bound ok
			default:
				System.out.print("###invcond j");
		}
		rel = readInt(); //readInt will change the offset!
		System.out.print(rel + offset);
	}
	
	private static void printMthdHint(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_im_i | SSADef.IP_para))
			throw new IOException("invalid param for printDual");
		System.out.print(" ");
		System.out.print(readInt()); //size of local variables
		System.out.print(",");
		System.out.print(readInt()); //size of method parameters
	}
	
	private static void printImmI(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != SSADef.IP_im_i)
			throw new IOException("invalid param for printImmI");
		System.out.print(" ");
		System.out.print(readInt());
	}
	
	private static void printImmIPar(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_im_i | SSADef.IP_para))
			throw new IOException("invalid param for printImmI");
		System.out.print(" ");
		System.out.print(readInt());
		System.out.print(",par==");
		System.out.print(readInt());
	}
	
	private static void printSizePara(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_size | SSADef.IP_para))
			throw new IOException("invalid param for printSizePara");
		internalPrintSize(readByte());
		System.out.print(readInt());
	}
	
	private static void printImmL(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != SSADef.IP_im_l)
			throw new IOException("invalid param for printImmL");
		System.out.print(" ");
		System.out.print(readLong());
	}
	
	private static void printReg(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != SSADef.IP_reg0)
			throw new IOException("invalid param for printReg");
		System.out.print(" r");
		System.out.print(readInt());
	}
	
	private static void printRegImmI(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_im_i))
			throw new IOException("invalid param for printRegImmI");
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",");
		System.out.print(readInt());
	}
	
	private static void printRegImmIPar(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_im_i | SSADef.IP_para))
			throw new IOException("invalid param for printRegImmI");
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",");
		System.out.print(readInt());
		System.out.print(",par==");
		System.out.print(readInt());
	}
	
	private static void printRegSizePara(String operator, int param) throws IOException
	{
		int reg;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_size | SSADef.IP_para))
			throw new IOException("invalid param for printRegImmI");
		reg = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg);
		System.out.print(",");
		System.out.print(readInt());
	}
	
	private static void printRegSizeImmL(String operator, int param) throws IOException
	{
		int reg;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_size | SSADef.IP_im_l))
			throw new IOException("invalid param for printRegImmL");
		reg = readInt();
		internalPrintSize(readByte());
		System.out.print(" r");
		System.out.print(reg);
		System.out.print(",");
		System.out.print(readLong());
	}
	
	private static void printRegDereg(String operator, int param) throws IOException
	{
		int rela;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_rela))
			throw new IOException("invalid param for printRegAddr");
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",[r");
		System.out.print(readInt());
		rela = readInt();
		if (rela > 0)
		{
			System.out.print("+");
			System.out.print(rela);
		}
		else if (rela < 0)
		{
			System.out.print("-");
			System.out.print(-rela);
		}
		//else: rela==0, do not print relative
		System.out.print("]");
	}
	
	private static void printRegDeregSize(String operator, int param) throws IOException
	{
		int reg0, reg1;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_size))
			throw new IOException("invalid param for printRegDeregSize");
		reg0 = readInt();
		reg1 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
		System.out.print(",[r");
		System.out.print(reg1);
		System.out.print("]");
	}
	
	private static void internalPrintSize(int size)
	{
		switch (size)
		{
			case 0xFE:
				System.out.print(" dptr ");
				break;
			case 0xFF:
				System.out.print(" ptr ");
				break;
			case 1:
				System.out.print(" byte ");
				break;
			case 2:
				System.out.print(" short ");
				break;
			case 4:
				System.out.print(" int ");
				break;
			case 5:
				System.out.print(" float ");
				break;
			case 8:
				System.out.print(" long ");
				break;
			case 9:
				System.out.print(" double ");
				break;
			default:
				System.out.print(" us");
				System.out.print(size);
				System.out.print(" ");
		}
	}
	
	private static void internalPrintRegRegSize(String operator, int param) throws IOException
	{
		int reg0, reg1;
		
		reg0 = readInt();
		reg1 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
		System.out.print(",r");
		System.out.print(reg1);
	}
	
	private static void printRegRegSizeSize(String operator, int param) throws IOException
	{
		int reg0, reg1;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_size | SSADef.IP_para))
			throw new IOException("invalid param for printRegRegSizeSize");
		
		reg0 = readInt();
		reg1 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
		System.out.print(" from");
		internalPrintSize(readInt()); //para is int, so skip unused 3 bytes here
		System.out.print("r");
		System.out.print(reg1);
	}
	
	private static void printRegReg(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1))
			throw new IOException("invalid param for printRegReg");
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",r");
		System.out.print(readInt());
	}
	
	private static void printRegRegSize(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_size))
			throw new IOException("invalid param for printRegRegSize");
		internalPrintRegRegSize(operator, param);
	}
	
	private static void printRegRegSizePara(String operator, int param) throws IOException
	{
		System.out.print(operator);
		int reg0, reg1;
		
		reg0 = readInt();
		reg1 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
		System.out.print(",r");
		System.out.print(reg1);
		System.out.print(",par==");
		System.out.print(readInt());
	}
	
	private static void printNoPar(String operator, int param) throws IOException
	{
		System.out.print(operator);
		if (param != 0)
			throw new IOException("invalid param for printNoPar");
	}
	
	private static void printAddrRegSize(String operator, int param) throws IOException
	{
		int reg0, reg1;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_size))
			throw new IOException("invalid param for printRegRegSize");
		reg0 = readInt();
		reg1 = readInt();
		internalPrintSize(readByte());
		System.out.print("[r");
		System.out.print(reg0);
		System.out.print("],r");
		System.out.print(reg1);
	}
	
	private static void printRegRegRegSize(String operator, int param) throws IOException
	{
		int reg0, reg1, reg2;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_reg2 | SSADef.IP_size))
			throw new IOException("invalid param for printRegRegRegSize");
		reg0 = readInt();
		reg1 = readInt();
		reg2 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
		System.out.print(",r");
		System.out.print(reg1);
		System.out.print(",r");
		System.out.print(reg2);
	}
	
	private static void printRegSize(String operator, int param) throws IOException
	{
		int reg0;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_size))
			throw new IOException("invalid param for printRegSize");
		reg0 = readInt();
		internalPrintSize(readByte());
		System.out.print("r");
		System.out.print(reg0);
	}
	
	private static void printAddrSize(String operator, int param) throws IOException
	{
		int reg0;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_size))
			throw new IOException("invalid param for printAddrSize");
		reg0 = readInt();
		internalPrintSize(readByte());
		System.out.print("[r");
		System.out.print(reg0);
		System.out.print("]");
	}
	
	private static void printRegRegBound(String operator, int param) throws IOException
	{
		int reg;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_rela))
			throw new IOException("invalid param for printRegRegBound");
		reg = readInt();
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",[r");
		System.out.print(reg);
		System.out.print("+");
		System.out.print(readInt());
		System.out.print("]");
		
	}
	
	private static void printRegRegDeregPara(String operator, int param) throws IOException
	{
		int rela;
		
		System.out.print(operator);
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_reg0 | SSADef.IP_reg1 | SSADef.IP_reg2 | SSADef.IP_im_i | SSADef.IP_para))
			throw new IOException("invalid param for printRegDeregPara");
		System.out.print(" r");
		System.out.print(readInt());
		System.out.print(",[r");
		System.out.print(readInt());
		System.out.print("+r");
		System.out.print(readInt());
		rela = readInt();
		System.out.print("*(");
		System.out.print(readInt());
		System.out.print(")");
		if (rela > 0)
		{
			System.out.print("+");
			System.out.print(rela);
		}
		else if (rela < 0)
		{
			System.out.print("-");
			System.out.print(-rela);
		}
		//else: rela==0, do not print relative
		System.out.print("]");
	}
	
	private static void printInline(int param) throws IOException
	{
		System.out.print("inline ");
		if (param < 1)
			throw new IOException("invalid param for printInline");
		System.out.print(readByte()); //at least there is one byte
		while (--param > 0)
		{ //reduce before because of leading byte
			System.out.print(",");
			System.out.print(readByte());
		}
	}
	
	private static void printInlineVarOffset(int param) throws IOException
	{
		int iPar1, iPar2;
		long lPar;
		System.out.print("inlvof ");
		if ((param << SSADef.IPOPTSHFT) != (SSADef.IP_im_i | SSADef.IP_para | SSADef.IP_im_l))
			throw new IOException("invalid param for printInlineVarOffset");
		iPar1 = readInt(); //pos
		lPar = readLong(); //upper: mode, lower: byteCount
		iPar2 = readInt(); //additionalOffset
		System.out.print((int) lPar); //byteCount
		System.out.print(",");
		System.out.print((int) (lPar >>> 32)); //mode
		System.out.print(",");
		System.out.print(iPar1); //pos
		System.out.print(",");
		System.out.print(iPar2); //additionalOffset
	}
}
