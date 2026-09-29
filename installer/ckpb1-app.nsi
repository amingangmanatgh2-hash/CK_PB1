; CK_PB1 application launcher (CK_PB1.exe)
; A tiny NSIS app that starts the CK_PB1 Launcher with the bundled (or system) Java.

Unicode true
Name "CK_PB1"
OutFile "build\CK_PB1.exe"
RequestExecutionLevel user
SetCompressor /FINAL lzma
Icon "appicon.ico"
SubCaption 3 " "
Caption "CK_PB1"

SilentInstall silent
AutoCloseWindow true

Section
  SetOutPath "$EXEDIR"
  ; prefer the bundled Java runtime
  IfFileExists "$EXEDIR\jre\bin\javaw.exe" bundled 0
  IfFileExists "$EXEDIR\jre\bin\java.exe" bundled 0
  ; fall back to system java
  Exec '"javaw" -jar "$EXEDIR\CK_PB1-Launcher.jar"'
  Goto done
bundled:
  Exec '"$EXEDIR\jre\bin\javaw.exe" -jar "$EXEDIR\CK_PB1-Launcher.jar"'
done:
SectionEnd
