# PicOS
A lightweight runtime environment for 32-bit and 64-bit programs, featuring virtual memory, interrupts, and basic device emulation.

# How To Build
+ You can build from Windows, Linux or Mac. Install [Jaxon](https://konloch.com/Jaxon) to compile.
+ **To build a 32-bit binary** run `Build-32bit`
+ **To build a 64-bit binary** run `Build-64bit`

# How To Run
+ Build file will output at `build/operating-system/build.img`
+ Run the binary using QEMU
	+ An example script has been included in `Run-OS-QEMU`

# Notes
+ Parent repository [Stefan Frenz's Repo](https://fam-frenz.de/stefan/picos.html)