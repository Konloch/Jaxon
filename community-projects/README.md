# Jaxon Community Projects
This repository is a mono repo containing Jaxon / SJC projects that have been contributed by several members of the Jaxon community. It's meant to act as a single location that contains the modern Jaxon build system.

Whether you're a seasoned SJC developer or just getting started, this repository offers a wealth of resources to help you get the most out of the compiler. From build scripts to custom operating-systems, you'll find a wide variety of projects to explore and contribute to.

Join the Jaxon community today and help us build a brighter future for SJC development!

## Applications
+ [Console Demo](application/demo-console/) - A cross platform hello world. Compiles to Win32 Executable & Linux
    + **Note** This template contains the in-development runtime library.
    + This version is considered the most 'cutting edge' and will contain the most complete runtime API.
+ [Barebones Demo](application/demo-barebones/) - A cross platform barebones version of hello world. Compiles to Win32 Executable & Linux
	+ This contains a very minified runtime which requires the developer to implement the expected features.
+ [Graphical Demo](application/demo-graphical/) - A cross platform example of windowed graphical drawing. Compiles to Win32 Executable & Linux
    + The graphical template is currently paused until the runtime has been completed more.
+ [Fake Javac](application/fake-javac/) - A tiny program that pretends to be javac 1.8.
    + Used in the Jaxon Blank SDK.

## Operating-Systems
+ **Note** To run these operating systems, use QEMU - example scripts are included to get you started!
+ [Hello World](operating-system/hello-world/) - A slimmed down hello world demo
+ [PicOS](operating-system/picos/) - The original OS written with SJC
+ [Bearded Robot](operating-system/bearded-robot/) - An operating system written in Java
	+ Currently has a bug on boot, this is planned to be fixed
+ [Self-Made-OS](operating-system/self-made-os/) - An operating system written in Java
+ [Winux](operating-system/winux/) - An operating system written in Java

## Embedded
+ [ATmega](embedded/atmega/) - A demo controlling an ATmega board
	+ This is currently not compiling, this is planned to be fixed

## Links
+ [Jaxon](https://konloch.com/jaxon)
+ [SJC Home Page](https://www.fam-frenz.de/stefan/compiler.html)
+ [SJC Manual English](https://www.fam-frenz.de/stefan/man042_0182eng.pdf)
+ [SJC Manual German](https://www.fam-frenz.de/stefan/man046_0190.pdf)