$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Normalize-Url {
  param(
    [string]$BaseUrl,
    [string]$Value
  )

  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $null
  }

  if ($Value.StartsWith('//')) {
    $base = [Uri]$BaseUrl
    return "$($base.Scheme):$Value"
  }

  if ($Value.StartsWith('http://') -or $Value.StartsWith('https://')) {
    return $Value
  }

  $uri = [Uri]::new([Uri]$BaseUrl, $Value)
  return $uri.AbsoluteUri
}

function Slugify {
  param([string]$Value)

  $slug = $Value.ToLowerInvariant() -replace '[^a-z0-9]+', '-' -replace '^-+', '' -replace '-+$', ''
  if ($slug -notmatch '-logo$') {
    $slug = "$slug-logo"
  }
  return $slug
}

function Parse-SqlValueRows {
  param(
    [string]$Path,
    [string]$CategoryLabel
  )

  $content = Get-Content $Path -Raw
  $matches = [regex]::Matches($content, "\('(?<name>[^']+)'\s*,\s*'(?<province>[^']*)'\s*,\s*(?<website>null|'[^']*')")
  $items = @()
  foreach ($match in $matches) {
    $websiteGroup = $match.Groups['website'].Value
    $website = if ($websiteGroup -eq 'NULL' -or $websiteGroup -eq 'null') { '' } else { $websiteGroup.Trim("'") }
    $items += [pscustomobject]@{
      name = $match.Groups['name'].Value
      website = $website
      category = $CategoryLabel
    }
  }
  return $items
}

function Get-LogoCandidate {
  param(
    [string]$Website
  )

  if ([string]::IsNullOrWhiteSpace($Website)) {
    return $null
  }

  try {
    $response = Invoke-WebRequest -Uri $Website -UseBasicParsing
  } catch {
    return $null
  }

  $html = $response.Content
  $matches = [regex]::Matches($html, '<img[^>]+src="([^"]+)"[^>]*>', 'IgnoreCase')
  $candidates = @()
  foreach ($match in $matches) {
    $src = $match.Groups[1].Value
    $resolved = Normalize-Url -BaseUrl $response.BaseResponse.ResponseUri.AbsoluteUri -Value $src
    if ([string]::IsNullOrWhiteSpace($resolved)) {
      continue
    }
    $score = 0
    if ($resolved -match 'logo|brand|header') { $score += 5 }
    if ($resolved -match '\.svg(\?|$)') { $score += 4 }
    if ($resolved -match '\.png(\?|$)|\.webp(\?|$)') { $score += 2 }
    if ($resolved -match 'tracking|pixel|avatar|close\\.svg|poweredbtcky') { $score -= 50 }
    $candidates += [pscustomobject]@{ url = $resolved; score = $score }
  }

  $best = $candidates | Sort-Object score -Descending | Select-Object -First 1
  if ($best) {
    return $best.url
  }

  return $null
}

function Download-Logo {
  param(
    [string]$Name,
    [string]$Category,
    [string]$Website,
    [string]$AssetsRoot
  )

  $candidate = Get-LogoCandidate -Website $Website
  if (-not $candidate) {
    return [pscustomobject]@{
      name = $Name
      sourceUrl = $Website
      localFilePath = ''
      fileType = ''
      retrievedAt = (Get-Date).ToString('yyyy-MM-dd')
      success = $false
      notes = 'No logo candidate found on official site.'
    }
  }

  try {
    $response = Invoke-WebRequest -Uri $candidate -UseBasicParsing
  } catch {
    return [pscustomobject]@{
      name = $Name
      sourceUrl = $candidate
      localFilePath = ''
      fileType = ''
      retrievedAt = (Get-Date).ToString('yyyy-MM-dd')
      success = $false
      notes = 'Logo download failed.'
    }
  }

  $contentType = $response.Headers['Content-Type']
  if ($contentType -notmatch '^image/') {
    return [pscustomobject]@{
      name = $Name
      sourceUrl = $candidate
      localFilePath = ''
      fileType = $contentType
      retrievedAt = (Get-Date).ToString('yyyy-MM-dd')
      success = $false
      notes = 'Response was not an image.'
    }
  }

  $extension = switch -Regex ($contentType) {
    'svg' { 'svg'; break }
    'png' { 'png'; break }
    'webp' { 'webp'; break }
    'jpeg|jpg' { 'jpg'; break }
    default { 'img' }
  }

  $folder = switch ($Category) {
    'University' { 'universities' }
    'TVET' { 'tvet' }
    'Online' { 'online' }
    default { 'private' }
  }

  $fileName = '{0}.{1}' -f (Slugify $Name), $extension
  $absoluteDir = Join-Path $AssetsRoot $folder
  $absolutePath = Join-Path $absoluteDir $fileName

  if (-not (Test-Path $absoluteDir)) {
    New-Item -ItemType Directory -Path $absoluteDir | Out-Null
  }

  if ($response.Content -is [string]) {
    Set-Content -Path $absolutePath -Value $response.Content -Encoding utf8
  } else {
    [System.IO.File]::WriteAllBytes($absolutePath, $response.Content)
  }

  return [pscustomobject]@{
    name = $Name
    sourceUrl = $candidate
    localFilePath = ("frontend/src/assets/institutions/{0}/{1}" -f $folder, $fileName)
    fileType = $contentType
    retrievedAt = (Get-Date).ToString('yyyy-MM-dd')
    success = $true
    notes = ''
  }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $repoRoot 'frontend/src/assets/institutions'
$manifestPath = Join-Path $assetsRoot 'logo-manifest.json'

$universities = @(
  [pscustomobject]@{ name = 'University of Cape Town'; website = 'https://www.uct.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Stellenbosch University'; website = 'https://www.sun.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of the Witwatersrand'; website = 'https://www.wits.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Pretoria'; website = 'https://www.up.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Johannesburg'; website = 'https://www.uj.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'North-West University'; website = 'https://www.nwu.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of KwaZulu-Natal'; website = 'https://www.ukzn.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Rhodes University'; website = 'https://www.ru.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of the Free State'; website = 'https://www.ufs.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Nelson Mandela University'; website = 'https://www.mandela.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of South Africa'; website = 'https://www.unisa.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of the Western Cape'; website = 'https://www.uwc.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Limpopo'; website = 'https://www.ul.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Venda'; website = 'https://www.univen.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Fort Hare'; website = 'https://www.ufh.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Walter Sisulu University'; website = 'https://www.wsu.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Zululand'; website = 'https://www.unizulu.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Mangosuthu University of Technology'; website = 'https://www.mut.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Cape Peninsula University of Technology'; website = 'https://www.cput.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Central University of Technology'; website = 'https://www.cut.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Durban University of Technology'; website = 'https://www.dut.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Tshwane University of Technology'; website = 'https://www.tut.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Vaal University of Technology'; website = 'https://www.vut.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Sefako Makgatho Health Sciences University'; website = 'https://www.smu.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'Sol Plaatje University'; website = 'https://www.spu.ac.za'; category = 'University' },
  [pscustomobject]@{ name = 'University of Mpumalanga'; website = 'https://www.ump.ac.za'; category = 'University' }
)

$online = @(
  [pscustomobject]@{ name = 'Alison'; website = 'https://alison.com'; category = 'Online' },
  [pscustomobject]@{ name = 'Coursera'; website = 'https://www.coursera.org'; category = 'Online' },
  [pscustomobject]@{ name = 'edX'; website = 'https://www.edx.org'; category = 'Online' },
  [pscustomobject]@{ name = 'FutureLearn'; website = 'https://www.futurelearn.com'; category = 'Online' },
  [pscustomobject]@{ name = 'Khan Academy'; website = 'https://www.khanacademy.org'; category = 'Online' },
  [pscustomobject]@{ name = 'freeCodeCamp'; website = 'https://www.freecodecamp.org'; category = 'Online' },
  [pscustomobject]@{ name = 'Microsoft Learn'; website = 'https://learn.microsoft.com'; category = 'Online' },
  [pscustomobject]@{ name = 'Cisco Networking Academy'; website = 'https://www.netacad.com'; category = 'Online' },
  [pscustomobject]@{ name = 'OpenLearn'; website = 'https://www.open.edu/openlearn'; category = 'Online' },
  [pscustomobject]@{ name = 'MIT OpenCourseWare'; website = 'https://ocw.mit.edu'; category = 'Online' },
  [pscustomobject]@{ name = 'W3Schools'; website = 'https://www.w3schools.com'; category = 'Online' },
  [pscustomobject]@{ name = 'AWS Skill Builder'; website = 'https://skillbuilder.aws'; category = 'Online' },
  [pscustomobject]@{ name = 'Oracle University'; website = 'https://education.oracle.com'; category = 'Online' },
  [pscustomobject]@{ name = 'IBM SkillsBuild'; website = 'https://skillsbuild.org'; category = 'Online' },
  [pscustomobject]@{ name = 'Google Cloud Skills Boost'; website = 'https://www.cloudskillsboost.google'; category = 'Online' },
  [pscustomobject]@{ name = 'Salesforce Trailhead'; website = 'https://trailhead.salesforce.com'; category = 'Online' },
  [pscustomobject]@{ name = 'Siyavula'; website = 'https://www.siyavula.com'; category = 'Online' },
  [pscustomobject]@{ name = 'DBE Cloud'; website = 'https://www.dbecloud.org'; category = 'Online' },
  [pscustomobject]@{ name = 'WCED ePortal'; website = 'https://wcedeportal.co.za'; category = 'Online' },
  [pscustomobject]@{ name = 'Mindset Learn'; website = 'https://learn.mindset.africa'; category = 'Online' }
)

$tvet = Parse-SqlValueRows -Path (Join-Path $repoRoot 'backend/src/main/resources/db/migration/V38__seed_public_tvet_colleges_and_colleges_module.sql') -CategoryLabel 'TVET'

$records = @()
foreach ($institution in ($universities + $online + $tvet)) {
  $records += Download-Logo -Name $institution.name -Category $institution.category -Website $institution.website -AssetsRoot $assetsRoot
}

$records | ConvertTo-Json -Depth 4 | Set-Content -Path $manifestPath -Encoding utf8
$summary = $records | Group-Object success | ForEach-Object { '{0}={1}' -f $_.Name, $_.Count }
Write-Output ($summary -join ', ')


