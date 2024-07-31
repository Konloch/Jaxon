
package rte;

public class SPackage { // zusätzliche Klasse
    public static SPackage root; // statisches Feld für root-Package
    public String name; // einfacher Name des Packages
    public SPackage outer; // höhergelegenes Package, noPackOuter deaktiviert*
    public SPackage subPacks; // erstes tiefergelegenes Package
    public SPackage nextPack; // nächstes Package auf gleicher Höhe
    public SClassDesc units; // erste Unit des aktuellen Packages
}
