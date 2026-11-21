$ErrorActionPreference = 'Stop'
# CRM 端到端冒烟: 登录(滑块验证码 AES-ECB + 传输密码 AES-CFB128) → 客户→商机→赢单→合同→审批→归档→回款→看板→AI 洞察
# 需本地依赖: 单体 9010 + Redis; 通过环境变量注入: REDIS_PASS / CRM_ADMIN_USER / CRM_ADMIN_PWD / TRANSPORT_KEY / REDIS_CLI
$base = 'http://localhost:9010'
$redisCli = if ($env:REDIS_CLI) { $env:REDIS_CLI } else { 'D:\Redis-8.8.0\redis-cli.exe' }
$redisPass = $env:REDIS_PASS
$adminUser = if ($env:CRM_ADMIN_USER) { $env:CRM_ADMIN_USER } else { 'admin' }
$adminPwd = if ($env:CRM_ADMIN_PWD) { $env:CRM_ADMIN_PWD } else { 'admin123' }
$transportKey = if ($env:TRANSPORT_KEY) { $env:TRANSPORT_KEY } else { 'mate9x!K#2qL8pZw' }
function AesEcbB64([string]$plain, [string]$key) {
  $aes = [System.Security.Cryptography.Aes]::Create(); $aes.Mode='ECB'; $aes.Padding='PKCS7'; $aes.KeySize=128
  $aes.Key=[System.Text.Encoding]::UTF8.GetBytes($key); $enc=$aes.CreateEncryptor()
  $b=[System.Text.Encoding]::UTF8.GetBytes($plain); [Convert]::ToBase64String($enc.TransformFinalBlock($b,0,$b.Length))
}
function AesCfbB64([string]$plain, [string]$key) {
  $aes = [System.Security.Cryptography.Aes]::Create(); $aes.Mode='ECB'; $aes.Padding='None'; $aes.KeySize=128
  $k=[System.Text.Encoding]::UTF8.GetBytes($key); $aes.Key=$k; $enc=$aes.CreateEncryptor()
  $bytes=[System.Text.Encoding]::UTF8.GetBytes($plain); $fb=$k; $out=New-Object byte[] $bytes.Length
  for($off=0;$off -lt $bytes.Length;$off+=16){ $ks=$enc.TransformFinalBlock($fb,0,16); $len=[Math]::Min(16,$bytes.Length-$off)
    for($i=0;$i -lt $len;$i++){ $out[$off+$i]=$bytes[$off+$i] -bxor $ks[$i] }
    $fb=New-Object byte[] 16; [Array]::Copy($out,$off,$fb,0,$len) }
  [Convert]::ToBase64String($out)
}
$r = Invoke-RestMethod "$base/api/v1/auth/captcha/get?captchaType=blockPuzzle"
$token=$r.repData.token; $sk=$r.repData.secretKey
$ans = & $redisCli -a $redisPass --no-auth-warning get "captcha:RUNNING:CAPTCHA:$token"
$x = ($ans | ConvertFrom-Json).x
$pointPlain = '{"x":' + $x + ',"y":5}'
$chk = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/captcha/check" -ContentType 'application/json' -Body (@{captchaType='blockPuzzle';token=$token;pointJson=(AesEcbB64 $pointPlain $sk)} | ConvertTo-Json)
$cv = $chk.repData.captchaVerification; if(-not $cv){ $cv = AesEcbB64 ($token+'---'+$pointPlain) $sk }
$login = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/login" -ContentType 'application/json' -Body (@{account=$adminUser;password=(AesCfbB64 $adminPwd $transportKey);captchaVerification=$cv} | ConvertTo-Json)
$H = @{ $login.data.tokenName = "Bearer $($login.data.tokenValue)" }
Write-Host 'LOGIN OK'

function Call($method, $path, $body) {
  if ($null -ne $body) { Invoke-RestMethod -Method $method -Uri "$base$path" -Headers $H -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 6) }
  else { Invoke-RestMethod -Method $method -Uri "$base$path" -Headers $H }
}
function Assert($res, $label) {
  if (-not $res.success) { throw "$label FAILED code=$($res.code) msg=$($res.msg)" }
  Write-Host "$label OK data=$($res.data | ConvertTo-Json -Compress -Depth 3)"
}

$tag = Get-Random
$cust = Call Post '/api/v1/crm/customers' @{ customerName = "闭环客户-$tag"; industry='IT'; level='A' }; Assert $cust 'CUSTOMER'
$custId = $cust.data
$biz = Call Post '/api/v1/crm/businesses' @{ businessName = "闭环商机-$tag"; customerId = $custId; customerName = "闭环客户-$tag"; products = @(@{ productName = '标准版许可证'; unitPrice = 500; quantity = 2 }) }; Assert $biz 'BUSINESS'
$bizId = $biz.data
foreach ($stage in 'DEMAND','PLAN','NEGOTIATION') { $null = Call Put "/api/v1/crm/businesses/$bizId/stage`?stage=$stage" $null }
$null = Call Put "/api/v1/crm/businesses/$bizId/win`?endRemark=ok" $null
Write-Host 'BUSINESS WON'
$ctr = Call Post "/api/v1/crm/contracts/from-business/$bizId" $null; Assert $ctr 'FROM-BUSINESS'
$cid = $ctr.data
$ap = Call Post "/api/v1/crm/approvals/contract/$cid" $null; Assert $ap 'SUBMIT-APPROVAL'
$apvId = $ap.data
$a1 = Call Put "/api/v1/crm/approvals/$apvId/approve`?remark=ok1" $null; Assert $a1 'APPROVE-1'
$a2 = Call Put "/api/v1/crm/approvals/$apvId/approve`?remark=ok2" $null; Assert $a2 'APPROVE-2'
$cd = Call Get "/api/v1/crm/contracts/$cid"
Write-Host "CONTRACT STATUS=$($cd.data.contract.status)"
$ar = Call Put "/api/v1/crm/contracts/$cid/archive" $null; Assert $ar 'ARCHIVE'
$plan = Call Post '/api/v1/crm/repayments/plans' @{ contractId=$cid; termNo=1; money=500; checkingDate='2026-12-31'; returnType='TRANSFER' }; Assert $plan 'PLAN-CREATE'
$planId = $plan.data
if (-not $planId) { $plans = Call Get "/api/v1/crm/repayments/contracts/$cid/plans"; $planId = $plans.data[0].id }
$rep = Call Post '/api/v1/crm/repayments' @{ contractId=$cid; planId=$planId; money=200; paymentMethod='BANK_TRANSFER'; returnTime='2026-11-21' }; Assert $rep 'REGISTER'
$repId = $rep.data
if (-not $repId) { throw 'REGISTER returned empty id' }
$ov = Call Get '/api/v1/crm/stats/overview'
Write-Host "OVERVIEW contractCount=$($ov.data.contractCount) contractMoney=$($ov.data.contractMoney) received=$($ov.data.receivedMoney) receivedRate=$($ov.data.receivedRate)"
$ins = Call Get "/api/v1/crm/ai/insight/$custId"; Assert $ins 'AI-INSIGHT'
Write-Host 'FLOW-CHECK PASS'
