package java.lang;

import rte.SClassDesc;

/**
 * @author S. Frenz
 */
public class Object
{
	public SClassDesc _r_type; //fixed by compiler: reloc-position 0
	
	public boolean equals(Object o)
	{
		return this == o;
	}
	
	public String toString()
	{
		return "Object";
	}
}