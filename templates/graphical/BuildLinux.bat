@echo off
setlocal EnableDelayedExpansion
set "args="

for /f "delims=" %%f in ('dir /b /s /a:-d "src\shared\src"') do (
    set "args=!args! %%~f"
)

for /f "delims=" %%f in ('dir /b /s /a:-d "src\linux\src"') do (
    set "args=!args! %%~f"
)

cd ../../

xcopy /s /y sjcdk\* sjc\
cd sjc
compile -s 512k -a 1049008 -l -o boot -O #llb !args!
cd ../
mkdir build\linux
copy sjc\OUT_LIN.O build\linux\build
endlocal