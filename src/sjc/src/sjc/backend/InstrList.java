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

package sjc.backend;

/**
 * InstrList: list of instructions to be fixed up
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 081015 made fields public to allow reusage in optimizers
 * version 070114 reduced access level where possible
 * version 060627 initial version
 */

public class InstrList
{
	public Instruction instr;
	public int offset;
	public InstrList next;
}
