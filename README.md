# Jaxon
Jaxon is a Software Development Kit built on top of SJC.

## What Does Jaxon Do?
+ Jaxon compiles Java 1.4-1.5* compliant syntax to:
  + Native IA32 & x86_64
    + Executables for Windows & Linux
      + Win32 console application
      + Win32 GUI application
      + Linux 32 bit binary with library support
    + Bootloader for Custom Operating Systems
      + Native IA32 image to boot
      + Native AMD64 image to boot
  + ARM 7
    + Atmega Atmel Hexout (Controllers such as LPC2103)

## How Does Jaxon Work
+ In an effort to make SJC easier to learn and write, Jaxon acts as a wrapper for the compiler.
+ Since SJC has no concept of a standard library:
  + Jaxon is meant to fix that by providing standard templates that contain a maintained runtime.

## Getting Started
+ First start by downloading and installing Jaxon.
+ Then start by creating a new project, this is done through code templates.
  + `Console` - Cross-platform minified template for printing to console.
  + `Graphical` - Cross-platform template for windowed graphical applications.
+ Once you've decided, create a new template by running `jaxon template [your-choice]` such as `jaxon template graphical`
  + This for example will create the cross-platform template for a graphical windowed program.

## Programming Jaxon
+ The templates all contain the basics to get you running (`Console` shows hello world, `Graphical` shows windowing & image drawing)
+ Rely heavily on the provided standard library and if there isn't a function or class that you regularly use, open a GitHub issue.
+ Each template can contain a different application starting class
  + You can find this class by going into Windows/kernel/Kernel.java & Linux/kernel/Kernel.java
+ The templates contain the entire standard library, edit these as much as you want
  + One benefit is you are not restricted by the concept of precompiled libraries, you are always provided the source unless you are invoking a DLL

## Special Classes
+ Kernel: The main class for each platform
+ MAGIC: Code-generating access to the hardware
  + For example access to the RAM using MAGIC.rMem8(addr) or MAGIC.wMem32(addr, value).
+ STRUCT: Parent class for custom classes that allow access to structured RAM areas without objects.
+ FLASH: Keep instances in flash (ie: do not copy to RAM).
+ @SJC: Instructions for the compiler to perform special treatment of the current method or the subsequent code.
  + For example marking the current method as an interrupt handler using @SJC.Interrupt or marking static final arrays as immutable and thus to be kept in flash using @SJC.Flash.

## Jaxon vs Java Differences
+ Compiles directly to native instead of bytecode
+ No enums
+ Generics do not exist 
  + Collections, lists & maps do not exist
+ Strings cannot be concat'd using the plus operator `+`
  + *Instead, use StringBuilder*

## Notes
+ *Note about Java 1.4-1.5 - Certain features of 1.5 are added.
+ Cross-platform is still in it's very early stages.
+ Built on top of SJC.