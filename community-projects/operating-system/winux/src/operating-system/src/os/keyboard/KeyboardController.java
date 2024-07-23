package os.keyboard;

import os.keyboard.layout.QWERTY;

/**
 * Check http://www.lowlevel.eu/wiki/Keyboard_Controller for detailed explanations and graphics
 */
public class KeyboardController {

    private static KeyBuffer keyBuffer;
    static {
       keyBuffer = new KeyBuffer();
    }

    private static boolean capsLock = false;
    private static boolean numLock = false;
    private static boolean scrollLock = false;

    private static boolean shift = false;
    private static boolean ctrl = false;
    private static boolean alt = false;
    private static boolean superKey = false;


    // status variable to handle e0- and e1-scancodes
    private static boolean e0_code = false;
    // set to 1 when e1 is read
    // set to 2 when first byte is read
    private static int e1_code = 0;
    private static int e1_prev = 0;

    public static void processIOBuffer() {
        int scancode;
        int keyCode = 0;
        boolean isBreakCode = false;

        scancode = readIOBuffer();

        // it's a breakCode if
        // - highest bit is set
        // - there is no e0 or e1 for extended scancode
        if(
                ((scancode & 0x80) != 0) //&&
//                ((e1_code != 0) || (scancode != 0xE1)) &&
//                (e0_code || (scancode != 0xE0))
        ) {
            isBreakCode = true;
            scancode &= ~0x80;
        }

        int mappedKey = 0;
        if(e0_code) {
            // catch fake shift and ignore it
            if((scancode == 0x2A) || (scancode == 0x36)) {
                e0_code = false;
                return;
            }

            mappedKey = QWERTY.mapKey(1, scancode, capsLock || shift);
            e0_code = false;

        } else if(e1_code == 1) {
            // e1 almost done
            e1_prev = scancode;
            e1_code++;

        } else if(e1_code == 2) {
            // e1 done
            // put second scancode in upper byte
            e1_prev |= scancode << 8;
            mappedKey = QWERTY.mapKey(2, e1_prev, capsLock || shift);
            e1_code = 0;

        } else if(scancode == 0xE0) {
            // beginning of e0
            e0_code = true;

        } else if(scancode == 0xE1) {
            // beginning of e1
            e1_code = 1;

        } else {
            // normal scancode
            keyCode = QWERTY.mapKey(0, scancode & 0xFF, capsLock || shift);
        }


        switch(keyCode) {
            case ASCII.SHIFT_LEFT:
            case ASCII.SHIFT_RIGHT:
                shift = !isBreakCode;
                break;

            case ASCII.CTRL_LEFT:
            case ASCII.CTRL_RIGHT:
                ctrl = !isBreakCode;
                break;

            case ASCII.ALT_LEFT:
            case ASCII.ALT_RIGHT:
                alt = !isBreakCode;
                break;

            case ASCII.SUPER_LEFT:
            case ASCII.SUPER_RIGHT:
                superKey = !isBreakCode;
                break;

            case ASCII.CAPSLOCK:
                if(!isBreakCode)
                    capsLock = !capsLock;
                break;

            case ASCII.NUMLOCK:
                if(!isBreakCode)
                    numLock = !numLock;
                break;

            case ASCII.SCROLLLOCK:
                if(!isBreakCode)
                    scrollLock = !scrollLock;
                break;

            default:
                if(!isBreakCode) {
                    if(keyCode == 'c' && !alt && ctrl)
                        MAGIC.inline(0xCC);
                    keyBuffer.add(new KeyEvent(keyCode, alt, ctrl, superKey));

                }
        }

        // keyBuffer.add(new KeyEvent(keyCode, false, false));
    }

    public static KeyBuffer getKeyBuffer() {
        return keyBuffer;
    }

    /**
     * Read keyboard commands at 0x60.
     *
     * @return Byte at IO Buffer 0x60
     */
    @SJC.Inline
    public static byte readIOBuffer() {
        return MAGIC.rIOs8(0x60);
    }

    /**
     *
     * Output buffer must be empty 0x64.Bit[1] == 0, @see {@link KeyboardController#readStatusRegister()}
     *
     * 0xED   Set LED on keyboard. A second byte is sent to port 0x60 (this one):
     *
     *        Bit 76543210
     *            │││││││└─ Scroll Lock : 0=off 1=on
     *            ││││││└── Num Lock    : 0=off 1=on
     *            │││││└─── Caps Lock   : 0=off 1=on
     *            └┴┴┴┴──── 0
     *
     * 0xEE   Test keyboard. Should respond with 0xEE.
     *
     * 0xF0   Choose scan code. Parameter 1, 2 (default) or 3 can be written to 0x60.
     *        With parameter 0 the current scan code can be read at port 0x60.
     *
     * 0xF2   Identify keyboard. Returns one of the following:
     *
     *        XT-keyboard    :  Timeout (has to be measured manually unfortunately)
     *        AT-keyboard    :  Returns 0xFA
     *        MF-II-keyboard :  Returns 0xFA 0xAB 0x41
     *
     * 0xF3   Set repetition rate of key pressed. A second byte is written to 0x60:
     *
     *        Bit 76543210
     *            │││└┴┴┴┴─ Value : Repetitions / second
     *            │││       00000 : 30
     *            │││       00001 : 26.7
     *            │││       00010 : 24
     *            │││       00100 : 20
     *            │││       01000 : 15
     *            │││       01010 : 10
     *            │││       01101 :  9
     *            │││       10000 :  7.5
     *            │││       10100 :  5
     *            │││       11111 :  2
     *            │││
     *            │└┴────── Waiting time before repetition starts
     *            │         00 :  250 ms
     *            │         01 :  500 ms
     *            │         10 :  750 ms
     *            │         11 : 1000 ms
     *            │
     *            └──────── 0
     *
     * 0xF4   Activate keyboard. If there was a transmission error, the keyboard can be
     *        reactivated. The internal buffer is deleted in the process.
     *
     * 0xF5   Deactivate keyboard and set values back to default.
     *        Delete buffer, turn off LEDs, repetition rate and waiting time reset, deactivate keyboard
     *        (keystrokes are not transmitted)
     *
     * 0xF6   Reset values to defaults
     *
     * 0xFE   Sent internally from KBC to keyboard. Not intended to be used by the CPU.
     *
     * 0xFF   Keyboard reset and health check. Returns 0xAA if successful, else 0xFC.
     *
     * @param b Byte to write
     */
    @SJC.Inline
    public static void writeIOBuffer(byte b) {
        // make sure input buffer is empty
        while((readStatusRegister() & 0x2) != 0);
        MAGIC.wIOs8(0x60, b);
    }

    /**
     * Send KBC-command depending on the status register 0x64
     *
     * Bit 76543210
     *     │││││││└─ Output buffer status (KBC -> CPU): 0=empty; 1=full (read at port 0x60)
     *     ││││││└── Input buffer status (KBC <- CPU): 0=empty (write on 0x60 or 0x64 possible); 1=full
     *     │││││└─── 1=Success (should always be 1)
     *     ││││└──── last used port: 0=0x60; 1=0x61 or 0x64
     *     │││└───── Keyboard lock: 0=Keyboard locked; 1=Keyboard not locked
     *     ││└────── PSAUX?
     *     │└─────── Timeout: 1=Keyboard or PSAUX-device not responding
     *     └──────── Priority error: 1=The last byte sent / received caused a priority error
     *
     * @return Byte at status register 0x64
     */
    @SJC.Inline
    public static byte readStatusRegister() {
        return MAGIC.rIOs8(0x64);
    }

    /**
     * Write byte to status register 0x64.
     *
     * Output buffer must be empty (Bit [1] == 0) and keyboard must not be in reset mode (Bit [2] == 1)
     * (@see {@link KeyboardController#readStatusRegister()}).
     *
     * @param b Byte command, see above
     */
    @SJC.Inline
    public static void writeStatusRegister(byte b) {
        MAGIC.wIOs8(0x64, b);
    }

    public void init() {
        // empty input buffer
        while((readStatusRegister() & 0x1) != 0) {
            readIOBuffer();
        }
    }
}
