$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Net.Http
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
$h = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }

$url = "https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=html5"
$r = Invoke-RestMethod -Uri $url -Headers $h
$vurl = $r.data.durl[0].url
Write-Host ("durl = " + ($vurl.Substring(0, [Math]::Min(120, $vurl.Length))))

$client = New-Object System.Net.Http.HttpClient
$req = New-Object System.Net.Http.HttpRequestMessage("GET", $vurl)
$req.Headers.TryAddWithoutValidation("User-Agent", $ua) | Out-Null
$req.Headers.TryAddWithoutValidation("Referer", "https://www.bilibili.com/") | Out-Null
$req.Headers.Range = New-Object System.Net.Http.Headers.RangeHeaderValue(0, 1023)
try {
    $resp = $client.SendAsync($req).Result
    Write-Host ("CDN status = " + [int]$resp.StatusCode + " " + $resp.StatusCode)
} catch {
    Write-Host ("CDN ERR: " + $_.Exception.Message)
}

Write-Host ""
Write-Host "=== 对比: 带 mid=0 的 uparams 是否影响 ---"
$req2 = New-Object System.Net.Http.HttpRequestMessage("GET", $vurl)
$req2.Headers.TryAddWithoutValidation("User-Agent", $ua) | Out-Null
$req2.Headers.TryAddWithoutValidation("Referer", "https://www.bilibili.com/") | Out-Null
$req2.Headers.Range = New-Object System.Net.Http.Headers.RangeHeaderValue(0, 1023)
$req2.Headers.TryAddWithoutValidation("Origin", "https://www.bilibili.com") | Out-Null
try {
    $resp2 = $client.SendAsync($req2).Result
    Write-Host ("CDN status(with origin) = " + [int]$resp2.StatusCode)
} catch {
    Write-Host ("CDN ERR2: " + $_.Exception.Message)
}
