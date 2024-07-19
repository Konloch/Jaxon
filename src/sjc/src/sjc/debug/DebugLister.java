/* Copyright (C) 2015 Stefan Frenz
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

import sjc.memory.MemoryImage;


/**
 * DebugLister: interface for memory debug lister (aka memory dumper)
 *
 * @author S. Frenz
 * @version 151108 initial version
 */

public abstract class DebugLister
{
	public DebugLister nextLister; //chaining of DebugListers
	
	//start/end image debug and global information
	public abstract void globalRAMInfo(boolean isDecompressor, Object ramInitLoc, int ramSize, int constMemorySize);
	
	//function to dump memory
	public abstract void listMemory(MemoryImage mem);
}
