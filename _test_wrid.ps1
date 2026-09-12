$ErrorActionPreference = "Continue"
$tab = @(46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52)
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
$h = @{ "User-Agent" = $ua; "Referer" = "https://www.bilibili.com/" }
$nav = Invoke-RestMethod -Uri "https://api.bilibili.com/x/web-interface/nav" -Headers $h
$raw = ((($nav.data.wbi_img.img_url -split '/')[-1]) -replace '\.png$','') + ((($nav.data.wbi_img.sub_url -split '/')[-1]) -replace '\.png$','')
$mixin = -join ($tab | ForEach-Object { $raw[$_] })
function MD5($s){ $m=[System.Security.Cryptography.MD5]::Create(); -join ($m.ComputeHash([Text.Encoding]::UTF8.GetBytes($s)) | %{ $_.ToString('x2') }) }

$base = "https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&cid=137649199&type=mp4&qn=16&platform=html5"
$wts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

$correct = MD5("bvid=BV1GJ411x7h7&cid=137649199&platform=html5&qn=16&type=mp4&wts=$wts$mixin")
$buggy   = MD5("cid=137649199&https://api.bilibili.com/x/player/wbi/playurl?bvid=BV1GJ411x7h7&platform=html5&qn=16&type=mp4&wts=$wts$mixin")

$cases = @{
  "correct"  = "$base&wts=$wts&w_rid=$correct"
  "buggy"    = "$base&wts=$wts&w_rid=$buggy"
  "no-wrid"  = "$base&wts=$wts"
  "garbage"  = "$base&wts=$wts&w_rid=00000000000000000000000000000000"
}
foreach($k in $cases.Keys){
  try{ $r = Invoke-RestMethod -Uri $cases[$k] -Headers $h -ErrorAction Stop; Write-Host ("[$k] code=" + $r.code + " msg=" + $r.message + " durl=" + @($r.data.durl).Count) }
  catch{ Write-Host ("[$k] EXC " + $_.Exception.Message) }
}
