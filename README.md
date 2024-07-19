# Jaxon
Jaxon is a Software Development Kit built on top of SJC.

## What Does Jaxon Do?
+ Jaxon compiles Java 1.4-1.5* compliant syntax to native.
  + x86 Executables for Windows & Linux
  + x86 Bootloader for Custom Operating Systems
  + ARM 7 for embedded devices

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
+ Strings cannot be concat'd through +, you'll need to use a String Builder and chain your debug calls.

## Notes
+ *Note about Java 1.4-1.5 - Certain syntax of 1.5 is added but certain features are missing such as enums.
+ Java source code is compiled to native, there is no bytecode step.
+ Cross-platform is still in it's very early stages.
+ Built on top of SJC.