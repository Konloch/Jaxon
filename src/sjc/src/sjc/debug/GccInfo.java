/* Copyright (C) 2008, 2009 Stefan Frenz
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

package sjc.debug;

import sjc.compbase.Context;
import sjc.memory.ImageContainer;

/**
 * GccInfo: symbol information writer for gcc
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 080718 writing real values out of created image
 * version 080717 moved most methods to newly created MthdInfo, added filling bytes
 * version 080712 initial version
 */

public class GccInfo extends MthdInfo
{
	private int memBase, memSize;
	private final Context ctx;
	private ImageContainer img;
	
	public GccInfo(String filename, Context ic)
	{
		super(filename, ic);
		ctx = ic;
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
		memBase = baseAddress;
		memSize = memBlockLen;
	}
	
	public void finalizeImageInfo()
	{
		MthdList mthd = firstMthd;
		int i = memBase;
		
		if (ctx.mem instanceof ImageContainer)
			img = (ImageContainer) ctx.mem;
		else
			ctx.out.println("memory is not readable, using dummy values");
		finalOut.print("// baseAddress is 0x");
		finalOut.printHexFix(memBase, 8);
		finalOut.print(", memBlockLen is ");
		finalOut.print(memSize);
		finalOut.println(" bytes");
		finalOut.println("// start of sjc-generated debug output for gcc");
		while (mthd != null)
		{
			if (mthd.codeSize > 0)
			{
				if (mthd.codeStart < i)
				{
					finalOut.println();
					finalOut.println("//sjc internal error: wrong method order");
					finalOut.close();
					ctx.out.println("sjc internal error: invalid method order in GccInfo");
					ctx.err = true;
					return;
				}
				fillBlock(i, i = mthd.codeStart, 0x90);
				printMethodInfo(mthd);
				fillBlock(i, i += mthd.codeSize, 0xC3);
				finalOut.print("// end of ");
				finalOut.println(mthd.namePlain);
			}
			else
				printMethodInfo(mthd);
			mthd = mthd.nextMthd;
		}
		fillBlock(i, memBase + memSize, 0x90);
		finalOut.println("// end of sjc-generated debug output for gcc");
		finalOut.close();
	}
	
	private void fillBlock(int start, int nextBlockStart, int defaultValue)
	{
		int i = 0;
		
		start -= img.baseAddress;
		nextBlockStart -= img.baseAddress;
		while (start < nextBlockStart)
		{
			if (i == 0)
				finalOut.print(".byte 0x");
			else
				finalOut.print(",0x");
			finalOut.printHexFix(img != null ? ((int) img.memBlock[start]) & 0xFF : defaultValue, 2);
			if (++i == 16)
			{
				finalOut.println();
				i = 0;
			}
			start++;
		}
		if (i != 0)
			finalOut.println();
	}
	
	private void printMethodInfo(MthdList mthd)
	{
		finalOut.print(mthd.unit);
		finalOut.print('_');
		finalOut.print(mthd.nameWithPar);
		finalOut.print(": // at 0x");
		finalOut.printHexFix(mthd.codeStart, 8);
		finalOut.print(" with ");
		finalOut.print(mthd.codeSize);
		finalOut.println(" bytes");
	}
}
