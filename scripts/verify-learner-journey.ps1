param([switch]$LiveAi)
$ErrorActionPreference = 'Stop'
# Run only against the disposable database created with learner-journey-fixtures.sql.
$base = 'http://127.0.0.1:8082/api'
function Login([string]$email) {
    $session = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType application/json -Body (@{email=$email;password='JourneyFixture-2026!'} | ConvertTo-Json)
    return @{Authorization='Bearer '+$session.accessToken}
}
function Api([string]$method,[string]$path,$body=$null,$headers=$script:pro) {
    $args = @{Uri="$base$path";Method=$method;Headers=$headers;TimeoutSec=660}
    if ($null -ne $body) { $args.ContentType='application/json'; $args.Body=$body | ConvertTo-Json -Depth 60 -Compress }
    $result = Invoke-RestMethod @args
    return $result
}
function Check([bool]$condition,[string]$label) { if (!$condition) { throw "FAILED: $label" }; Write-Output "PASS: $label" }
$script:pro = Login 'journey-pro@example.test'
$basic = Login 'journey-basic@example.test'
Check ((Api GET '/subscriptions/entitlements').plan -eq 'PRO') 'Pro fixture recognized by backend'
Check ((Api GET '/subscriptions/entitlements' $null $basic).plan -eq 'BASIC') 'Basic fixture remains Basic'
try { Api POST '/student/career-roadmaps/study-plans' @{requestKey=[guid]::NewGuid();careerName='Unsupported'} $basic | Out-Null; throw 'Basic generation was allowed' }
catch { Check ([int]$_.Exception.Response.StatusCode -eq 403) 'Basic study-plan generation denied by backend' }
$catalogue=Api GET '/careers?size=25'
Check ($catalogue.content.Count -gt 0) 'Supported career catalogue available'
$career=$catalogue.content[0]
$empty=Api GET '/careers?q=UnsupportedImaginaryCareer98765'
Check ($empty.totalElements -eq 0) 'Unsupported search returns no invented careers'
$profile=Api GET '/student/profile'
$subjects=@('Mathematics','English Home Language','Physical Sciences','Life Sciences','Geography','History') | ForEach-Object { @{subjectName=$_;markPercentage=60} }
Api PUT '/student/profile' @{selectedGrade='Grade 12';subjectAchievements=$subjects;expectedSubjectAchievements=@($profile.subjectAchievements);careerGoals='Improve mathematics through weekly practice'} | Out-Null
$script:pro=Login 'journey-pro@example.test'
$profile=Api GET '/student/profile'
Check ($profile.subjectAchievements.Count -eq 6) 'Manual subjects persist after a new login'
Api PUT '/student/profile' @{subjectAchievements=$subjects;expectedSubjectAchievements=@($profile.subjectAchievements)} | Out-Null
Check ((Api GET '/student/profile').subjectAchievements.Count -eq 6) 'Repeated academic save does not duplicate subjects'
Api PUT '/student/career-roadmaps/selection' @{careerName=$career.title} | Out-Null
Check ((Api GET '/student/career-roadmaps/selection').careerName -eq $career.title) 'Career selection persists'
Api POST "/student/careers/$($career.id)/save" | Out-Null
Check ((Api GET '/student/careers/saved').items -contains $career.id) 'Career save reloads'
Api PUT "/student/profile/associations/CAREER/$($career.id)" | Out-Null
Check (@(Api GET '/student/profile/associations').Count -ge 1) 'Profile career link persists'
Check (@(Api GET '/student/profile/associations' $null $basic).Count -eq 0) 'Other learner cannot see profile links'
foreach ($path in @('/student/settings','/student/transcripts','/student/psychometric/history','/student/psychometric/assessments','/recommendations/me')) {
    Api GET $path | Out-Null; Write-Output "PASS: HTTP $path"
}
if ($LiveAi) {
    $roadmap=Api POST '/student/career-roadmaps/generate' @{careerName=$career.title;subjects=@()}
    Check ($roadmap.roadmapTimeline.Count -gt 0) 'Live roadmap generation returns a timeline'
    Api POST '/student/career-roadmaps/save' @{careerName=$career.title;roadmap=$roadmap;learnerAps=999;requiredAps=999;apsGap=999;readinessScore=100} | Out-Null
    $saved=@(Api GET '/student/career-roadmaps/saved')
    Check ($saved.Count -gt 0 -and $saved[0].learnerAps -ne 999) 'Saved roadmap reloads with server-authoritative academic evidence'
    $key=[guid]::NewGuid().ToString()
    $plan=Api POST '/student/career-roadmaps/study-plans' @{requestKey=$key;careerName=$career.title}
    Check ($plan.steps.Count -ge 4) 'Live personalized study plan generated and saved'
    $retry=Api POST '/student/career-roadmaps/study-plans' @{requestKey=$key;careerName=$career.title}
    Check ($retry.id -eq $plan.id) 'Study-plan retry is idempotent'
    Api PUT "/student/career-roadmaps/study-plans/$($plan.id)/progress" @{completedSteps=@(0)} | Out-Null
    $script:pro=Login 'journey-pro@example.test'
    $history=@(Api GET '/student/career-roadmaps/study-plans')
    Check (($history | Where-Object id -eq $plan.id).completedSteps -contains 0) 'Plan progress persists after new login'
    $answer=Api POST '/student/tutor/ask' @{subject='MATHEMATICS';question='Solve x squared minus 5x plus 6 equals zero. Show each step and check both roots.'}
    Check ($answer.answer.Length -gt 150) 'Live tutor returns a worked explanation'
    $reopened=Api GET "/student/tutor/sessions/$($answer.sessionId)"
    Check ($reopened.messages.Count -ge 2) 'Tutor conversation reloads from database'
}
Api DELETE "/student/careers/$($career.id)/save" | Out-Null
Check ((Api GET '/student/careers/saved').items -notcontains $career.id) 'Saved career removal persists'
Api DELETE '/student/career-roadmaps/selection' | Out-Null
Check ([string]::IsNullOrEmpty((Api GET '/student/career-roadmaps/selection').careerName)) 'Career selection clear persists'
