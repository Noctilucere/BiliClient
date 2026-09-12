$ErrorActionPreference = "Stop"
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
$h = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }

# 未登录（不带任何 cookie）请求 playurl
$url = "https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=html5"
$r = Invoke-RestMethod -Uri $url -Headers $h
$vurl = $r.data.durl[0].url
Write-Host ("durl url = " + $vurl)

Write-Host ""
Write-Host "=== 尝试 Range 请求视频 CDN (不带 cookie) ==="
try {
    $resp = Invoke-WebRequest -Uri $vurl -Headers @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/"; "Range" = "bytes=0-1023" } -Method Head -ErrorAction Stop
    Write-Host ("status=" + $resp.StatusCode)
} catch {
    Write-Host ("HEAD ERR: " + $_.Exception.Message)
}
try {
    $resp2 = Invoke-WebRequest -Uri $vurl -Headers @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/"; "Range" = "bytes=0-1023" } -ErrorAction Stop
    Write-Host ("GET status=" + $resp2.StatusCode + " len=" + $resp2.RawContentLength)
} catch {
    Write-Host ("GET ERR: " + $_.Exception.Message)
}
