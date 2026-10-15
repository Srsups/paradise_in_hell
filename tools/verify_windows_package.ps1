param([string]$Archive)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $Archive) {
    $Archive = Join-Path $projectRoot 'lwjgl3\build\construo\dist\paradise-in-hell-gdx-winX64.zip'
}
$destination = Join-Path $projectRoot 'build\package-smoke'
New-Item -ItemType Directory -Path $destination -Force | Out-Null
Expand-Archive -LiteralPath $Archive -DestinationPath $destination -Force
$executables = @(Get-ChildItem -LiteralPath $destination -Recurse -Filter 'paradise-in-hell-gdx.exe')
if ($executables.Count -ne 1) { throw 'Expected exactly one game executable.' }
$executable = $executables[0]
if (-not (Test-Path -LiteralPath (Join-Path $executable.DirectoryName 'LEIA-ME.txt'))) {
    throw 'Player instructions are missing from the package.'
}
foreach ($diagnostic in @('--smoke-test', '--audio-smoke-test')) {
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $executable.FullName
    $start.Arguments = $diagnostic
    $start.WorkingDirectory = $projectRoot
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.EnvironmentVariables.Remove('JAVA_HOME')
    $start.EnvironmentVariables['PATH'] = Join-Path $env:SystemRoot 'System32'
    # OpenAL Soft's null device decodes/mixes natively without playing through the user's speakers.
    $start.EnvironmentVariables['ALSOFT_DRIVERS'] = 'null'
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $start
    $process.Start() | Out-Null
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit(20000)) {
        $process.Kill()
        throw "Packaged game did not finish $diagnostic within 20 seconds."
    }
    $output = $stdout.Result + $stderr.Result
    if ($process.ExitCode -ne 0) { throw "Packaged game failed ($diagnostic): $output" }
    $process.Dispose()
}
Write-Output 'WINDOWS_PACKAGE_OK: bundled Java, gameplay/menu/report/audio assets, 24 native effects, streaming music via OpenAL null device, external working directory'
