; =====================================================================
; CK_PB1 Setup - Windows installer (NSIS / MUI2)
; Output: dist\CK_PB1_Setup.exe
;
; Features:
;   - install path selection
;   - desktop + start menu shortcuts
;   - bundled Java 17 runtime (optional component)
;   - upgrade over previous versions (registry-detected install dir)
;   - dependency check (Java 17+ when bundled runtime is not selected)
;   - full uninstaller (keeps user data)
; =====================================================================

Unicode true
!include "MUI2.nsh"
!include "LogicLib.nsh"

!define APP_NAME "CK_PB1"
!define APP_VERSION "1.0.0"
!define APP_PUBLISHER "CK_PB1"
!define APP_URL "https://github.com/amingangmanatgh2-hash/CK_PB1"
!define APP_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\CK_PB1"
!define APP_REG_KEY "Software\CK_PB1"

Name "${APP_NAME} ${APP_VERSION}"
OutFile "../dist/CK_PB1_Setup.exe"
InstallDir "$PROGRAMFILES64\CK_PB1"
InstallDirRegKey HKLM "${APP_REG_KEY}" "InstallDir"
RequestExecutionLevel admin
SetCompressor /SOLID lzma
Icon "appicon.ico"
UninstallIcon "appicon.ico"

Var BundledJreSelected

; ---------------------------------------------------------------- MUI2 pages
!define MUI_ICON "appicon.ico"
!define MUI_UNICON "appicon.ico"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\CK_PB1.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Run CK_PB1 Launcher"
!define MUI_FINISHPAGE_LINK "GitHub - ${APP_NAME}"
!define MUI_FINISHPAGE_LINK_LOCATION "${APP_URL}"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "../LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
; dependency check runs right before the files are installed
!define MUI_PAGE_CUSTOMFUNCTION_PRE CheckDependencies
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

; ---------------------------------------------------------------- init
Function .onInit
  StrCpy $BundledJreSelected "1"
  ReadRegStr $0 HKLM "${APP_REG_KEY}" "InstallDir"
  ${If} $0 != ""
    ; upgrade: keep the previous install directory
    StrCpy $INSTDIR $0
  ${EndIf}
FunctionEnd

; ---------------------------------------------------------------- sections
; (CheckDependencies is defined after the sections because it uses ${SEC_*})

Section "!CK_PB1 Launcher (required)" SEC_CORE
  SectionIn RO
  SetOutPath "$INSTDIR"

  ; stop a running launcher before upgrading
  nsExec::ExecToLog 'taskkill /IM CK_PB1.exe /F'
  Sleep 500

  File "build/CK_PB1.exe"
  File "../launcher/build/libs/CK_PB1-Launcher-${APP_VERSION}.jar"
  File "../client/build/libs/CK_PB1-Client-${APP_VERSION}+mc1.20.1.jar"
  File "../README.md"
  File "../CHANGELOG.md"
  File "../LICENSE"

  ; default configs shipped with the installer
  SetOutPath "$INSTDIR\configs"
  File /nonfatal "../configs/default/*.json"

  ; write uninstaller + registry
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "${APP_REG_KEY}" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "${APP_REG_KEY}" "Version" "${APP_VERSION}"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "DisplayName" "${APP_NAME}"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "DisplayIcon" "$INSTDIR\CK_PB1.exe"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "Publisher" "${APP_PUBLISHER}"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "URLInfoAbout" "${APP_URL}"
  WriteRegStr HKLM "${APP_UNINST_KEY}" "UninstallString" "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Bundled Java 17 Runtime (recommended)" SEC_JRE
  SetOutPath "$INSTDIR"
  File /r "build/jre"
  StrCpy $BundledJreSelected "1"
SectionEnd

Section "Desktop Shortcut" SEC_DESKTOP
  CreateShortCut "$DESKTOP\CK_PB1.lnk" "$INSTDIR\CK_PB1.exe" "" "$INSTDIR\CK_PB1.exe" 0
SectionEnd

Section "Start Menu Shortcut" SEC_STARTMENU
  CreateDirectory "$SMPROGRAMS\CK_PB1"
  CreateShortCut "$SMPROGRAMS\CK_PB1\CK_PB1.lnk" "$INSTDIR\CK_PB1.exe" "" "$INSTDIR\CK_PB1.exe" 0
  CreateShortCut "$SMPROGRAMS\CK_PB1\Changelog.lnk" "$INSTDIR\CHANGELOG.md" "" "" 0
  CreateShortCut "$SMPROGRAMS\CK_PB1\Uninstall CK_PB1.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
SectionEnd

; track whether the user deselected the bundled runtime
Function .onSelChange
  SectionGetFlags ${SEC_JRE} $0
  IntOp $0 $0 & 1
  ${If} $0 == 0
    StrCpy $BundledJreSelected "0"
  ${Else}
    StrCpy $BundledJreSelected "1"
  ${EndIf}
FunctionEnd

; ---------------------------------------------------------------- dependency check
Function CheckDependencies
  ${If} $BundledJreSelected == "0"
    nsExec::ExecToStack 'java -version'
    Pop $0
    Pop $1
    ${If} $0 != 0
      MessageBox MB_YESNO|MB_ICONEXCLAMATION \
        "Java was not found on this system.$\r$\n\
         CK_PB1 requires Java 17 or newer.$\r$\n$\r$\n\
         Click YES to install the bundled Java 17 runtime instead, \
         or NO to continue anyway." \
        IDYES reinstalljre IDNO depdone
    ${EndIf}
  ${EndIf}
  Goto depdone
reinstalljre:
  SectionGetFlags ${SEC_JRE} $1
  IntOp $1 $1 | 1
  SectionSetFlags ${SEC_JRE} $1
depdone:
FunctionEnd

LangString DESC_CORE ${LANG_ENGLISH} "CK_PB1 Launcher, client mod and documentation."
LangString DESC_JRE ${LANG_ENGLISH} "A private Java 17 runtime used only by CK_PB1. No system Java required."
LangString DESC_DESKTOP ${LANG_ENGLISH} "Create a CK_PB1 shortcut on the desktop."
LangString DESC_STARTMENU ${LANG_ENGLISH} "Create a CK_PB1 folder in the Start Menu."

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_CORE} $(DESC_CORE)
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_JRE} $(DESC_JRE)
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DESKTOP} $(DESC_DESKTOP)
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_STARTMENU} $(DESC_STARTMENU)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

; ---------------------------------------------------------------- uninstall
Section "Uninstall"
  ; stop the launcher if it is running
  nsExec::ExecToLog 'taskkill /IM CK_PB1.exe /F'

  ; remove app files (all CK_PB1 jars, also from older versions)
  Delete "$INSTDIR\CK_PB1.exe"
  Delete "$INSTDIR\CK_PB1-Launcher-*.jar"
  Delete "$INSTDIR\CK_PB1-Client-*.jar"
  Delete "$INSTDIR\README.md"
  Delete "$INSTDIR\CHANGELOG.md"
  Delete "$INSTDIR\LICENSE"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR\jre"
  RMDir /r "$INSTDIR\configs"
  RMDir "$INSTDIR"

  ; shortcuts
  Delete "$DESKTOP\CK_PB1.lnk"
  RMDir /r "$SMPROGRAMS\CK_PB1"

  ; registry
  DeleteRegKey HKLM "${APP_UNINST_KEY}"
  DeleteRegKey HKLM "${APP_REG_KEY}"

  ; user data (launcher settings) is kept unless the user confirms removal
  ${If} ${FileExists} "$APPDATA\CK_PB1"
    MessageBox MB_YESNO|MB_ICONQUESTION \
      "Delete CK_PB1 launcher settings in %APPDATA%\CK_PB1?$\r$\n\
       (Game directories are never touched)" \
      IDNO +2
    RMDir /r "$APPDATA\CK_PB1"
  ${EndIf}
SectionEnd
