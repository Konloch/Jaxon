/* Copyright (C) 2010 Stefan Frenz, Florian Hercher
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

package sjc.backend.ssaopt;

import sjc.backend.Instruction;
import sjc.backend.ssa.SSA;
import sjc.compbase.Context;
import sjc.compbase.Mthd;
import sjc.memory.MemoryImage;

/**
 * SSAopt: backend independent optimizer on SSA basis
 *
 * @author F. Hercher
 * @version 100923 initial version
 */

public class SSAopt extends SSA
{
	private final SSAopt2bin Backend;
	
	public SSAopt(SSAopt2bin Backend, int iRB)
	{
		
		super(iRB);
		this.Backend = Backend;
	}
	
	/*
	 * overwrite createNewInstruction
	 * to force the use of SSAoptInstruction
	 */
	public Instruction createNewInstruction()
	{
		
		return new SSAoptInstruction(maxInstrCodeSize);
	}
	
	/**
	 * glue logic for init
	 *
	 * @param MemoryImage imem
	 * @param int         ilev max inline level
	 * @param Context     ictx
	 */
	public void init(MemoryImage imem, int ilev, Context ictx)
	{
		super.init(imem, ilev, ictx);
		Backend.doInit(imem, ilev, ictx);
	}
	
	/**
	 * glue logic for prepareMethodCoding
	 *
	 * @param Mthd mthd Method to be coded
	 * @return Mthd Method to be coded
	 */
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		ctx.out.print("SSAopt prepareMethodCoding for: ");
		ctx.out.println(mthd.name);
		Backend.doPrepareMethod(mthd);
		return super.prepareMethodCoding(mthd);
	}
	
	public int getMethodSize()
	{
		return Backend.doMethodSize();
	}
	
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		Backend.doPutRef(loc, offset, ptr, ptrOff);
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		Backend.doPutCodeRef(loc, offset, ptr, ptrOff);
	}
	
	public void copyMethodCode(Mthd generatingMthd, Object loc, int offset)
	{
		ctx.out.print("SSAopt copyMethodCode for: ");
		ctx.out.println(generatingMthd.name);
		Backend.doCopyMethod(generatingMthd, loc, offset);
		disposeMethodCode();
	}
	
	/**
	 * add funny stuff here
	 */
	public void finalizeMethodCoding()
	{
		super.finalizeMethodCoding();
		Backend.doEnumerate();
		Backend.doFinalizeMethod();
	}
}