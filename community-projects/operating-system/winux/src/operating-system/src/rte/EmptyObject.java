package rte;

import devices.StaticV24;

public class EmptyObject {

    // the way EmptyObject is initialized, these attributes are not set (!)
    // null or 0 by default, prev must be set outside (not set to "this" as shown here!)
    public EmptyObject next = null;
    public Object prev = this;

    /**
     * Add new object to EmptyObject. If successful, _r_scalarSize of this EmptyObject
     * is decreased by the amount of space the object needs.
     *
     * If the requested object does not fit, the method returns null.
     */
    public Object addObject(int scalarSize, int relocEntries, SClassDesc type) {

        int i, start, end, address;
        Object me;

        end = MAGIC.cast2Ref(this) + this._r_scalarSize;
        address = end - ((scalarSize+3)&~3);
        start = address - (relocEntries << 2);

        if(start <= MAGIC.cast2Ref(this)) {
            // not enough space
            return null;
        }

        for(i = start; i < end; i += 4) {
            MAGIC.wMem32(i, 0);
        }

        me = MAGIC.cast2Obj(address);
        MAGIC.assign(me._r_relocEntries, relocEntries);
        MAGIC.assign(me._r_scalarSize, scalarSize);
        MAGIC.assign(me._r_type, type);

        // adjust next pointers
        MAGIC.assign(me._r_next, (Object) next);
        MAGIC.assign(prev._r_next, me);
        prev = me;

        // adjust space size
        MAGIC.assign(this._r_scalarSize, this._r_scalarSize - (end - start));

        return me;
    }
}
