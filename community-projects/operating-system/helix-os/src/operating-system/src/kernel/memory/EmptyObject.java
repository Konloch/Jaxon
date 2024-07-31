package kernel.memory;

import kernel.Kernel;
import rte.SClassDesc;

public class EmptyObject extends Object
{
	
	@SJC.Inline
	public EmptyObject Next()
	{
		if (_r_next == null)
		{
			return null;
		}
		return (EmptyObject) _r_next;
	}
	
	@SJC.Inline
	public void SetNext(EmptyObject next)
	{
		MAGIC.assign(_r_next, (Object) next);
	}
	
	@SJC.Inline
	public static int MinimumClassSize()
	{
		return MAGIC.getInstRelocEntries("EmptyObject") * MAGIC.ptrSize + MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int BaseScalarSize()
	{
		return MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int RelocEntries()
	{
		return MAGIC.getInstRelocEntries("EmptyObject");
	}
	
	@SJC.Inline
	public static SClassDesc Type()
	{
		return MAGIC.clssDesc("EmptyObject");
	}
	
	@SJC.Inline
	public void ShrinkBy(int shrinkBy)
	{
		MAGIC.assign(_r_scalarSize, _r_scalarSize - shrinkBy);
		if (_r_scalarSize < 4)
		{
			Kernel.panic("EmptyObject::ShrinkBy: _r_scalarSize < 4");
		}
	}
	
	@SJC.Inline
	public void ExpandBy(int expandBy)
	{
		if (expandBy < 0)
		{
			Kernel.panic("EmptyObject::ExpandBy: expandBy < 0");
		}
		MAGIC.assign(_r_scalarSize, _r_scalarSize + expandBy);
	}
	
	@SJC.Inline
	public int UnreservedScalarSize()
	{
		return _r_scalarSize - MAGIC.getInstScalarSize("EmptyObject");
	}
	
	@SJC.Inline
	public static int RelocEntriesSize()
	{
		return MAGIC.getInstRelocEntries("EmptyObject") * MAGIC.ptrSize;
	}
}
