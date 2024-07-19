/* Copyright (C) 2006, 2007, 2008, 2009 Stefan Frenz and Patrick Schmidt
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

/**
 * Listener being informed, if a breakpoint is reached
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060619 intial version
 */
public interface BreakPointListener
{
	
	/**
	 * Method called, if a breakpoint is reached
	 *
	 * @param c the condition that caused the break
	 */
	void breakPointOccurred(Condition c);
	
	/**
	 * Method called to inform for example a GUI that in order to process a step
	 * over, the execution of the emulation has to be continued until a break-
	 * point occurs
	 */
	void proceedAfterStep();
	
	/**
	 * Method called, whenenver the emulator detected an endless loop
	 */
	void endlessLoopDetected();
}
