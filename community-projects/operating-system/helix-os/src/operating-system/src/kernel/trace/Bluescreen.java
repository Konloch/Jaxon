package kernel.trace;

import kernel.MemoryLayout;
import kernel.bios.call.DisplayModes;
import kernel.display.text.TM3;
import kernel.display.text.TM3Color;
import rte.SMthdBlock;

public class Bluescreen {
    private static final byte COL_HEADLINE = TM3Color.RED;
    private static final byte COL_MESSAGE = TM3Color.LIGHT_RED;

    public static void Show(String title, String message) {
        DisplayModes.ActivateTextMode();
        TM3.DisableCursorCaret();
        int pos = 0;
        pos = PrintHeader(pos, title, message);
    }

    public static void Show(String title, String reason,
            int ebp, int eip) {
        DisplayModes.ActivateTextMode();
        TM3.DisableCursorCaret();
        int pos = 0;
        pos = PrintHeader(pos, title, reason);
        pos = TM3.sNewLine(pos);
        pos = PrintStackTrace(pos, ebp, eip);
    }

    public static void Show(String title, String reason,
            int ebp, int eip,
            int rEDI, int rESI, int rEBP, int rESP,
            int rEBX, int rEDX, int rECX, int rEAX) {
        DisplayModes.ActivateTextMode();
        TM3.DisableCursorCaret();
        int pos = 0;
        pos = PrintHeader(pos, title, reason);
        pos = TM3.sNewLine(pos);
        pos = PrintRegisters(pos, rEDI, rESI, rEBP, rESP, rEBX, rEDX, rECX, rEAX);
        pos = TM3.sNewLine(pos);
        pos = TM3.sNewLine(pos);
        pos = PrintStackTrace(pos, ebp, eip);
    }

    private static int PrintHeader(int pos, String title, String reasib) {
        pos = TM3.sPrint(title, pos, COL_HEADLINE);
        pos = TM3.sNewLine(pos);
        if (reasib != null) {
            pos = TM3.sPrint("Reason: ", pos, COL_HEADLINE);
            pos = TM3.sPrintln(reasib, pos, COL_MESSAGE);
        }
        return pos;
    }

    private static int PrintRegisters(int pos, int edi, int esi, int ebp, int esp, int ebx, int edx, int ecx, int eax) {
        pos = TM3.sPrintln("Registers: ", pos, COL_HEADLINE);

        pos = TM3.sPrintln("  Register   | Value         Register   | Value", pos, COL_HEADLINE);
        pos = TM3.sPrintln(" ------------|------------  ------------|------------", pos, COL_HEADLINE);

        pos = PrintRegisterTableEntry(pos, "EDI", edi);
        pos = TM3.sPrint("  ", pos, COL_HEADLINE);
        pos = PrintRegisterTableEntry(pos, "ESI", esi);
        pos = TM3.sNewLine(pos);
        pos = PrintRegisterTableEntry(pos, "EBP", ebp);
        pos = TM3.sPrint("  ", pos, COL_HEADLINE);
        pos = PrintRegisterTableEntry(pos, "ESP", esp);
        pos = TM3.sNewLine(pos);
        pos = PrintRegisterTableEntry(pos, "EBX", ebx);
        pos = TM3.sPrint("  ", pos, COL_HEADLINE);
        pos = PrintRegisterTableEntry(pos, "EDX", edx);
        pos = TM3.sNewLine(pos);
        pos = PrintRegisterTableEntry(pos, "ECX", ecx);
        pos = TM3.sPrint("  ", pos, COL_HEADLINE);
        pos = PrintRegisterTableEntry(pos, "EAX", eax);

        return pos;
    }

    private static int PrintRegisterTableEntry(int pos, String name, int value) {
        pos = TM3.sPrint("  ", pos, COL_MESSAGE);
        pos = TM3.sPrint(name, pos, COL_MESSAGE);
        pos = TM3.sPrint("        | ", pos, COL_HEADLINE);
        pos = TM3.sPrint("0x", pos, COL_MESSAGE);
        pos = TM3.sPrint(value, 16, 8, '0', pos, COL_MESSAGE);
        return pos;
    }

    private static int PrintStackTrace(int pos, int ebp, int eip) {
        pos = TM3.sPrintln("Stacktrace: ", pos, COL_HEADLINE);
        pos = TM3.sPrintln("  EBP        |  EIP       | Method", pos, COL_HEADLINE);
        pos = TM3.sPrintln(" ------------|------------|----------------------------------------------------", pos,
                COL_HEADLINE);

        do {
            pos = TM3.sPrint("  ", pos, COL_MESSAGE);
            pos = TM3.sPrint("0x", pos, COL_MESSAGE);
            pos = TM3.sPrint(ebp, 16, 8, '0', pos, COL_MESSAGE);
            pos = TM3.sPrint(" | ", pos, COL_HEADLINE);
            pos = TM3.sPrint("0x", pos, COL_MESSAGE);
            pos = TM3.sPrint(eip, 16, 8, '0', pos, COL_MESSAGE);
            pos = TM3.sPrint(" | ", pos, COL_HEADLINE);

            SMthdBlock m = SymbolResolution.Resolve(eip);
            if (m != null) {
                int maxLen = 51;
                pos = TM3.sPrint(m.owner.name, pos, COL_MESSAGE, maxLen);
                pos = TM3.sPrint('.', pos, COL_MESSAGE);
                maxLen -= m.owner.name.length() + 1;
                pos = TM3.sPrint(m.namePar, pos, COL_MESSAGE, maxLen);
            } else {
                pos = TM3.sPrint("unable to resolve method", pos, COL_MESSAGE);
            }
            pos = TM3.sNewLine(pos);
            ebp = MAGIC.rMem32(ebp);
            eip = MAGIC.rMem32(ebp + 4);
        } while (ebp <= MemoryLayout.PROGRAM_STACK_COMPILER_TOP && ebp > 0 && TM3.Line(pos) < TM3.LINE_COUNT);
        return pos;
    }
}
