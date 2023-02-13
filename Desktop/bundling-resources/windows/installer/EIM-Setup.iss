#define appName "EIM - Email Instant Messaging"
#define appVersion "0.13.2"
#define appPublisher "Denis Meyer"
#define appURL "https://bit.ly/emailim"
#define appExeName "EIM.exe"
#define appPublisherContact "denmeyer.eim@gmail.com"
#define appCopyright "2014 Denis Meyer"
#define srcPath "C:\Users\calltopower\Documents\Repositories\EIM\Desktop"
#define srcPathShort "\Repositories\EIM\Desktop"
#define desktopPath "C:\Users\calltopower\Desktop"
#define desktopFolderName "EIM_v0-13-2_beta_build-1_windows"
#define versionFolder "0-13-2"

[Setup]
AppId={{C1A60531-BE04-4963-A570-2CAD98B9D77E}}
AppName={#appName}
AppVersion={#appVersion}
AppPublisher={#appPublisher}
AppPublisherURL={#appURL}
AppSupportURL={#appURL}
AppUpdatesURL={#appURL}
DefaultDirName={pf}\EIM\{#versionFolder}
DefaultGroupName={#appName}
LicenseFile={#srcPath}\bundling-resources\Files\EULA
OutputDir={#desktopPath}
OutputBaseFilename={#desktopFolderName}-Setup
SetupIconFile={#srcPath}\bundling-resources\windows\eim.ico
Compression=lzma
SolidCompression=yes
RestartIfNeededByRun=False
UsePreviousAppDir=False
AppContact={#appPublisherContact}
PrivilegesRequired=admin
AppReadmeFile={#srcPath}\bundling-resources\Files\README
CloseApplicationsFilter=*.exe,*.dll,*.chm, *.bat
UninstallLogMode=overwrite
UninstallDisplayName=EIM
UninstallDisplayIcon={uninstallexe}
AppCopyright={#appCopyright}
MinVersion=0,6.1
RestartApplications=False
VersionInfoVersion={#appVersion}
VersionInfoCompany={#appPublisher}
VersionInfoCopyright={#appCopyright}
VersionInfoProductName={#appName}
VersionInfoProductVersion={#appVersion}
WizardImageFile=userdocs:{#srcPathShort}\bundling-resources\windows\installer\installer_logo_big.bmp
WizardSmallImageFile=userdocs:{#srcPathShort}\bundling-resources\windows\installer\installer_logo_small.bmp
DisableDirPage=yes
CreateUninstallRegKey=yes
AlwaysRestart=False

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "german"; MessagesFile: "compiler:Languages\German.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#desktopPath}\{#desktopFolderName}\EIM.exe"; DestDir: "{app}"; Flags: ignoreversion  
Source: "{#desktopPath}\{#desktopFolderName}\Files\*"; DestDir: "{app}\Files"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#srcPath}\bundling-resources\windows\eim.ico"; DestDir: "{app}\Files"; Flags: ignoreversion recursesubdirs createallsubdirs

[Dirs]
Name: "{app}\Files"

[Icons]
Name: "{group}\{#appName}"; Filename: "{app}\{#appExeName}"; IconFilename: "{app}\Files\eim.ico"
Name: "{group}\{cm:ProgramOnTheWeb,{#appName}}"; Filename: "{#appURL}"
Name: "{group}\{cm:UninstallProgram,{#appName}}"; Filename: "{uninstallexe}"; IconFilename: "{app}\Files\eim.ico";
Name: "{commondesktop}\{#appName}"; Filename: "{app}\{#appExeName}"; IconFilename: "{app}\Files\eim.ico"; Tasks: desktopicon;
                                                         
[Run]
Filename: icacls.exe; Parameters: """{app}"" /grant {username}:(OI)(CI)F /T /C /Q"
