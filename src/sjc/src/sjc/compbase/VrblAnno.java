/* Copyright (C) 2010 Stefan Frenz
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

package sjc.compbase;

/**
 * VrblAnno: annotated Vrbl
 *
 * @author S. Frenz
 * @version 100401 initial version
 */

public class VrblAnno extends Vrbl
{
	public FilledAnno anno;
	
	public VrblAnno(String ii, int im, FilledAnno ia, int fid, int il, int ic)
	{
		super(ii, im, fid, il, ic);
		anno = ia;
	}
	
	public FilledAnno getAnnotation()
	{
		return anno;
	}
}
