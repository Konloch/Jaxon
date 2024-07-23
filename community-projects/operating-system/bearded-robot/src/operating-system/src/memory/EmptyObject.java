package memory;

final class EmptyObject
{
	// ATTENTION: This class must not define its own attributes in order to be able to manage the
	// manage the free memory of any objects.
	
	public static int getMinimumSize()
	{
		return (MAGIC.getInstRelocEntries("EmptyObject") * 4 + MAGIC.getInstScalarSize("EmptyObject") + 3) & ~3;
	}
	
	public int getSize()
	{
		return (this._r_relocEntries * 4 + this._r_scalarSize + 3) & ~3;
	}
	
	// Prevents EmptyObject objects from being created dynamically.
	private EmptyObject()
	{
	}
}
