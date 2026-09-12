$ErrorActionPreference = "Stop"
$mixinKeyEncTab = @(46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52)

$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
$baseHeaders = @{
    "User-Agent" = $ua
    "Referer"    = "https://www.bilibili.com/"
}

$nav = Invoke-RestMethod -Uri "https://api.bilibili.com/x/web-interface/nav" -Headers $baseHeaders
$imgKey = (($nav.data.wbi_img.img_url -split '/')[-1]) -replace '\.png$', ''
$subKey = (($nav.data.wbi_img.sub_url -split '/')[-1]) -replace '\.png$', ''
$raw = $imgKey + $subKey
$mixin = -join ($mixinKeyEncTab | ForEach-Object { $raw[$_] })
Write-Host "raw=$raw"
Write-Host "mixin=$mixin"

function Invoke-PlayUrl($params) {
    $p = @{}
    foreach ($k in $params.Keys) { $p[$k] = $params[$k] }
    $p["wts"] = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $sorted = $p.GetEnumerator() | Sort-Object Name
    $parts = @()
    foreach ($e in $sorted) {
        $val = [regex]::Replace([string]$e.Value, "[!()'*]", "")
        $parts += "$($e.Name)=$([uri]::EscapeDataString($val))"
    }
    $query = $parts -join '&'
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $hash = $md5.ComputeHash([Text.Encoding]::UTF8.GetBytes($query + $mixin))
    $wrid = -join ($hash | ForEach-Object { $_.ToString('x2') })
    $url = "https://api.bilibili.com/x/player/wbi/playurl?$query&w_rid=$wrid"
    Write-Host "URL: $url"
    return Invoke-RestMethod -Uri $url -Headers $baseHeaders
}

Write-Host "=== (A) platform=html5, type=mp4, qn=16, high_quality=0  [app current] ==="
$r = Invoke-PlayUrl @{ bvid = "BV1GJ411x7h7"; cid = "137649199"; qn = "16"; type = "mp4"; platform = "html5"; high_quality = "0" }
Write-Host ("code=" + $r.code + " msg=" + $r.message)
Write-Host ("quality=" + $r.data.quality + " format=" + $r.data.format)
Write-Host ("durl count=" + @($r.data.durl).Count)
if (@($r.data.durl).Count -gt 0) { Write-Host ("durl[0].url=" + $r.data.durl[0].url) }
Write-Host ("dash is null=" + ($null -eq $r.data.dash))

Write-Host ""
Write-Host "=== (B) platform=html5, type=mp4, qn=16 (no high_quality) ==="
$r2 = Invoke-PlayUrl @{ bvid = "BV1GJ411x7h7"; cid = "137649199"; qn = "16"; type = "mp4"; platform = "html5" }
Write-Host ("code=" + $r2.code + " msg=" + $r2.message)
Write-Host ("durl count=" + @($r2.data.durl).Count)

Write-Host ""
Write-Host "=== (C) no platform, fnval=0, qn=16 ==="
$r3 = Invoke-PlayUrl @{ bvid = "BV1GJ411x7h7"; cid = "137649199"; qn = "16"; fnval = "0"; fnver = "0"; fourk = "1" }
Write-Host ("code=" + $r3.code + " msg=" + $r3.message)
Write-Host ("durl count=" + @($r3.data.durl).Count)
if (@($r3.data.durl).Count -gt 0) { Write-Host ("durl[0].url=" + $r3.data.durl[0].url) }
