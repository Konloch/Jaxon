/* Copyright (C) 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.emulation;

import sjc.emulation.ssa.SSAEmul;
import sjc.osio.TextPrinter;
import sjc.real.EmulReal;

/**
 * Factory for obtaining instances of Emulator
 *
 * @author P. Schmidt, S. Frenz
 * @version 090207 added copyright notice
 * version 080203 adopted changed signature of SSAEmul
 * version 060608 initial version
 */
public class EmulFactory
{
	/**
	 * Method printing the known emulators to the given Viewer
	 *
	 * @param v the Viewer to write the information to
	 */
	public static void printKnownEmulators(TextPrinter v)
	{
		v.println(" ssa - Pseudo-SSA virtual machine (default)");
	}
	
	/**
	 * Method for obtaining an instance of Emulator
	 *
	 * @param type the desired type of the emulator
	 * @return an instance corresponding to the type
	 */
	public static Emulator getEmulator(String type)
	{
		if (type == null || type.equals("ssa"))
		{
			return new SSAEmul(new EmulReal());
		}
		return null;
	}
}
