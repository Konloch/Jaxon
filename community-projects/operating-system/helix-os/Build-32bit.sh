#!/bin/bash
jaxon "sjc-env" "operating-system" "BOOT_FLP.IMG" "build.img" "-t" "ia32" "-o" "raw" "-O" "BOOT_FLP.IMG" "-s" "10000M" "-T" "nsop" "-u" "rte" "-y" "-g" "-G" "-n" "src"
mkisofs -o build/operating-system/build.iso -N -b bbk_iso.bin -no-emul-boot -boot-load-seg 0x7C0 -boot-load-size 4 -V "SJCCD" -A "SJC compiled bootable OS" -graft-points CDBOOT/BOOT_ISO.IMG=build/operating-system/build.img bbk_iso.bin
del "build\operating-system\build.img"