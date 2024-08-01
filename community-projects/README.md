# Jaxon Community Projects

---

This is a mono repo containing Jaxon / SJC projects that have been contributed by several members of the Jaxon community. It's meant to act as a single location that contains the modern Jaxon build system.

Whether you're a seasoned SJC developer or just getting started, this repository offers a wealth of resources to help you learn. From build scripts to custom operating-systems, you'll find a wide variety of projects to explore and contribute to.

You'll find all the community projects listed here. To install them, use the information provided for each entry. For exact versioning information, visit the [package.list](https://github.com/Konloch/Jaxon/blob/master/community-projects/package.list).

---

## Applications
+ [Console Template](application/demo-console/) - Cross-platform template for printing to console. *(Compiles to Win32 Executable & Linux)*
    + Install with `jaxon package console`
    + **Note** This template contains the latest in-development runtime library.
    + This version is considered the most 'cutting edge' and will contain the most complete runtime API.
+ [Graphical Template](application/demo-graphical/) - Cross-platform template for windowed graphical applications. *(Compiles to Win32 Executable & Linux)*
    + Install with `jaxon package graphical`
    + This contains a proof of concept for shared windowing between Linux and Windows.
    + The runtime is outdated and is planned to be updated with a windowing API.
+ [Barebones Demo](application/demo-barebones/) - A cross-platform barebones version of hello world. *(Compiles to Win32 Executable & Linux)*
    + Install with `jaxon package barebones`
    + This contains a very minified runtime which requires the developer to implement the expected features.
+ [Fake Javac](application/fake-javac/) - A tiny program that pretends to be javac 1.8.
    + Install with `jaxon package fake-javac`
    + Used in the Jaxon Blank SDK.

---

## Operating-Systems
+ **Note** To run these operating systems, use QEMU - example scripts are included to get you started!
+ [Operating System Template](operating-system/picos/) - An operating system written in Java
    + Install with `jaxon package operating-system`
+ [Operating System Hello World Template](operating-system/hello-world/) - A slimmed down operating system that says hello world
    + Install with `jaxon package operating-system-hello-world`
+ [PicOS](operating-system/picos/) - The original OS written with SJC
    + Install with `jaxon package picos`
+ [Bearded Robot](operating-system/bearded-robot/) - An operating system written in Java
    + Install with `jaxon package bearded-robot`
	+ *Currently has a bug on boot, this is planned to be fixed*
+ [Self-Made-OS](operating-system/self-made-os/) - An operating system written in Java
    + Install with `jaxon package self-made-os`
+ [Winux](operating-system/winux/) - An operating system written in Java
    + Install with `jaxon package winux`
+ [Helix-OS](operating-system/helix-os/) - An operating system written in Java
    + Install with `jaxon package helix-os`

---

## Embedded
+ [ATmega](embedded/atmega/) - A demo controlling an ATmega board
    + Install with `jaxon package atmega`
	+ *This is currently not compiling, this is planned to be fixed*

---

## Links
+ [Jaxon](https://konloch.com/jaxon)
+ [SJC Home Page](https://www.fam-frenz.de/stefan/compiler.html)
+ [SJC Manual English](https://www.fam-frenz.de/stefan/man042_0182eng.pdf)
+ [SJC Manual German](https://www.fam-frenz.de/stefan/man046_0190.pdf)