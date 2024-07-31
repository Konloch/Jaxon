package kernel.memory;

import kernel.Kernel;
import rte.SClassDesc;

public class EmptyObject extends Object
{
	
	@SJC.Inline
	public EmptyObject next()
	{
		if (_r_next == null)
			return null;
		
		return (EmptyObject) _r_next;
	}
	
	@SJC.Inline
	public void setNext(EmptyObject next)
	{
		MAGIC.assign(_r_next, (Object) next);
	}
	
	@SJC.Inline
	public static int minimumClassSize()
	{
		return MAGIC.getInstRelocEntries("EmptyObject") * MAGIC.ptrSize + MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int baseScalarSize()
	{
		return MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int relocEntries()
	{
		return MAGIC.getInstRelocEntries("EmptyObject");
	}
	
	@SJC.Inline
	public static SClassDesc type()
	{
		return MAGIC.clssDesc("EmptyObject");
	}
	
	@SJC.Inline
	public void shrinkBy(int shrinkBy)
	{
		MAGIC.assign(_r_scalarSize, _r_scalarSize - shrinkBy);
		if (_r_scalarSize < 4)
			Kernel.panic("EmptyObject::ShrinkBy: _r_scalarSize < 4");
	}
	
	@SJC.Inline
	public void expandBy(int expandBy)
	{
		if (expandBy < 0)
			Kernel.panic("EmptyObject::ExpandBy: expandBy < 0");
		
		MAGIC.assign(_r_scalarSize, _r_scalarSize + expandBy);
	}
	
	@SJC.Inline
	public int unreservedScalarSize()
	{
		return _r_scalarSize - MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int relocEntriesSize()
	{
		return MAGIC.getInstRelocEntries("EmptyObject") * MAGIC.ptrSize;
	}
}
