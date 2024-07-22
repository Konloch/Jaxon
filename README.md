# Jaxon
Jaxon is a "with the batteries" SDK built on top of SJC - a compiler that compiles a tailored subset of Java directly to native code, eliminating the need for bytecode and virtual machines.

## Getting Started
+ [First start by downloading Jaxon]()
  + Add Jaxon to your System-Path
+ Then start by creating a new project, this is done through code templates. (such as `jaxon template console`)
  + `Console` - Cross-platform minified template for printing to console.
    + **Note** This template contains the in-development runtime library.
    + This version is considered the most 'cutting edge' and will contain the most complete runtime API.
    + The graphical template is currently paused until the runtime has been ironed out more.
+ Once you've decided, create a new template by running `jaxon template console` such as `jaxon template console`
  + This for example will create the cross-platform template for a console program.
  + Use the build scripts to build binaries & read them to write your own.
  + At some point you're going to want to get Intellij working, to do that just run the following command
    + `jaxon jdk jdk-1.8`
    + Then set it as the SDK loaded from folder 'jdk-1.8' in Intellij.
      + F4 for Module Settings
      + Project > SDK > Edit
      + Click the Plus > Add JDK...
      + Paste in the path sent from the Jaxon CLI

## Using Jaxon Templates
+ The templates all contain the basics to get you running (`Console` shows hello world, `Graphical` shows windowing & image drawing)
+ Rely heavily on the provided standard library and if there isn't a function or class that you regularly use, open a GitHub issue.
+ Each template can contain a different application starting class
  + You can find this class by going into Windows/kernel/Kernel.java & Linux/kernel/Kernel.java
+ The templates contain the entire standard library, edit these as much as you want
  + One benefit is you are not restricted by the concept of precompiled libraries, you are always provided the source unless you are invoking a DLL

## Jaxon Templates
   + `Console` - Cross-platform template for printing to console.
        + **Note** This template contains the latest in-development runtime library.
        + This version is considered the most 'cutting edge' and will contain the most complete runtime library.
        + The graphical template is currently paused until the runtime has been ironed out more.
   + `Graphical` - Cross-platform template for windowed graphical applications.
        + This contains an early proof of concept for shared windowing between Linux and Windows.
        + The runtime is outdated and won't be updated until we support windowing on the runtime officially.
   + `Barebones` - Cross-platform minified template for printing to console.
     + This is added if you want to roll your own runtime library.
     + This lacks the entire Runtime and only contains the bare template to compile.
   + `ATmega` - ARM7 ATmega embedded controller template.
        + **Note** This template is currently having build issues.
   + `Operating-System` - 32bit / 64bit template for operating system.
        + This runtime is also outdated and won't be updated for quite some time.
            + A modified and minified version will be added as we won't need to support the concept of cross-platform runtimes.

## Jaxon Command Line
+ `template` - Create a new template in the current directory
  + Available Templates: (`Console`, `Graphical`, `ATmega`, `Operating-System`)
  + Example Command: `jaxon template console`
+ `build` - Build using a specific profile
  + Available Profiles: (`win-exe`, `win-app`, `lin`, `atmega`, `os-32`, `os-64`)
  + Example Command: `jaxon build win-exe src/shared/src src/windows/src`
+ `jdk` - Create a Jaxon-Blank-SDK that will resolve all issues with Jaxon template projects.
  + Example Command: `jaxon jdk jdk-1.8`
  + Example Usage: **Load the SDK Into Intellij:**
      + F4 for Module Settings
      + Project > SDK > Edit
      + Click the Plus > Add JDK...
      + Paste in the path sent from the Jaxon CLI
+ `sjc` - Access underlying SJC command-line
  + If you end up needing to use this, open an issue on Github letting us know what command you used (We'll add it in the next update)
+ `zip` - Zip util for packaging Jaxon templates

## Java Differences
+ Source code only - compiles directly to native instead of bytecode
  + No virtual machines either
  + Libraries are only in the form of source code (Unless a DLL)
+ No enums
+ Generics do not exist 
  + Collections, lists & maps do not exist
+ Strings cannot be concat'd using the plus operator `+`
  + **Instead, use StringBuilder**

## How Does Jaxon Work
+ In an effort to make SJC easier to learn and write:
    + Jaxon acts as a wrapper for the compiler.
    + It also provides templates to aid creating new applications.
+ Since SJC has no concept of a standard library:
    + Jaxon is meant to fix that by providing standard templates that contain a maintained runtime.

## Jaxon vs SJC Differences
+ Jaxon is built on top of SJC and requires it to do anything
+ Jaxon provides built-in templates - allowing you to easily start new projects
    + `jaxon template console`
+ Jaxon templates use Maven to separate the modules
+ Jaxon provides a tool to create a Jaxon-Blank-SDK to make all Jaxon templates fully Intellij compatible
+ Jaxon provides a build wrapper on top of the SJC build system
    + `jaxon build win-exe` is the equivalent of `sjc sc -s 1m -a 4198912 -l -o boot -O #win`

## What Does SJC Do?
+ SJC compiles a subset of Java 1.4-1.5* compliant syntax to:
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

## Special Classes
+ Kernel: The main class for each platform.
+ MAGIC: Code-generating access to the hardware.
    + For example access to the RAM using MAGIC.rMem8(addr) or MAGIC.wMem32(addr, value).
+ STRUCT: Parent class for custom classes that allow access to structured RAM areas without objects.
+ FLASH: Keep instances in flash (ie: do not copy to RAM).
+ @SJC: Instructions for the compiler to perform special treatment of the current method or the subsequent code.
    + For example marking the current method as an interrupt handler using @SJC.Interrupt or marking static final arrays as immutable and thus to be kept in flash using @SJC.Flash.

## Notes
+ *Note about Java 1.4-1.5 - Certain features of 1.5 are added.
  + Enhanced for-loops is one example.
+ Built on top of SJC.
  + You can access SJC command-line by using `jaxon sjc [sjc commands goes here]`
+ **Fully compatible with Intellij, just select "NO SDK" and you'll have no errors with full modern tooling.**