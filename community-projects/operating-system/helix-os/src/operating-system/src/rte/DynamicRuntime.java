package rte;

import kernel.Kernel;
import kernel.MemoryLayout;
import kernel.memory.MemoryManager;

public class DynamicRuntime {
    static final int SIZE_FOR_PANIC_CALL = 1024;
    static int stackExtreme = MemoryLayout.PROGRAM_STACK_BOTTOM + SIZE_FOR_PANIC_CALL;

    /*
     * Gets called if the function to be called would exceed stackExtreme.
     * Panics since unrecoverable stack overflow.
     */
    @SJC.StackExtreme
    static void stackExtremeError() {
        // make space for panic call
        stackExtreme -= SIZE_FOR_PANIC_CALL;
        Kernel.panic("Stack Overflow");
    }

    static void nullException() {
        Kernel.panic("Null Pointer Exception");
    }

    public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type) {
        return MemoryManager.AllocateObject(scalarSize, relocEntries, type);
    }

    /*
     * Copied from SJC manual
     */
    public static SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType) {
        int scS = MAGIC.getInstScalarSize("SArray");
        int rlE = MAGIC.getInstRelocEntries("SArray");

        if (arrDim > 1 || entrySize < 0)
            rlE += length; // Array mit Reloc-Elementen
        else
            scS += length * entrySize; // Array mit skalaren Elementen

        SArray obj = (SArray) MemoryManager.AllocateObject(scS, rlE, MAGIC.clssDesc("SArray"));
        MAGIC.assign(obj.length, length);
        MAGIC.assign(obj._r_dim, arrDim);
        MAGIC.assign(obj._r_stdType, stdType);
        MAGIC.assign(obj._r_unitType, unitType);
        return obj;
    }

    /*
     * Copied from SJC manual
     */
    public static void newMultArray(SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize,
            int stdType, Object unitType) {
        int i; // temporäre Variable
        if (curLevel + 1 < destLevel) { // es folgt noch mehr als eine Dimension
            curLevel++; // aktuelle Dimension erhöhen
            for (i = 0; i < parent.length; i++) // jedes Element mit Array befüllen
                newMultArray((SArray[]) ((Object) parent[i]), curLevel, destLevel,
                        length, arrDim, entrySize, stdType, unitType);
        } else { // letzte anzulegende Dimension
            destLevel = arrDim - curLevel; // Zieldimension eines Elementes
            for (i = 0; i < parent.length; i++) // jedes Element mit Zieltyp befüllen
                parent[i] = newArray(length, destLevel, entrySize, stdType, unitType);
        }
    }

    /*
     * Copied from SJC manual
     */
    public static boolean isInstance(Object o, SClassDesc dest, boolean asCast) {
        if (o == null) { // Prüfung auf null
            if (asCast)
                return true; // null darf immer konvertiert werden
            return false; // null ist keine Instanz
        }

        SClassDesc check = o._r_type; // temporäre Variable // für weitere Vergleiche Objekttyp ermitteln
        while (check != null) { // suche passende Klasse
            if (check == dest)
                return true; // passende Klasse gefunden
            check = check.parent; // Elternklasse versuchen
        }
        if (asCast)
            Kernel.panic("Conversion error"); // Konvertierungsfehler
        return false; // Objekt passt nicht zu Klasse
    }

    /*
     * Copied from SJC manual
     */
    public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast) {
        if (o == null)
            return null; // null implementiert nichts

        SIntfMap check = o._r_type.implementations;
        // Liste der Interface-Maps ermitteln
        while (check != null) { // suche passendes Interface
            if (check.owner == dest)
                return check; // Interface gefunden, Map liefern
            check = check.next; // nächste Interface-Map versuchen
        }
        if (asCast)
            Kernel.panic("Conversion error");// Konvertierungsfehler
        return null; // Objekt passt nicht zu Interface
    }

    /*
     * Copied from SJC manual
     */
    public static boolean isArray(SArray o, int stdType, Object unitType, int dim, boolean asCast) {
        // o ist eigentlich Object, Prüfung unten!
        if (o == null) { // Prüfung auf null
            if (asCast)
                return true; // null darf immer konvertiert werden
            return false; // null ist keine Instanz
        }
        if (o._r_type != MAGIC.clssDesc("SArray")) { // Array-Prüfung
            if (asCast)
                Kernel.panic("isArray: Conversion error");
            return false; // kein Array
        }
        if (unitType == MAGIC.clssDesc("SArray")) { // Sonderbehandlung für SArray
            if (o._r_unitType == MAGIC.clssDesc("SArray"))
                dim--; // Array aus SArray
            if (o._r_dim > dim)
                return true; // ausreichende Resttiefe
            if (asCast)
                Kernel.panic("isArray: Conversion error");
            return false; // kein SArray
        }
        if (o._r_stdType != stdType || o._r_dim < dim) { // notwendige Bedingungen
            if (asCast)
                Kernel.panic("isArray: Conversion error");
            return false; // Array mit nicht passenden Elementen
        }
        if (stdType != 0) { // Array aus Basistypen
            if (o._r_dim == dim)
                return true; // passende Tiefe
            if (asCast)
                Kernel.panic("isArray: Conversion error");
            return false; // Array nicht mit passenden Elementen
        }
        // Typ-Prüfung erforderlich
        if (o._r_unitType._r_type == MAGIC.clssDesc("SClassDesc")) { // Instanzen
            SClassDesc clss = (SClassDesc) o._r_unitType;
            while (clss != null) {
                if (clss == unitType)
                    return true;
                clss = clss.parent;
            }
        } else { // Interfaces nicht unterstützt
            Kernel.panic("isArray: Interface not supported");
        }
        if (asCast)
            Kernel.panic("isArray: Conversion error");
        return false; // Array mit nicht passenden Elementen
    }

    /*
     * Copied from SJC manual
     */
    public static void checkArrayStore(SArray dest, SArray newEntry) {
        // newEntry ist eigentlich Object", die Prüfung muss in isArray erfolgen!
        if (dest._r_dim > 1)
            isArray(newEntry, dest._r_stdType, dest._r_unitType,
                    dest._r_dim - 1, true); // Prüfung des Arrays über isArray,
        // falls Dimension des Zielarrays größer 1 ist
        else if (dest._r_unitType == null)
            Kernel.panic("Zuweisungsfehler"); // Zuweisungsfehler,
        // falls Zielarray aus keine Reloc-Elemente hat
        else { // Instanz-Prüfung in allen anderen Fällen
            if (dest._r_unitType._r_type == MAGIC.clssDesc("SClassDesc"))
                isInstance(newEntry, (SClassDesc) dest._r_unitType, true);
            else
                isImplementation(newEntry, (SIntfDesc) dest._r_unitType, true);
        }
    }
}