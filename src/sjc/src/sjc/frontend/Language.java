/* Copyright (C) 2005, 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.frontend;

import sjc.compbase.Context;
import sjc.compbase.StringList;

/**
 * Language: interface for FrontAdmin to a specific language
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 070114 reduced access level where possible
 * version 061211 removed checkEnvironment
 * version 060607 initial version
 */

public abstract class Language
{
	protected abstract void init(Context iCtx);
	
	protected abstract boolean fileCompetence(String name);
	
	protected abstract boolean scanparseFile(StringList fileName);
}
