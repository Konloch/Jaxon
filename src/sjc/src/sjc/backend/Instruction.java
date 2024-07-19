/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2016 Stefan Frenz
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

import sjc.compbase.Mthd;
import sjc.compbase.Token;

/**
 * Instruction: abstract placeholder for a single instruction
 *
 * @author S. Frenz
 * @version 160818 added asmText
 * version 100805 added cleanup
 * version 091102 added token
 * version 091001 adopted changed memory interface
 * version 090922 integrated functionality of little endian instructions for simplicity reasons
 * version 090207 added copyright notice
 * version 080525 removed bPar
 * version 070615 removed realAddr
 * version 070501 changed refObj to refMthd
 * version 061229 removed access to firstInstr
 * version 060720 added third iPar
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public class Instruction
{
	public Instruction prev, next;
	public byte[] code;
	public boolean isDest; //is this instruction a jump-dest?
	public int size, instrNr, type;
	public int reg0, reg1, reg2; //three registers
	public int iPar1, iPar2, iPar3; //three integer-parameters
	public long lPar; //one long-parameter
	public Instruction jDest; //the destination in case of a jump
	public Mthd refMthd; //referenced method object (only used for staticMem compilation)
	public Token token; //generating source-token
	public String asmText; //optional assembler text to be used *instead* of code
	
	public Instruction(int maxInstrSize)
	{
		code = new byte[maxInstrSize]; //maximum length of a single instruction
	}
	
	public void cleanup()
	{
		type = Architecture.I_NONE;
		size = instrNr = 0;
		reg0 = reg1 = reg2 = 0;
		iPar1 = iPar2 = iPar3 = 0;
		lPar = 0l;
		jDest = null;
		isDest = false;
		refMthd = null;
		prev = next = null;
		token = null;
		asmText = null;
	}
	
	public void replaceShort(int off, int val)
	{
		code[off++] = (byte) val;
		code[off] = (byte) (val >> 8);
	}
	
	public void replaceInt(int off, int val)
	{
		code[off++] = (byte) val;
		code[off++] = (byte) (val >> 8);
		code[off++] = (byte) (val >> 16);
		code[off] = (byte) (val >> 24);
	}
	
	public void replaceLong(int off, long val)
	{
		code[off++] = (byte) val;
		code[off++] = (byte) (val >> 8);
		code[off++] = (byte) (val >> 16);
		code[off++] = (byte) (val >> 24);
		code[off++] = (byte) (val >> 32);
		code[off++] = (byte) (val >> 40);
		code[off++] = (byte) (val >> 48);
		code[off] = (byte) (val >> 56);
		
	}
	
	public void putByte(int val)
	{
		code[size++] = (byte) val;
	}
	
	public void putShort(int val)
	{
		replaceShort(size, val);
		size += 2;
	}
	
	public void putInt(int val)
	{
		replaceInt(size, val);
		size += 4;
	}
	
	public void putLong(long val)
	{
		replaceLong(size, val);
		size += 8;
	}
}
