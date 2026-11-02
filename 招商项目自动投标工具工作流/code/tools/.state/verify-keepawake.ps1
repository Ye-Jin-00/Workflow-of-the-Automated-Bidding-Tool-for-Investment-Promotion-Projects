param([int]$Minutes = 9)

$ErrorActionPreference = 'Stop'
$StateDir = $PSScriptRoot
$ReportPath = Join-Path $StateDir 'verify-report.txt'

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class DshVerifyIdle
{
    [StructLayout(LayoutKind.Sequential)]
    private struct LASTINPUTINFO { public uint cbSize; public uint dwTime; }

    [DllImport("user32.dll")]
    private static extern bool GetLastInputInfo(ref LASTINPUTINFO p);

    public static uint Milliseconds()
    {
        LASTINPUTINFO l = new LASTINPUTINFO();
        l.cbSize = (uint)Marshal.SizeOf(typeof(LASTINPUTINFO));
        if (!GetLastInputInfo(ref l)) { return 0; }
        return unchecked((uint)Environment.TickCount - l.dwTime);
    }
}
'@

$start    = Get-Date
$deadline = $start.AddMinutes($Minutes)
$maxIdle  = 0
$lines    = New-Object System.Collections.Generic.List[string]
$lines.Add(('verifier start: {0:yyyy-MM-dd HH:mm:ss}' -f $start))

while ((Get-Date) -lt $deadline) {
    $idle = [int]([DshVerifyIdle]::Milliseconds() / 1000)
    if ($idle -gt $maxIdle) { $maxIdle = $idle }
    $line = ('  {0:HH:mm:ss} idle={1}s maxIdle={2}s' -f (Get-Date), $idle, $maxIdle)
    $lines.Add($line)
    Write-Output $line
    if ($maxIdle -ge 420) { break }
    Start-Sleep -Seconds 15
}

$end = Get-Date
$newStandby = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; Id = 506; StartTime = $start } -ErrorAction SilentlyContinue)

if ($maxIdle -lt 330) {
    $verdict = 'INCONCLUSIVE'
    $reason  = ('观测窗口内用户最长只空闲 {0} 秒，不足以触发 300 秒待机阈值，无法判定。' -f $maxIdle)
}
elseif ($newStandby.Count -eq 0) {
    $verdict = 'PASS'
    $reason  = ('连续空闲达 {0} 秒（远超 300 秒阈值），期间没有出现 Kernel-Power 506 进入待机事件。' -f $maxIdle)
}
else {
    $verdict = 'FAIL'
    $reason  = ('连续空闲达 {0} 秒，但出现了 {1} 次 Kernel-Power 506 进入待机事件。' -f $maxIdle, $newStandby.Count)
}

$lines.Add('')
$lines.Add(('verifier end: {0:yyyy-MM-dd HH:mm:ss}' -f $end))
$lines.Add(('maxIdle: {0}s' -f $maxIdle))
$lines.Add(('new 506 events: {0}' -f $newStandby.Count))
foreach ($e in $newStandby) { $lines.Add(('  506 at {0:yyyy-MM-dd HH:mm:ss}' -f $e.TimeCreated)) }
$lines.Add(('VERDICT: {0}' -f $verdict))
$lines.Add(('REASON: {0}' -f $reason))

$lines | Out-File -LiteralPath $ReportPath -Encoding UTF8
$lines | ForEach-Object { Write-Output $_ }
