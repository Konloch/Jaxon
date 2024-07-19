/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Stefan Frenz
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
 * StringList: linear list of strings
 *
 * @author S. Frenz
 * @version 110607 got buildStringList from FrontAdmin
 * version 090207 added copyright notice
 * version 060607 initial version
 */

public class StringList
{
	public String str;
	public StringList next;
	//required fields code-generation in ExStr and Clss
	public int tablePos; //this is the index in the corresponding class-reloc-table
	
	public StringList(String istr)
	{
		str = istr;
		tablePos = -1;
	}
	
	public StringList(StringList last, String istr)
	{
		if (last != null)
			last.next = this;
		str = istr;
		tablePos = -1;
	}
	
	public static StringList buildStringList(String source)
	{
		StringList ret = null, last = null;
		int start = 0, end = 0, count = 0;
		char c;
		
		while (end < source.length())
		{
			c = source.charAt(end);
			if (c == '.')
			{
				count++;
				last = new StringList(last, source.substring(start, end++));
				if (ret == null)
					ret = last;
				start = end;
			}
			else
				end++;
		}
		if (start != end)
		{
			new StringList(last, source.substring(start, end));
			count++;
		}
		if (ret != null)
			ret.tablePos = count;
		return ret;
	}
}
