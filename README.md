# Jaxon
Jaxon is a "with the batteries" SDK built on top of SJC - a compiler that compiles a tailored subset of Java directly to native code, eliminating the need for bytecode and virtual machines.
+ [Click here for a quick preview of some example projects](https://github.com/Konloch/Jaxon/tree/master/community-projects)

---

## What Does It Do?
+ Compiles Java 1.6 source to multiple native architectures.
+ Code templates for [Cross-platform Win32 / X11 Linux Applications](https://github.com/Konloch/Jaxon/tree/master/community-projects/application/demo-graphical), and even multiple [operating system templates](https://github.com/Konloch/Jaxon/tree/master/community-projects/operating-system).
+ Install to System-Path / Uninstall.
+ Deploys Intellij compatible JDK to enable JetBrains tooling to work.

---

## Jaxon Quick Start Guide
To help get new users started, use the following guide to get yourself setup.

### Jaxon Installation
[First start by downloading Jaxon for your platform](https://github.com/Konloch/Jaxon/releases/latest)
+ Run the command `jaxon install`
  + If you're not on windows you'll have to **add Jaxon to your System-Path manually**
### Jaxon New Project
Start an initial blank project based off the console package
+ Run the command `jaxon init`
  + If you have a project name in mind, use `jaxon init [project-name]`
+ This will create a new project, following the on-screen directions open that directory in your Java IDE of choice.
### Jaxon Package Manager
Community projects are accessible through Jaxon package manager
+ Run the command `jaxon package [name] [version*] [project-name*]` - * *denotes optional command*
+ Example commands: `jaxon package console`, `jaxon package console 0.1.0`, `jaxon package console 0.1.0 project-name`
  + For a [full list of packages click here](https://github.com/Konloch/Jaxon/tree/master/community-projects)
+ Use the build scripts to build binaries.
### Jaxon JetBrains Tooling (Intellij)
At some point you're going to want to get Intellij working.
+ If you've already ran `jaxon install`
  + Your JDK will be in {user}/.jaxon/JDK
+ If you haven't done that, just run the following command
  + `jaxon jdk jdk-1.8`
+ Then set the JDK folder as the SDK in Intellij
    + F4 for Module Settings
    + Project > SDK > Edit
    + Click the Plus > Add JDK...
    + Paste in the path sent from the Jaxon CLI

---

## Jaxon Packages
+ For a full list of packages, visit the [Community Projects](https://github.com/Konloch/Jaxon/tree/master/community-projects)
+ The packages contain all the basics to get you running (`Console` shows hello world, `Graphical` shows windowing & image drawing)
+ Each package can contain a different application starting class
    + You can find this class by going into Windows/kernel/Kernel.java & Linux/kernel/Kernel.java
+ The packages contain the entire standard library, edit these as much as you want
    + One benefit is you are not restricted by the concept of precompiled libraries, you are always provided the source unless you are invoking a DLL
+ Packages also contain a versioning system, using the [package.list](https://raw.githubusercontent.com/Konloch/Jaxon/master/community-projects/package.list) you can view all the published versions of a template
  + Using a package with a specific version: `jaxon package console 0.1.0`
  
---

## Jaxon Command Line
+ `init` - Create a blank project with the latest runtime.
  + Example Command: `jaxon init project-name`
    + `jaxon init` - *Name is optional, it will just use 'console' by default*
+ `package` - Create a new package template in the current directory
  + Example Command: `jaxon package console`
+ `build` - Build using a specific profile
  + Available Profiles: (`win-exe`, `win-app`, `lin`, `atmega`, `os-32`, `os-64`)
  + Example Command: `jaxon build win-exe src/shared/src src/windows/src`
+ `install` - Install Jaxon to the computer
  + Example Command: `jaxon install`
  + Copy the Jaxon binary into {user}/.jaxon/binaries
  + Add Jaxon to the System-Path
  + Copy the Jaxon JDK into {user}/.jaxon/JDK
+ `uninstall` - Uninstall Jaxon from the computer
  + Delete the {user}/.jaxon/bin/ folder
  + Remove all traces of Jaxon from the System-Path
+ `jdk` - Create a Jaxon-Blank-SDK that will resolve all issues with Jaxon projects.
  + Example Command: `jaxon jdk jdk-1.8`
  * **Load the SDK Into Intellij:**
    + F4 for Module Settings
      + Project > SDK > Edit
      + Click the Plus > Add JDK...
      + Paste in the path sent from the Jaxon CLI
+ `system-path` - Manage the system path by adding or removing jaxon to it.
  + Add Jaxon to System-Path: `jaxon system-path add`
  + Remove Jaxon from System-Path: `jaxon system-path remove`
+ `sjc` - Access underlying SJC command-line

---

## How Does Jaxon Work
+ In an effort to make SJC easier to learn and write:
    + Jaxon acts as a wrapper for the compiler.
    + It also provides templates & packages to aid creating new applications.
+ Since SJC has no concept of a standard library:
    + Jaxon is meant to fix that by providing packages that contain a maintained runtime.

---

## Jaxon vs SJC Differences
+ Jaxon is built on top of SJC and requires it to do anything
+ Jaxon acts as a package manager for the community projects repository
+ Jaxon provides built-in packages - allowing you to easily start new projects
    + `jaxon package console`
+ Jaxon projects use Maven to separate the modules
+ Jaxon provides a tool to create a Jaxon-Blank-SDK to make all Jaxon templates compatible with JetBrains tooling (Intellij)
+ Jaxon provides a build wrapper on top of the SJC build system
    + `jaxon build win-exe` is the equivalent of `sjc sc -s 1m -a 4198912 -l -o boot -O #win`

---

## What Does SJC Do?
+ SJC compiles a subset of Java 1.6* compliant syntax to:
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

---

## Java Differences
+ Source code only - compiles directly to native instead of bytecode
    + No virtual machines either
    + Libraries are only in the form of source code (Unless a DLL)
+ No enums
+ Generics do not exist
    + Collections, lists & maps do not exist
+ Strings cannot be concat'd using the plus operator `+`
    + **Instead, use StringBuilder**

---

## Special Classes
+ Kernel: The main class for each platform.
+ MAGIC: Code-generating access to the hardware.
    + For example access to the RAM using MAGIC.rMem8(addr) or MAGIC.wMem32(addr, value).
+ STRUCT: Parent class for custom classes that allow access to structured RAM areas without objects.
+ FLASH: Keep instances in flash (ie: do not copy to RAM).
+ @SJC: Instructions for the compiler to perform special treatment of the current method or the subsequent code.
    + For example marking the current method as an interrupt handler using @SJC.Interrupt or marking static final arrays as immutable and thus to be kept in flash using @SJC.Flash.

---

### Jaxon Community Projects
+ Jaxon packages are hosted in the [Community Projects](https://github.com/Konloch/Jaxon/tree/master/community-projects) area of the repo.

---

## Requirements
+ Jaxon runs out of the box without any requirements.
  + **Just make sure to install Jaxon via** `jaxon install` and you'll be good to go for full development.
+ Jaxon runs on **Windows, MacOS, Linux** & has a Java Jar variant if those binaries aren't compatible.

---

## Notes
+ *Note about Java 1.6 - Certain features of 1.4-1.6 are added.
    + Enhanced for-loops is one example.
+ **Fully compatible with Intellij**

---

## Links
+ [Jaxon Community Projects](https://github.com/Konloch/Jaxon/tree/master/community-projects)
+ [SJC Home Page](https://www.fam-frenz.de/stefan/compiler.html)
+ [SJC Manual English](https://www.fam-frenz.de/stefan/man042_0182eng.pdf)
+ [SJC Manual German](https://www.fam-frenz.de/stefan/man046_0190.pdf)