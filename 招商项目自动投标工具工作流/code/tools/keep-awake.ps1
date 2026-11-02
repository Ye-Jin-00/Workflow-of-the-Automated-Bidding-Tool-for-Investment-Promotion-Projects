#Requires -Version 5.1
<#
.SYNOPSIS
    保持系统唤醒：阻止空闲待机/休眠，但允许显示器正常关闭（熄屏）。

.DESCRIPTION
    通过 kernel32!SetThreadExecutionState 申请 ES_CONTINUOUS | ES_SYSTEM_REQUIRED 电源请求。
    只要本进程（持有该请求的线程）还活着，系统就不会因为「空闲超时」进入待机
    （本机只有 S0 低电量待机 / Modern Standby，同样会被这条请求挡住）。
    显示器关闭（熄屏）不受影响，除非加 -KeepDisplayOn。

    注意：这是「空闲」层面的拦截。用户主动点「睡眠」、合盖、断电等情况不在拦截范围内。

.PARAMETER Minutes
    最长持有时长，单位分钟，可带小数。0（默认）表示一直持有，直到手动 -Stop。

.PARAMETER KeepDisplayOn
    同时阻止显示器关闭（追加 ES_DISPLAY_REQUIRED）。默认不加，熄屏照常发生。

.PARAMETER Stop
    停止正在运行的实例，释放电源请求。

.PARAMETER Status
    查看运行状态：是否在跑、当前空闲秒数、日志尾部、最近待机事件。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\keep-awake.ps1
    一直保持唤醒，直到 stop。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\keep-awake.ps1 -Minutes 120
    最多保持 120 分钟后自动退出并释放。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\keep-awake.ps1 -Status
    查看状态。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\keep-awake.ps1 -Stop
    停止并确认请求已释放。
#>
[CmdletBinding()]
param(
    [double]$Minutes = 0,
    [switch]$Stop,
    [switch]$Status,
    [switch]$KeepDisplayOn
)

$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ScriptDir)) { $ScriptDir = (Get-Location).ProviderPath }
$StateDir = Join-Path $ScriptDir '.state'
$LogPath  = Join-Path $StateDir 'keep-awake.log'
$PidPath  = Join-Path $StateDir 'keep-awake.pid'
$StopPath = Join-Path $StateDir 'keep-awake.stop'

# --- Win32 常量 -------------------------------------------------------------
$ES_CONTINUOUS       = [uint32]2147483648  # 0x80000000
$ES_SYSTEM_REQUIRED  = [uint32]1           # 0x00000001
$ES_DISPLAY_REQUIRED = [uint32]2           # 0x00000002

if (-not (Test-Path -LiteralPath $StateDir)) {
    New-Item -ItemType Directory -Path $StateDir -Force | Out-Null
}

function Write-Log {
    param([string]$Message, [string]$Level = 'INFO')
    $line = '{0} [{1}] {2}' -f (Get-Date).ToString('yyyy-MM-dd HH:mm:ss'), $Level, $Message
    Add-Content -LiteralPath $LogPath -Value $line -Encoding UTF8
    Write-Host $line
}

function Initialize-NativeTypes {
    if (-not ('DshSleepGuard' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class DshSleepGuard
{
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern uint SetThreadExecutionState(uint esFlags);
}
'@
    }
    if (-not ('DshIdleProbe' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class DshIdleProbe
{
    [StructLayout(LayoutKind.Sequential)]
    private struct LASTINPUTINFO
    {
        public uint cbSize;
        public uint dwTime;
    }

    [DllImport("user32.dll")]
    private static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);

    public static uint GetIdleMilliseconds()
    {
        LASTINPUTINFO lii = new LASTINPUTINFO();
        lii.cbSize = (uint)Marshal.SizeOf(typeof(LASTINPUTINFO));
        if (!GetLastInputInfo(ref lii)) { return 0; }
        return unchecked((uint)Environment.TickCount - lii.dwTime);
    }
}
'@
    }
}

function Get-IdleSeconds {
    try { return [int]([DshIdleProbe]::GetIdleMilliseconds() / 1000) } catch { return -1 }
}

function Get-RunningPid {
    if (-not (Test-Path -LiteralPath $PidPath)) { return 0 }
    $raw = Get-Content -LiteralPath $PidPath -ErrorAction SilentlyContinue | Select-Object -First 1
    $parsed = 0
    if (-not [int]::TryParse(([string]$raw).Trim(), [ref]$parsed)) { return 0 }
    if ($null -ne (Get-Process -Id $parsed -ErrorAction SilentlyContinue)) { return $parsed }
    return 0
}

# --- -Status ---------------------------------------------------------------
if ($Status) {
    Initialize-NativeTypes
    $running = Get-RunningPid
    Write-Host '=== keep-awake 状态 ==='
    if ($running -ne 0) {
        $proc = Get-Process -Id $running -ErrorAction SilentlyContinue
        $started = ''
        $uptime = ''
        if ($proc -and $proc.StartTime) {
            $started = $proc.StartTime.ToString('yyyy-MM-dd HH:mm:ss')
            $uptime = '{0:N1}' -f ((Get-Date) - $proc.StartTime).TotalMinutes
        }
        Write-Host ("运行中 : 是 (PID={0}, 启动={1}, 已运行={2} 分钟)" -f $running, $started, $uptime)
    }
    else {
        Write-Host '运行中 : 否'
    }
    Write-Host ("当前空闲: {0} 秒 (本机超过 300 秒会熄屏并进入 S0 待机)" -f (Get-IdleSeconds))

    if (Test-Path -LiteralPath $LogPath) {
        Write-Host '--- 日志尾部 ---'
        Get-Content -LiteralPath $LogPath -Tail 8 -Encoding UTF8 | ForEach-Object { Write-Host $_ }
    }

    Write-Host '--- 最近待机事件 (Kernel-Power 506=进入 / 507=退出) ---'
    Get-WinEvent -FilterHashtable @{ LogName = 'System'; Id = 506, 507 } -MaxEvents 5 -ErrorAction SilentlyContinue |
        Select-Object TimeCreated, Id |
        Format-Table -AutoSize |
        Out-String -Width 120 |
        Write-Host
    exit 0
}

# --- -Stop -----------------------------------------------------------------
if ($Stop) {
    $running = Get-RunningPid
    Set-Content -LiteralPath $StopPath -Value (Get-Date).ToString('o') -Encoding UTF8
    if ($running -eq 0) {
        Write-Host '没有正在运行的 keep-awake 实例（已清理残留标记）。'
        Remove-Item -LiteralPath $StopPath -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
        exit 0
    }

    $deadline = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $deadline) {
        if ($null -eq (Get-Process -Id $running -ErrorAction SilentlyContinue)) { break }
        Start-Sleep -Milliseconds 500
    }
    if ($null -ne (Get-Process -Id $running -ErrorAction SilentlyContinue)) {
        Stop-Process -Id $running -Force -ErrorAction SilentlyContinue
        Write-Host ("实例 PID={0} 未在 15 秒内自行退出，已强制结束。" -f $running)
    }
    else {
        Write-Host ("实例 PID={0} 已正常退出。" -f $running)
    }
    Remove-Item -LiteralPath $StopPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
    Write-Host '电源请求已释放，系统恢复正常的空闲待机行为。'
    exit 0
}

# --- 启动 -------------------------------------------------------------------
$existing = Get-RunningPid
if ($existing -ne 0) {
    Write-Host ("已有实例在运行 (PID={0})，不重复启动。用 -Status 查看，-Stop 停止。" -f $existing)
    exit 2
}

Remove-Item -LiteralPath $StopPath -Force -ErrorAction SilentlyContinue
Initialize-NativeTypes

$flags = $ES_CONTINUOUS -bor $ES_SYSTEM_REQUIRED
if ($KeepDisplayOn) { $flags = $flags -bor $ES_DISPLAY_REQUIRED }

$previous = [DshSleepGuard]::SetThreadExecutionState($flags)
if ($previous -eq 0) {
    Write-Log 'SetThreadExecutionState 调用失败，未能申请电源请求。' 'ERROR'
    exit 1
}
Set-Content -LiteralPath $PidPath -Value $PID -Encoding ASCII

$mode = '阻止空闲待机（允许熄屏）'
if ($KeepDisplayOn) { $mode = '阻止空闲待机 + 阻止熄屏' }
$plan = '直到手动停止'
if ($Minutes -gt 0) { $plan = ('最长 {0} 分钟' -f $Minutes) }

$start = Get-Date
$deadline = [datetime]::MaxValue
if ($Minutes -gt 0) { $deadline = $start.AddMinutes($Minutes) }
$lastBeat = $start

try {
    Write-Log ("开始保持唤醒 | PID={0} | 模式={1} | 计划={2}" -f $PID, $mode, $plan)
    while ($true) {
        if (Test-Path -LiteralPath $StopPath) {
            Write-Log '检测到停止标记，退出。'
            break
        }
        if ((Get-Date) -ge $deadline) {
            Write-Log '已达设定的最长持有时长，退出。'
            break
        }

        $now = Get-Date
        if (($now - $lastBeat).TotalSeconds -ge 60) {
            # 重新申请一次：既是自检，也能在异常恢复后补上请求
            $held = [DshSleepGuard]::SetThreadExecutionState($flags)
            Write-Log ("保持中 | 已运行 {0:N1} 分钟 | 用户空闲 {1} 秒 | 请求状态={2}" -f `
                ($now - $start).TotalMinutes, (Get-IdleSeconds), $held)
            $lastBeat = $now
        }
        Start-Sleep -Seconds 1
    }
}
finally {
    [DshSleepGuard]::SetThreadExecutionState($ES_CONTINUOUS) | Out-Null
    Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $StopPath -Force -ErrorAction SilentlyContinue
    $total = '{0:N1}' -f ((Get-Date) - $start).TotalMinutes
    Write-Log ("已释放电源请求并退出 | 总计 {0} 分钟" -f $total)
}
