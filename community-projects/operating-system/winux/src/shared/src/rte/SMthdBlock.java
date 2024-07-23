package rte;

public class SMthdBlock {
    public final static int M_STAT = 0x00000020; //"static" modifier, taken from compiler

    public SClassDesc owner;
    public String namePar; //einfacher Name, vollqualifizierte Parametertypen
    public SMthdBlock nextMthd; //nächste Methode der aktuellen Klasse
    public int modifier; //Modifier der Methode
    public int[] lineInCodeOffset; //optionale Zeilen-Zuordnung zu Code-Bytes**
}
