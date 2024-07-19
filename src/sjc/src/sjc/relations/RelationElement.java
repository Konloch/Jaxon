/* Copyright (C) 2009, 2010 Patrick Schmidt, Stefan Frenz
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

package sjc.relations;

import sjc.compbase.AccVar;
import sjc.compbase.Mthd;
import sjc.compbase.Unit;

/**
 * RelationElement: one specific relation
 *
 * @author P. Schmidt, S. Frenz
 * @version 100826 redesign
 * version 091020 redesign
 * version 091009 initial version
 */

public class RelationElement
{
	
	public static final int DEP_STD = 0;
	public static final int DEP_IMPORT = 1;
	public static final int DEP_STRUCTURAL = 2;
	
	public int type;
	public Unit unit;
	public Mthd mthd;
	public AccVar var;
	public RelationElement next, referencedBy;
}
