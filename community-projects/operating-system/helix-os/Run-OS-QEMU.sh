#!/bin/bash
qemu-system-x86_64 -m 1024 -rtc base=localtime -no-reboot -debugcon file:build/serial.log -boot d -cdrom build/operating-system/build.iso