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

import sjc.backend.x86.AMD64;
import sjc.compbase.Context;
import sjc.compbase.Mthd;
import sjc.memory.MemoryImage;

/**
 * SSAopt2amd: AMD64 dependent optimizer on SSA basis
 *
 * @author F. Hercher
 * @version 100923 initial version
 */

public class SSAopt2amd extends AMD64 implements SSAopt2bin
{
	
	public void doInit(MemoryImage imem, int ilev, Context ictx)
	{
		
		init(imem, ilev, ictx);
		ctx.out.println("ssa2amd backend doInit done");
	}
	
	public void doPrepareMethod(Mthd mthd)
	{
		
		prepareMethodCoding(mthd);
	}
	
	public int doMethodSize()
	{
		
		return getMethodSize();
	}
	
	public void doPutRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		
		putRef(loc, offset, ptr, ptrOff);
	}
	
	public void doPutCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		
		putCodeRef(loc, offset, ptr, ptrOff);
	}
	
	public void doCopyMethod(Mthd generatingMthd, Object loc, int offset)
	{
		
		copyMethodCode(generatingMthd, loc, offset);
	}
	
	public void doEnumerate()
	{
		
		enumerateInstructions();
	}
	
	public void doFinalizeMethod()
	{
		
		finalizeMethodCoding();
	}
	
}