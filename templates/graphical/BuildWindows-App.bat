@echo off
setlocal EnableDelayedExpansion
set "args="

for /f "delims=" %%f in ('dir /b /s /a:-d "src\shared\src"') do (
    set "args=!args! %%~f"
)

for /f "delims=" %%f in ('dir /b /s /a:-d "src\windows\src"') do (
    set "args=!args! %%~f"
)

cd ../../

xcopy /s /y sjcdk\* sjc\
cd sjc
compile -s 512k -a 4198912 -l -o boot -O #win !args!
cd ../
mkdir build\windows
copy sjc\OUT_WIN.EXE build\windows\build.exe
endlocal