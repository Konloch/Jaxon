/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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

import sjc.osio.TextPrinter;

/**
 * Pack: nameservice directory
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 110512 re-invented NamedObject
 * version 101231 removed never used reference to NamedObject
 * version 101219 added reference to NamedObject
 * version 101218 replaced getTypeTo by getQIDTo
 * version 091123 removed no longer needed symHint
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 071222 added support for inner units
 * version 070909 added getTypeTo to avoid multiple creation of types
 * version 070727 renamed "parent" to "outer" to match runtime symbol information, added symHint
 * version 070713 publicated access levels to support symbol informer, avoid printing "root."
 * version 070114 reduced access level where possible
 * version 060607 initial version
 */

public class Pack
{
	public String name;
	public Pack outer, subPacks, nextPack;
	public Unit units;
	public NamedObject objs;
	
	private QualID qidOfThis;
	
	public Pack(String in, Pack ip)
	{
		name = in;
		outer = ip;
	}
	
	public QualID getQIDTo()
	{
		if (qidOfThis == null)
		{
			qidOfThis = new QualID(new StringList(name), QualID.Q_PACKAGE, -1, -1, -1);
			qidOfThis.packDest = this;
		}
		return qidOfThis;
	}
	
	public void printFullName(TextPrinter v)
	{
		if (outer != null && outer.name != null)
		{
			outer.printFullName(v);
			v.print('.');
		}
		if (name != null)
			v.print(name);
		else
			v.print("root");
	}
	
	public Pack searchSubPackage(String what)
	{
		Pack sub;
		
		sub = subPacks;
		while (sub != null)
		{
			if (sub.name.equals(what))
				return sub;
			sub = sub.nextPack;
		}
		return null;
	}
	
	public Pack searchSubPackage(StringList what, boolean createIfNew)
	{
		Pack sub = subPacks, last = null;
		boolean found = false;
		
		while (sub != null)
		{
			last = sub;
			if (sub.name.equals(what.str))
			{
				found = true;
				break;
			}
			sub = sub.nextPack;
		}
		if (!found)
		{
			if (!createIfNew)
				return null; //not found
			if (searchUnit(what.str) != null)
				return null; //class with this name already exists
			if (last != null)
			{
				last.nextPack = new Pack(what.str, this);
				sub = last.nextPack;
			}
			else
			{
				sub = new Pack(what.str, this);
				subPacks = sub;
			}
		}
		if (what.next == null)
			return sub; //found everything
		//search in subPackage
		return sub.searchSubPackage(what.next, createIfNew);
	}
	
	public Unit searchUnit(String what)
	{
		Unit u;
		
		u = units;
		while (u != null)
		{
			if (u.name.equals(what))
				return u;
			u = u.nextUnit;
		}
		return null; //name not found
	}
	
	public Unit searchUnit(StringList what)
	{
		Pack sub = subPacks;
		Unit u, res;
		StringList cur;
		
		if (what.next == null)
			return searchUnit(what.str); //no subpackage left, search units
		//subpackages existing
		if (what.next != null)
			while (sub != null)
			{ //search subpackage
				if (sub.name.equals(what.str))
					return sub.searchUnit(what.next);
				sub = sub.nextPack;
			}
		//try to find inner unit
		u = units;
		while (u != null)
		{
			res = u;
			cur = what;
			while (res != null)
			{
				if (cur.str.equals(res.name))
				{ //found current inner unit
					if ((cur = cur.next) == null)
						return res; //found all
					res = res.innerUnits; //get next level
				}
				else
					res = res.nextUnit; //try next inner unit
			}
			u = u.nextUnit; //try next unit
		}
		//nothing found
		return null;
	}
	
	public boolean addUnit(Unit c)
	{
		Unit u;
		Pack sub = subPacks;
		
		while (sub != null)
		{
			if (sub.name.equals(c.name))
				return false; //package with this name already exists
			sub = sub.nextPack;
		}
		if (units == null)
		{
			units = c;
			return true;
		}
		u = units;
		while (u.nextUnit != null)
		{ //search entered units except last one
			if (u.name.equals(c.name))
				return false; //name already entered
			u = u.nextUnit;
		}
		//compare last one, enter if not equal
		if (u.name.equals(c.name))
			return false; //name already entered
		u.nextUnit = c;
		return true;
	}
	
	public Object searchNamedObject(String name)
	{
		NamedObject o = objs;
		while (o != null)
		{
			if (o.name.equals(name))
				return o.item;
			o = o.nextObj;
		}
		return null;
	}
}
