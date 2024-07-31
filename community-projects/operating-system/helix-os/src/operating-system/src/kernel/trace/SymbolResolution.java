package kernel.trace;

import kernel.MemoryLayout;
import rte.SClassDesc;
import rte.SMthdBlock;
import rte.SPackage;

public class SymbolResolution {
    private static SClassDesc bootloaderClassDesc;
    private static SMthdBlock bootloaderMethod;

    public static void Initialize() {
        bootloaderClassDesc = new SClassDesc();
        bootloaderClassDesc.name = "Bootloader";
        bootloaderMethod = new SMthdBlock();
        bootloaderMethod.namePar = "bootloader()";
        bootloaderMethod.owner = bootloaderClassDesc;
        bootloaderMethod.nextMthd = null;
    }

    public static SMthdBlock Resolve(int addr) {
        if (MemoryLayout.BOOTLOADER_START <= addr && addr <= MemoryLayout.BOOTLOADER_END) {
            return bootloaderMethod;
        }
        return ResolveInPackage(addr, SPackage.root);
    }

    private static SMthdBlock ResolveInPackage(int addr, SPackage pkg) {
        while (pkg != null) {
            SMthdBlock found = ResolveInPackage(addr, pkg.subPacks);
            if (found != null) {
                return found;
            }

            found = ResolveInClass(addr, pkg.units);
            if (found != null) {
                return found;
            }
            pkg = pkg.nextPack;
        }
        return null;
    }

    private static SMthdBlock ResolveInClass(int addr, SClassDesc cls) {
        while (cls != null) {
            SMthdBlock found = ResolveInMethodBlock(addr, cls.mthds);
            if (found != null) {
                return found;
            }
            cls = cls.nextUnit;
        }
        return null;
    }

    private static SMthdBlock ResolveInMethodBlock(int addr, SMthdBlock mths) {
        while (mths != null) {
            int start = MAGIC.cast2Ref(mths);
            int end = start + mths._r_scalarSize;
            if (start <= addr && addr <= end) {
                return mths;
            }
            mths = mths.nextMthd;
        }
        return null;
    }
}
