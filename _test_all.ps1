$ErrorActionPreference = "Continue"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"

function Call($name, $url, $cookie) {
    $h = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }
    if ($cookie -ne $null) { $h["Cookie"] = $cookie }
    try {
        $r = Invoke-RestMethod -Uri $url -Headers $h -ErrorAction Stop
        $dc = @($r.data.durl).Count
        Write-Host ("[$name] code=" + $r.code + " msg=" + $r.message + " durlCount=" + $dc + " dashNull=" + ($null -eq $r.data.dash) + " acceptQuality=" + (@($r.data.accept_quality) -join ','))
    } catch {
        Write-Host ("[$name] EXCEPTION: " + $_.Exception.Message)
    }
}

$ugcHtml5 = "https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=html5"
$ugcPc    = "https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=pc"

Write-Host "---- UGC html5 ----"
Call "no-cookie"      $ugcHtml5 $null
Call "empty-cookie"   $ugcHtml5 ""
Call "buvid-cookie"   $ugcHtml5 "buvid3=abc123infoc"
Write-Host "---- UGC pc ----"
Call "no-cookie"      $ugcPc $null
Call "empty-cookie"   $ugcPc ""

# 番剧测试：用某个 ep（先查 season）
Write-Host "---- bangumi ep ----"
try {
    $pgc = Invoke-RestMethod -Uri "https://api.bilibili.com/pgc/view/web/season?ep_id=321104" -Headers @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }
    $ep = $pgc.result.episodes[0]
    Write-Host ("ep_id=" + $ep.id + " aid=" + $ep.aid + " cid=" + $ep.cid + " title=" + $ep.long_title)
    $epUrl = "https://api.bilibili.com/pgc/player/web/playurl?ep_id=" + $ep.id + "&cid=" + $ep.cid + "&qn=16&type=mp4&platform=pc"
    Call "no-cookie"    $epUrl $null
    Call "empty-cookie" $epUrl ""
} catch { Write-Host ("pgc err: " + $_.Exception.Message) }
