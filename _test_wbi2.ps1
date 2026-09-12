$ErrorActionPreference = "Stop"
$mixinKeyEncTab = @(46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52)

$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
$baseHeaders = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }

$nav = Invoke-RestMethod -Uri "https://api.bilibili.com/x/web-interface/nav" -Headers $baseHeaders
$raw = ((($nav.data.wbi_img.img_url -split '/')[-1]) -replace '\.png$', '') + ((($nav.data.wbi_img.sub_url -split '/')[-1]) -replace '\.png$', '')
$mixin = -join ($mixinKeyEncTab | ForEach-Object { $raw[$_] })

function Get-MD5($s) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    return (-join ($md5.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)) | ForEach-Object { $_.ToString('x2') }))
}

$query = "bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=html5"

Write-Host "=== (1) 正确算法 w_rid ==="
$wts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$correct = "bvid=BV1GJ411x7h7&cid=137649199&platform=html5&qn=16&type=mp4&wts=$wts"
$wrid1 = Get-MD5 ($correct + $mixin)
$u1 = "https://api.bilibili.com/x/player/wbi/playurl?$query&w_rid=$wrid1&wts=$wts"
try { $r1 = Invoke-RestMethod -Uri $u1 -Headers $baseHeaders; Write-Host ("code=" + $r1.code + " msg=" + $r1.message + " durl=" + @($r1.data.durl).Count) } catch { Write-Host "ERR $_" }

Write-Host ""
Write-Host "=== (2) App 现有算法（把整条URL当作第一个参数）==="
$wts2 = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$fullUrl = "https://api.bilibili.com/x/player/wbi/playurl?$query"
# 模拟 sortUrlParams: 以 & 分割后按 key 排序, 第一个 key 变成 "https://...?bvid"
$appCalc = "cid=137649199&https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&platform=html5&qn=16&type=mp4&wts=$wts2"
$wrid2 = Get-MD5 ($appCalc + $mixin)
$u2 = "$fullUrl&w_rid=$wrid2&wts=$wts2"
try { $r2 = Invoke-RestMethod -Uri $u2 -Headers $baseHeaders; Write-Host ("code=" + $r2.code + " msg=" + $r2.message + " durl=" + @($r2.data.durl).Count) } catch { Write-Host "ERR $_" }

Write-Host ""
Write-Host "=== (3) 正确 w_rid + 空 Cookie 头 (app 未登录时的情形) ==="
$h3 = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/"; "Cookie" = "" }
try { $r3 = Invoke-RestMethod -Uri $u1 -Headers $h3; Write-Host ("code=" + $r3.code + " msg=" + $r3.message + " durl=" + @($r3.data.durl).Count) } catch { Write-Host "ERR $_" }

Write-Host ""
Write-Host "=== (4) 完全没有 w_rid ==="
$u4 = "$fullUrl&wts=$wts"
try { $r4 = Invoke-RestMethod -Uri $u4 -Headers $baseHeaders; Write-Host ("code=" + $r4.code + " msg=" + $r4.message) } catch { Write-Host "ERR $_" }
