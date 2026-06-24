# EduRite API Endpoint Audit

Generated from source on 2026-05-01 after route compatibility fixes.

## Summary

- Frontend API call sites under `frontend/src`: 160
- Unique frontend method/path pairs: 158
- Backend controller mappings extracted from `@RestController` classes: 396
- Result after fixes: every frontend method/path pair is `MATCHED`.
- No servlet context path is configured in `application.yml`, `application-dev.yml`, `application-prod.yml`, `docker-compose.yml`, `.env.example`, or `.env.test`; controllers own their `/api` and `/api/v1` prefixes directly.

## Frontend Calls

| Frontend file | Line | Method | API path | Status | Access |
|---|---:|---|---|---|---|
| `frontend/src/services/accountService.ts` | 5 | DELETE | `/account/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/accountService.ts` | 6 | POST | `/account/password/change/request-otp` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/accountService.ts` | 7 | POST | `/account/password/change/confirm` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 101 | GET | `/admin/analytics` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 103 | GET | `/admin/users` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 106 | PATCH | `/admin/users/{id}/status` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 107 | PATCH | `/admin/users/{id}/suspend` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 108 | PATCH | `/admin/users/{id}/unsuspend` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 109 | DELETE | `/admin/users/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 111 | GET | `/admin/roles` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 112 | POST | `/admin/roles` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 113 | PUT | `/admin/roles/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 114 | DELETE | `/admin/roles/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 116 | GET | `/admin/companies` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 119 | GET | `/admin/companies/pending` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 120 | GET | `/admin/companies/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 121 | PATCH | `/admin/companies/{id}/approve` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 122 | PATCH | `/admin/companies/{id}/reject` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 123 | PATCH | `/admin/companies/{id}/more-info` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 124 | PATCH | `/admin/companies/{id}/suspend` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 125 | PATCH | `/admin/companies/{id}/reactivate` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 126 | DELETE | `/admin/companies/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 128 | GET | `/admin/bursaries` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 131 | GET | `/admin/bursaries/pending` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 132 | PATCH | `/admin/bursaries/{id}/review` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 133 | PATCH | `/admin/bursaries/{id}/suspend` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 134 | PATCH | `/admin/bursaries/{id}/reactivate` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 135 | DELETE | `/admin/bursaries/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 137 | GET | `/admin/settings` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 138 | PUT | `/admin/settings` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 140 | GET | `/admin/audit-logs` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 144 | POST | `/admin/users/bulk-upload` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/adminService.ts` | 146 | GET | `/admin/users/bulk-upload/template` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/aiGuidanceService.ts` | 69 | POST | `/ai/career-advice` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/aiGuidanceService.ts` | 71 | POST | `/ai/analyse-university-sources` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/aiGuidanceService.ts` | 73 | GET | `/ai/default-university-sources` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/aiGuidanceService.ts` | 74 | GET | `/recommendations/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/apiClient.ts` | 167 | POST | `/auth/refresh` | MATCHED | PUBLIC |
| `frontend/src/services/apiClient.ts` | 177 | POST | `/auth/refresh` | MATCHED | PUBLIC |
| `frontend/src/services/applicationService.ts` | 4 | GET | `/applications/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/applicationService.ts` | 5 | POST | `/bursaries/{bursaryId}/applications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/authService.ts` | 96 | POST | `/auth/login` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 100 | POST | `/auth/google` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 102 | POST | `/auth/register/student` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 103 | POST | `/auth/register/company` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 104 | POST | `/auth/verify-otp` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 105 | POST | `/auth/resend-verification-otp` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 106 | POST | `/auth/forgot-password/request-otp` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 107 | POST | `/auth/forgot-password/reset` | MATCHED | PUBLIC |
| `frontend/src/services/authService.ts` | 108 | POST | `/auth/logout` | MATCHED | PUBLIC |
| `frontend/src/services/bursaryService.ts` | 18 | GET | `/bursaries` | MATCHED | PUBLIC |
| `frontend/src/services/bursaryService.ts` | 19 | GET | `/bursaries/search` | MATCHED | PUBLIC |
| `frontend/src/services/bursaryService.ts` | 20 | GET | `/bursaries/recommendations/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/bursaryService.ts` | 21 | GET | `/bursaries/{id}` | MATCHED | PUBLIC |
| `frontend/src/services/careerService.ts` | 5 | GET | `/careers` | MATCHED | PUBLIC |
| `frontend/src/services/careerService.ts` | 6 | GET | `/careers/{id}` | MATCHED | PUBLIC |
| `frontend/src/services/companyService.ts` | 4 | GET | `/companies/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 5 | GET | `/companies/dashboard` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 6 | PUT | `/companies/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 11 | POST | `/companies/me/documents` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 13 | GET | `/companies/me/documents` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 14 | GET | `/companies/bursaries` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 15 | GET | `/companies/bursaries/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 16 | POST | `/companies/bursaries` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 17 | PUT | `/companies/bursaries/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 18 | PATCH | `/companies/bursaries/{id}/close` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 19 | PATCH | `/companies/bursaries/{id}/unpublish` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 20 | PATCH | `/companies/bursaries/{id}/reopen` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 21 | GET | `/companies/students/search` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 22 | POST | `/companies/students/{studentId}/bookmarks` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 23 | GET | `/companies/students/bookmarks` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 24 | POST | `/companies/students/{studentId}/shortlists` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 25 | GET | `/companies/students/shortlists` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 26 | POST | `/companies/students/{studentId}/messages` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 27 | GET | `/companies/messages` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 28 | POST | `/companies/students/{studentId}/invitations` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/companyService.ts` | 29 | GET | `/companies/invitations` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/courseService.ts` | 5 | GET | `/courses` | MATCHED | PUBLIC |
| `frontend/src/services/courseService.ts` | 6 | GET | `/courses/{id}` | MATCHED | PUBLIC |
| `frontend/src/services/featureModulesService.ts` | 21 | GET | `/student/progress-score` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 23 | GET | `/student/cv` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 24 | PUT | `/student/cv` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 25 | GET | `/student/cv/ai-suggestions` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 27 | GET | `/student/scholarship-applications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 28 | GET | `/student/scholarship-applications/upcoming` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 29 | POST | `/student/scholarship-applications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 30 | PUT | `/student/scholarship-applications/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 31 | DELETE | `/student/scholarship-applications/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 32 | POST | `/student/scholarship-applications/{id}/motivation-letter` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 34 | GET | `/student/tutor/sessions` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 35 | GET | `/student/tutor/sessions/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 36 | POST | `/student/tutor/ask` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 38 | GET | `/student/university-applications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 39 | POST | `/student/university-applications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 40 | PUT | `/student/university-applications/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 41 | DELETE | `/student/university-applications/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 43 | GET | `/student/mentorship/mentors` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 44 | GET | `/student/mentorship/requests` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 45 | POST | `/student/mentorship/requests` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 47 | GET | `/student/internships` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 48 | POST | `/student/internships/{id}/interest` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 50 | GET | `/student/career-roadmaps` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 51 | GET | `/student/career-roadmaps/{slug}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 53 | GET | `/student/international-opportunities` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 54 | POST | `/student/international-opportunities/{id}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 55 | DELETE | `/student/international-opportunities/{id}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 57 | GET | `/admin/schools` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 58 | POST | `/admin/schools` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 59 | PUT | `/admin/schools/{id}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 60 | GET | `/admin/schools/{id}/summary` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/featureModulesService.ts` | 61 | POST | `/admin/schools/{id}/students` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/gamificationService.ts` | 5 | GET | `/student/gamification/summary` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/gamificationService.ts` | 6 | POST | `/student/gamification/tasks/complete` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/gamificationService.ts` | 7 | POST | `/student/gamification/claims` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/institutionService.ts` | 2 | GET | `/institutions` | MATCHED | PUBLIC |
| `frontend/src/services/institutionService.ts` | 2 | GET | `/institutions/{id}` | MATCHED | PUBLIC |
| `frontend/src/services/learningService.ts` | 5 | GET | `/student/learning-centre/catalogue` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/learningService.ts` | 6 | GET | `/student/learning-centre/recommended` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/notificationService.ts` | 4 | GET | `/notifications` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/notificationService.ts` | 5 | PATCH | `/notifications/{id}/read` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 6 | GET | `/student/psychometric/assessments` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 7 | GET | `/student/psychometric/assessments/{assessmentId}/questions` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 8 | GET | `/student/psychometric/assessments/{assessmentId}/attempts` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 10 | POST | `/student/psychometric/assessments/{assessmentId}/attempts` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 11 | POST | `/student/psychometric/submit` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 14 | GET | `/student/psychometric/latest` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/psychometricService.ts` | 23 | POST | `/public/psychometric/submit` | MATCHED | PUBLIC |
| `frontend/src/services/publicDiscoveryService.ts` | 11 | GET | `/public/discovery/careers/insight` | MATCHED | PUBLIC |
| `frontend/src/services/publicDiscoveryService.ts` | 12 | GET | `/public/discovery/courses/insight` | MATCHED | PUBLIC |
| `frontend/src/services/publicDiscoveryService.ts` | 13 | GET | `/public/discovery/bursaries/insight` | MATCHED | PUBLIC |
| `frontend/src/services/recommendationService.ts` | 3 | GET | `/recommendations/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/settingsService.ts` | 10 | GET | `/student/settings` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/settingsService.ts` | 11 | PUT | `/student/settings` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 13 | GET | `/student/profile` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 14 | PUT | `/student/profile` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 15 | GET | `/student/profile/saved` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 17 | POST | `/student/profile/saved` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 18 | GET | `/student/profile/saved/{savedProfileId}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 19 | POST | `/student/profile/saved/{savedProfileId}/apply` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 20 | DELETE | `/student/profile/saved/{savedProfileId}` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 24 | POST | `/student/profile/cv` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 29 | POST | `/student/profile/transcript` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 31 | GET | `/student/dashboard` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 32 | GET | `/student/opportunities` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 33 | POST | `/student/opportunities/{type}/{opportunityId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 34 | DELETE | `/student/opportunities/{type}/{opportunityId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 35 | POST | `/student/careers/{careerId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 36 | DELETE | `/student/careers/{careerId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 37 | GET | `/student/careers/saved` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 38 | POST | `/student/bursaries/{bursaryId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 39 | DELETE | `/student/bursaries/{bursaryId}/save` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/studentService.ts` | 40 | GET | `/student/bursaries/saved` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 15 | GET | `/subscriptions/plans` | MATCHED | PUBLIC |
| `frontend/src/services/subscriptionService.ts` | 16 | GET | `/subscriptions/me` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 17 | POST | `/subscriptions/checkout` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 18 | POST | `/subscriptions/confirm` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 19 | POST | `/subscriptions/cancel` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 20 | POST | `/payments/payfast/initiate` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 21 | GET | `/payments/payfast/status` | MATCHED | AUTH REQUIRED |
| `frontend/src/services/subscriptionService.ts` | 22 | POST | `/subscriptions/purchase` | MATCHED | AUTH REQUIRED |

## Backend Controller Mappings

| Controller file | Line | Handler | Method | Full path |
|---|---:|---|---|---|
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 26 | `deleteMyAccount` | DELETE | `/api/v1/account/me` |
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 26 | `deleteMyAccount` | DELETE | `/api/account/me` |
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 31 | `requestPasswordChangeOtp` | POST | `/api/v1/account/password/change/request-otp` |
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 31 | `requestPasswordChangeOtp` | POST | `/api/account/password/change/request-otp` |
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 36 | `changePasswordWithOtp` | POST | `/api/v1/account/password/change/confirm` |
| `backend/src/main/java/com/edurite/account/controller/AccountController.java` | 36 | `changePasswordWithOtp` | POST | `/api/account/password/change/confirm` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 41 | `users` | GET | `/api/v1/admin/users` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 41 | `users` | GET | `/api/admin/users` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 52 | `userStatus` | PATCH | `/api/v1/admin/users/{id}/status` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 52 | `userStatus` | PATCH | `/api/admin/users/{id}/status` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 57 | `suspendUser` | PATCH | `/api/v1/admin/users/{id}/suspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 57 | `suspendUser` | PATCH | `/api/admin/users/{id}/suspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 62 | `unsuspendUser` | PATCH | `/api/v1/admin/users/{id}/unsuspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 62 | `unsuspendUser` | PATCH | `/api/admin/users/{id}/unsuspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 67 | `deleteUser` | DELETE | `/api/v1/admin/users/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 67 | `deleteUser` | DELETE | `/api/admin/users/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 72 | `deleteUserAccount` | DELETE | `/api/v1/admin/users/{id}/account` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 72 | `deleteUserAccount` | DELETE | `/api/admin/users/{id}/account` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 77 | `roles` | GET | `/api/v1/admin/roles` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 77 | `roles` | GET | `/api/admin/roles` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 82 | `createRole` | POST | `/api/v1/admin/roles` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 82 | `createRole` | POST | `/api/admin/roles` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 87 | `updateRole` | PUT | `/api/v1/admin/roles/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 87 | `updateRole` | PUT | `/api/admin/roles/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 92 | `deleteRole` | DELETE | `/api/v1/admin/roles/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 92 | `deleteRole` | DELETE | `/api/admin/roles/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 97 | `bursaries` | GET | `/api/v1/admin/bursaries` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 97 | `bursaries` | GET | `/api/admin/bursaries` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 108 | `pendingBursaries` | GET | `/api/v1/admin/bursaries/pending` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 108 | `pendingBursaries` | GET | `/api/admin/bursaries/pending` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 113 | `reviewBursary` | PATCH | `/api/v1/admin/bursaries/{id}/review` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 113 | `reviewBursary` | PATCH | `/api/admin/bursaries/{id}/review` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 118 | `suspendBursary` | PATCH | `/api/v1/admin/bursaries/{id}/suspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 118 | `suspendBursary` | PATCH | `/api/admin/bursaries/{id}/suspend` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 123 | `reactivateBursary` | PATCH | `/api/v1/admin/bursaries/{id}/reactivate` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 123 | `reactivateBursary` | PATCH | `/api/admin/bursaries/{id}/reactivate` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 128 | `deleteBursary` | DELETE | `/api/v1/admin/bursaries/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 128 | `deleteBursary` | DELETE | `/api/admin/bursaries/{id}` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 133 | `settings` | GET | `/api/v1/admin/settings` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 133 | `settings` | GET | `/api/admin/settings` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 138 | `updateSettings` | PUT | `/api/v1/admin/settings` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 138 | `updateSettings` | PUT | `/api/admin/settings` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 143 | `bulkUpload` | POST | `/api/v1/admin/users/bulk-upload` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 143 | `bulkUpload` | POST | `/api/admin/users/bulk-upload` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 148 | `bulkUploadTemplate` | GET | `/api/v1/admin/users/bulk-upload/template` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 148 | `bulkUploadTemplate` | GET | `/api/admin/users/bulk-upload/template` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 155 | `auditLogs` | GET | `/api/v1/admin/audit-logs` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 155 | `auditLogs` | GET | `/api/admin/audit-logs` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 160 | `analytics` | GET | `/api/v1/admin/analytics` |
| `backend/src/main/java/com/edurite/admin/controller/AdminController.java` | 160 | `analytics` | GET | `/api/admin/analytics` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 44 | `careerAdvice` | POST | `/api/v1/ai/career-advice` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 44 | `careerAdvice` | POST | `/api/ai/career-advice` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 62 | `test` | GET | `/api/v1/ai/test` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 62 | `test` | GET | `/api/ai/test` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 72 | `careerAdviceForStudent` | GET | `/api/v1/ai/career-advice/me` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 72 | `careerAdviceForStudent` | GET | `/api/ai/career-advice/me` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 81 | `bursaryGuidanceForStudent` | GET | `/api/v1/ai/bursary-guidance/me` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 81 | `bursaryGuidanceForStudent` | GET | `/api/ai/bursary-guidance/me` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 86 | `dashboardSummary` | GET | `/api/v1/ai/dashboard-summary` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 86 | `dashboardSummary` | GET | `/api/ai/dashboard-summary` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 96 | `analyseUniversitySources` | POST | `/api/v1/ai/analyse-university-sources` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 96 | `analyseUniversitySources` | POST | `/api/ai/analyse-university-sources` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 119 | `defaultUniversitySources` | GET | `/api/v1/ai/default-university-sources` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 119 | `defaultUniversitySources` | GET | `/api/ai/default-university-sources` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 124 | `sourceCoverage` | GET | `/api/v1/ai/source-coverage` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 124 | `sourceCoverage` | GET | `/api/ai/source-coverage` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 129 | `geminiHealth` | GET | `/api/v1/ai/gemini-health` |
| `backend/src/main/java/com/edurite/ai/controller/AiController.java` | 129 | `geminiHealth` | GET | `/api/ai/gemini-health` |
| `backend/src/main/java/com/edurite/application/controller/ApplicationController.java` | 31 | `apply` | POST | `/api/v1/bursaries/{id}/applications` |
| `backend/src/main/java/com/edurite/application/controller/ApplicationController.java` | 31 | `apply` | POST | `/api/bursaries/{id}/applications` |
| `backend/src/main/java/com/edurite/application/controller/ApplicationController.java` | 41 | `myApplications` | GET | `/api/v1/applications/me` |
| `backend/src/main/java/com/edurite/application/controller/ApplicationController.java` | 41 | `myApplications` | GET | `/api/applications/me` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 37 | `me` | GET | `/api/v1/auth/me` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 37 | `me` | GET | `/api/auth/me` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 46 | `register` | POST | `/api/v1/auth/register` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 46 | `register` | POST | `/api/auth/register` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 51 | `registerStudent` | POST | `/api/v1/auth/register/student` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 51 | `registerStudent` | POST | `/api/auth/register/student` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 56 | `registerCompany` | POST | `/api/v1/auth/register/company` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 56 | `registerCompany` | POST | `/api/auth/register/company` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 61 | `login` | POST | `/api/v1/auth/login` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 61 | `login` | POST | `/api/auth/login` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 66 | `loginWithGoogle` | POST | `/api/v1/auth/google` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 66 | `loginWithGoogle` | POST | `/api/auth/google` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 71 | `refresh` | POST | `/api/v1/auth/refresh` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 71 | `refresh` | POST | `/api/auth/refresh` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 76 | `logout` | POST | `/api/v1/auth/logout` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 76 | `logout` | POST | `/api/auth/logout` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 81 | `verifyOtp` | POST | `/api/v1/auth/verify-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 81 | `verifyOtp` | POST | `/api/auth/verify-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 86 | `resendVerificationOtp` | POST | `/api/v1/auth/resend-verification-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 86 | `resendVerificationOtp` | POST | `/api/auth/resend-verification-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 93 | `forgotPasswordRequestOtp` | POST | `/api/v1/auth/forgot-password/request-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 93 | `forgotPasswordRequestOtp` | POST | `/api/auth/forgot-password/request-otp` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 100 | `resetPasswordWithOtp` | POST | `/api/v1/auth/forgot-password/reset` |
| `backend/src/main/java/com/edurite/auth/controller/AuthController.java` | 100 | `resetPasswordWithOtp` | POST | `/api/auth/forgot-password/reset` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 41 | `list` | GET | `/api/v1/bursaries` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 41 | `list` | GET | `/api/bursaries` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 54 | `search` | GET | `/api/v1/bursaries/search` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 54 | `search` | GET | `/api/bursaries/search` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 66 | `myRecommendations` | GET | `/api/v1/bursaries/recommendations/me` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 66 | `myRecommendations` | GET | `/api/bursaries/recommendations/me` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 71 | `get` | GET | `/api/v1/bursaries/{id}` |
| `backend/src/main/java/com/edurite/bursary/controller/BursaryController.java` | 71 | `get` | GET | `/api/bursaries/{id}` |
| `backend/src/main/java/com/edurite/career/controller/CareerController.java` | 33 | `list` | GET | `/api/v1/careers` |
| `backend/src/main/java/com/edurite/career/controller/CareerController.java` | 33 | `list` | GET | `/api/careers` |
| `backend/src/main/java/com/edurite/career/controller/CareerController.java` | 72 | `get` | GET | `/api/v1/careers/{id}` |
| `backend/src/main/java/com/edurite/career/controller/CareerController.java` | 72 | `get` | GET | `/api/careers/{id}` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 29 | `list` | GET | `/api/v1/admin/companies` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 29 | `list` | GET | `/api/admin/companies` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 38 | `pending` | GET | `/api/v1/admin/companies/pending` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 38 | `pending` | GET | `/api/admin/companies/pending` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 43 | `details` | GET | `/api/v1/admin/companies/{id}` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 43 | `details` | GET | `/api/admin/companies/{id}` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 48 | `approve` | PATCH | `/api/v1/admin/companies/{id}/approve` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 48 | `approve` | PATCH | `/api/admin/companies/{id}/approve` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 53 | `reject` | PATCH | `/api/v1/admin/companies/{id}/reject` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 53 | `reject` | PATCH | `/api/admin/companies/{id}/reject` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 58 | `moreInfo` | PATCH | `/api/v1/admin/companies/{id}/more-info` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 58 | `moreInfo` | PATCH | `/api/admin/companies/{id}/more-info` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 63 | `suspend` | PATCH | `/api/v1/admin/companies/{id}/suspend` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 63 | `suspend` | PATCH | `/api/admin/companies/{id}/suspend` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 68 | `reactivate` | PATCH | `/api/v1/admin/companies/{id}/reactivate` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 68 | `reactivate` | PATCH | `/api/admin/companies/{id}/reactivate` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 73 | `delete` | DELETE | `/api/v1/admin/companies/{id}` |
| `backend/src/main/java/com/edurite/company/controller/AdminCompanyReviewController.java` | 73 | `delete` | DELETE | `/api/admin/companies/{id}` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 40 | `me` | GET | `/api/v1/companies/me` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 40 | `me` | GET | `/api/companies/me` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 43 | `updateMe` | PUT | `/api/v1/companies/me` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 43 | `updateMe` | PUT | `/api/companies/me` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 46 | `uploadDocument` | POST | `/api/v1/companies/me/documents` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 46 | `uploadDocument` | POST | `/api/companies/me/documents` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 51 | `listDocuments` | GET | `/api/v1/companies/me/documents` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 51 | `listDocuments` | GET | `/api/companies/me/documents` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 54 | `dashboard` | GET | `/api/v1/companies/dashboard` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 54 | `dashboard` | GET | `/api/companies/dashboard` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 57 | `createBursary` | POST | `/api/v1/companies/bursaries` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 57 | `createBursary` | POST | `/api/companies/bursaries` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 62 | `updateBursary` | PUT | `/api/v1/companies/bursaries/{id}` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 62 | `updateBursary` | PUT | `/api/companies/bursaries/{id}` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 65 | `companyBursaries` | GET | `/api/v1/companies/bursaries` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 65 | `companyBursaries` | GET | `/api/companies/bursaries` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 68 | `companyBursary` | GET | `/api/v1/companies/bursaries/{id}` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 68 | `companyBursary` | GET | `/api/companies/bursaries/{id}` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 71 | `unpublishBursary` | PATCH | `/api/v1/companies/bursaries/{id}/unpublish` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 71 | `unpublishBursary` | PATCH | `/api/companies/bursaries/{id}/unpublish` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 74 | `closeBursary` | PATCH | `/api/v1/companies/bursaries/{id}/close` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 74 | `closeBursary` | PATCH | `/api/companies/bursaries/{id}/close` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 77 | `reopenBursary` | PATCH | `/api/v1/companies/bursaries/{id}/reopen` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 77 | `reopenBursary` | PATCH | `/api/companies/bursaries/{id}/reopen` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 80 | `searchStudents` | GET | `/api/v1/companies/students/search` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 80 | `searchStudents` | GET | `/api/companies/students/search` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 89 | `bookmarkStudent` | POST | `/api/v1/companies/students/{studentId}/bookmarks` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 89 | `bookmarkStudent` | POST | `/api/companies/students/{studentId}/bookmarks` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 94 | `bookmarks` | GET | `/api/v1/companies/students/bookmarks` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 94 | `bookmarks` | GET | `/api/companies/students/bookmarks` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 97 | `shortlistStudent` | POST | `/api/v1/companies/students/{studentId}/shortlists` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 97 | `shortlistStudent` | POST | `/api/companies/students/{studentId}/shortlists` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 102 | `shortlists` | GET | `/api/v1/companies/students/shortlists` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 102 | `shortlists` | GET | `/api/companies/students/shortlists` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 105 | `messageStudent` | POST | `/api/v1/companies/students/{studentId}/messages` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 105 | `messageStudent` | POST | `/api/companies/students/{studentId}/messages` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 110 | `messages` | GET | `/api/v1/companies/messages` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 110 | `messages` | GET | `/api/companies/messages` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 113 | `inviteStudent` | POST | `/api/v1/companies/students/{studentId}/invitations` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 113 | `inviteStudent` | POST | `/api/companies/students/{studentId}/invitations` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 118 | `invitations` | GET | `/api/v1/companies/invitations` |
| `backend/src/main/java/com/edurite/company/controller/CompanyController.java` | 118 | `invitations` | GET | `/api/companies/invitations` |
| `backend/src/main/java/com/edurite/compliance/controller/ComplianceAdminController.java` | 22 | `consents` | GET | `/api/v1/admin/compliance/consents/{userId}` |
| `backend/src/main/java/com/edurite/compliance/controller/ComplianceAdminController.java` | 22 | `consents` | GET | `/api/admin/compliance/consents/{userId}` |
| `backend/src/main/java/com/edurite/course/controller/CourseController.java` | 39 | `list` | GET | `/api/v1/courses` |
| `backend/src/main/java/com/edurite/course/controller/CourseController.java` | 39 | `list` | GET | `/api/courses` |
| `backend/src/main/java/com/edurite/course/controller/CourseController.java` | 58 | `details` | GET | `/api/v1/courses/{id}` |
| `backend/src/main/java/com/edurite/course/controller/CourseController.java` | 58 | `details` | GET | `/api/courses/{id}` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 25 | `get` | GET | `/api/v1/student/cv` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 25 | `get` | GET | `/api/student/cv` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 30 | `upsert` | PUT | `/api/v1/student/cv` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 30 | `upsert` | PUT | `/api/student/cv` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 35 | `suggestions` | GET | `/api/v1/student/cv/ai-suggestions` |
| `backend/src/main/java/com/edurite/cv/controller/StudentCvController.java` | 35 | `suggestions` | GET | `/api/student/cv/ai-suggestions` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 20 | `careersInsight` | GET | `/api/v1/public/discovery/careers/insight` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 20 | `careersInsight` | GET | `/api/public/discovery/careers/insight` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 31 | `coursesInsight` | GET | `/api/v1/public/discovery/courses/insight` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 31 | `coursesInsight` | GET | `/api/public/discovery/courses/insight` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 41 | `bursariesInsight` | GET | `/api/v1/public/discovery/bursaries/insight` |
| `backend/src/main/java/com/edurite/discovery/controller/PublicDiscoveryController.java` | 41 | `bursariesInsight` | GET | `/api/public/discovery/bursaries/insight` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 27 | `summary` | GET | `/api/v1/student/gamification/summary` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 27 | `summary` | GET | `/api/student/gamification/summary` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 32 | `markTaskComplete` | POST | `/api/v1/student/gamification/tasks/complete` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 32 | `markTaskComplete` | POST | `/api/student/gamification/tasks/complete` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 38 | `claimReward` | POST | `/api/v1/student/gamification/claims` |
| `backend/src/main/java/com/edurite/gamification/controller/GamificationController.java` | 38 | `claimReward` | POST | `/api/student/gamification/claims` |
| `backend/src/main/java/com/edurite/institution/controller/InstitutionController.java` | 27 | `list` | GET | `/api/v1/institutions` |
| `backend/src/main/java/com/edurite/institution/controller/InstitutionController.java` | 27 | `list` | GET | `/api/institutions` |
| `backend/src/main/java/com/edurite/institution/controller/InstitutionController.java` | 39 | `details` | GET | `/api/v1/institutions/{id}` |
| `backend/src/main/java/com/edurite/institution/controller/InstitutionController.java` | 39 | `details` | GET | `/api/institutions/{id}` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 30 | `list` | GET | `/api/v1/admin/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 30 | `list` | GET | `/api/admin/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 35 | `create` | POST | `/api/v1/admin/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 35 | `create` | POST | `/api/admin/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 40 | `update` | PUT | `/api/v1/admin/international-opportunities/{id}` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 40 | `update` | PUT | `/api/admin/international-opportunities/{id}` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 45 | `delete` | DELETE | `/api/v1/admin/international-opportunities/{id}` |
| `backend/src/main/java/com/edurite/international/controller/AdminInternationalOpportunityController.java` | 45 | `delete` | DELETE | `/api/admin/international-opportunities/{id}` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 28 | `browse` | GET | `/api/v1/student/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 28 | `browse` | GET | `/api/student/international-opportunities` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 37 | `save` | POST | `/api/v1/student/international-opportunities/{id}/save` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 37 | `save` | POST | `/api/student/international-opportunities/{id}/save` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 42 | `unsave` | DELETE | `/api/v1/student/international-opportunities/{id}/save` |
| `backend/src/main/java/com/edurite/international/controller/StudentInternationalOpportunityController.java` | 42 | `unsave` | DELETE | `/api/student/international-opportunities/{id}/save` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 28 | `list` | GET | `/api/v1/admin/internships` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 28 | `list` | GET | `/api/admin/internships` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 33 | `create` | POST | `/api/v1/admin/internships` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 33 | `create` | POST | `/api/admin/internships` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 38 | `update` | PUT | `/api/v1/admin/internships/{id}` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 38 | `update` | PUT | `/api/admin/internships/{id}` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 43 | `applicants` | GET | `/api/v1/admin/internships/{id}/applicants` |
| `backend/src/main/java/com/edurite/internship/controller/AdminInternshipController.java` | 43 | `applicants` | GET | `/api/admin/internships/{id}/applicants` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 29 | `list` | GET | `/api/v1/companies/internships` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 29 | `list` | GET | `/api/companies/internships` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 34 | `create` | POST | `/api/v1/companies/internships` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 34 | `create` | POST | `/api/companies/internships` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 39 | `update` | PUT | `/api/v1/companies/internships/{id}` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 39 | `update` | PUT | `/api/companies/internships/{id}` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 44 | `applicants` | GET | `/api/v1/companies/internships/{id}/applicants` |
| `backend/src/main/java/com/edurite/internship/controller/CompanyInternshipController.java` | 44 | `applicants` | GET | `/api/companies/internships/{id}/applicants` |
| `backend/src/main/java/com/edurite/internship/controller/StudentInternshipController.java` | 29 | `browse` | GET | `/api/v1/student/internships` |
| `backend/src/main/java/com/edurite/internship/controller/StudentInternshipController.java` | 29 | `browse` | GET | `/api/student/internships` |
| `backend/src/main/java/com/edurite/internship/controller/StudentInternshipController.java` | 39 | `interest` | POST | `/api/v1/student/internships/{opportunityId}/interest` |
| `backend/src/main/java/com/edurite/internship/controller/StudentInternshipController.java` | 39 | `interest` | POST | `/api/student/internships/{opportunityId}/interest` |
| `backend/src/main/java/com/edurite/learning/controller/LearningCentreController.java` | 22 | `catalogue` | GET | `/api/v1/student/learning-centre/catalogue` |
| `backend/src/main/java/com/edurite/learning/controller/LearningCentreController.java` | 22 | `catalogue` | GET | `/api/student/learning-centre/catalogue` |
| `backend/src/main/java/com/edurite/learning/controller/LearningCentreController.java` | 27 | `recommended` | GET | `/api/v1/student/learning-centre/recommended` |
| `backend/src/main/java/com/edurite/learning/controller/LearningCentreController.java` | 27 | `recommended` | GET | `/api/student/learning-centre/recommended` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 27 | `mentors` | GET | `/api/v1/admin/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 27 | `mentors` | GET | `/api/admin/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 32 | `create` | POST | `/api/v1/admin/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 32 | `create` | POST | `/api/admin/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 37 | `update` | PUT | `/api/v1/admin/mentors/{id}` |
| `backend/src/main/java/com/edurite/mentorship/controller/AdminMentorController.java` | 37 | `update` | PUT | `/api/admin/mentors/{id}` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 27 | `mentors` | GET | `/api/v1/student/mentorship/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 27 | `mentors` | GET | `/api/student/mentorship/mentors` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 32 | `requests` | GET | `/api/v1/student/mentorship/requests` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 32 | `requests` | GET | `/api/student/mentorship/requests` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 37 | `requestSession` | POST | `/api/v1/student/mentorship/requests` |
| `backend/src/main/java/com/edurite/mentorship/controller/MentorshipController.java` | 37 | `requestSession` | POST | `/api/student/mentorship/requests` |
| `backend/src/main/java/com/edurite/notification/controller/NotificationController.java` | 29 | `mine` | GET | `/api/v1/notifications` |
| `backend/src/main/java/com/edurite/notification/controller/NotificationController.java` | 29 | `mine` | GET | `/api/notifications` |
| `backend/src/main/java/com/edurite/notification/controller/NotificationController.java` | 40 | `markRead` | PATCH | `/api/v1/notifications/{id}/read` |
| `backend/src/main/java/com/edurite/notification/controller/NotificationController.java` | 40 | `markRead` | PATCH | `/api/notifications/{id}/read` |
| `backend/src/main/java/com/edurite/progress/controller/ProgressScoreController.java` | 20 | `score` | GET | `/api/v1/student/progress-score` |
| `backend/src/main/java/com/edurite/progress/controller/ProgressScoreController.java` | 20 | `score` | GET | `/api/student/progress-score` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 32 | `studentAssessments` | GET | `/api/v1/student/psychometric/assessments` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 32 | `studentAssessments` | GET | `/api/student/psychometric/assessments` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 37 | `studentAssessmentQuestions` | GET | `/api/v1/student/psychometric/assessments/{assessmentId}/questions` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 37 | `studentAssessmentQuestions` | GET | `/api/student/psychometric/assessments/{assessmentId}/questions` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 42 | `submitAssessmentAttempt` | POST | `/api/v1/student/psychometric/assessments/{assessmentId}/attempts` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 42 | `submitAssessmentAttempt` | POST | `/api/student/psychometric/assessments/{assessmentId}/attempts` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 51 | `attemptHistory` | GET | `/api/v1/student/psychometric/assessments/{assessmentId}/attempts` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 51 | `attemptHistory` | GET | `/api/student/psychometric/assessments/{assessmentId}/attempts` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 59 | `submitForStudent` | POST | `/api/v1/student/psychometric/submit` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 59 | `submitForStudent` | POST | `/api/student/psychometric/submit` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 67 | `latestForStudent` | GET | `/api/v1/student/psychometric/latest` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 67 | `latestForStudent` | GET | `/api/student/psychometric/latest` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 72 | `submitPublic` | POST | `/api/v1/public/psychometric/submit` |
| `backend/src/main/java/com/edurite/psychometric/controller/PsychometricController.java` | 72 | `submitPublic` | POST | `/api/public/psychometric/submit` |
| `backend/src/main/java/com/edurite/recommendation/controller/RecommendationController.java` | 28 | `myRecommendations` | GET | `/api/v1/recommendations/me` |
| `backend/src/main/java/com/edurite/recommendation/controller/RecommendationController.java` | 28 | `myRecommendations` | GET | `/api/recommendations/me` |
| `backend/src/main/java/com/edurite/roadmap/controller/CareerRoadmapController.java` | 21 | `list` | GET | `/api/v1/student/career-roadmaps` |
| `backend/src/main/java/com/edurite/roadmap/controller/CareerRoadmapController.java` | 21 | `list` | GET | `/api/student/career-roadmaps` |
| `backend/src/main/java/com/edurite/roadmap/controller/CareerRoadmapController.java` | 26 | `detail` | GET | `/api/v1/student/career-roadmaps/{slug}` |
| `backend/src/main/java/com/edurite/roadmap/controller/CareerRoadmapController.java` | 26 | `detail` | GET | `/api/student/career-roadmaps/{slug}` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 32 | `list` | GET | `/api/v1/student/scholarship-applications` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 32 | `list` | GET | `/api/student/scholarship-applications` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 37 | `upcoming` | GET | `/api/v1/student/scholarship-applications/upcoming` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 37 | `upcoming` | GET | `/api/student/scholarship-applications/upcoming` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 42 | `create` | POST | `/api/v1/student/scholarship-applications` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 42 | `create` | POST | `/api/student/scholarship-applications` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 47 | `update` | PUT | `/api/v1/student/scholarship-applications/{id}` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 47 | `update` | PUT | `/api/student/scholarship-applications/{id}` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 52 | `delete` | DELETE | `/api/v1/student/scholarship-applications/{id}` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 52 | `delete` | DELETE | `/api/student/scholarship-applications/{id}` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 58 | `motivationLetter` | POST | `/api/v1/student/scholarship-applications/{id}/motivation-letter` |
| `backend/src/main/java/com/edurite/scholarship/controller/ScholarshipApplicationController.java` | 58 | `motivationLetter` | POST | `/api/student/scholarship-applications/{id}/motivation-letter` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 29 | `list` | GET | `/api/v1/admin/schools` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 29 | `list` | GET | `/api/admin/schools` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 34 | `create` | POST | `/api/v1/admin/schools` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 34 | `create` | POST | `/api/admin/schools` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 39 | `update` | PUT | `/api/v1/admin/schools/{schoolId}` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 39 | `update` | PUT | `/api/admin/schools/{schoolId}` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 44 | `linkStudent` | POST | `/api/v1/admin/schools/{schoolId}/students` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 44 | `linkStudent` | POST | `/api/admin/schools/{schoolId}/students` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 49 | `summary` | GET | `/api/v1/admin/schools/{schoolId}/summary` |
| `backend/src/main/java/com/edurite/school/controller/SchoolPortalController.java` | 49 | `summary` | GET | `/api/admin/schools/{schoolId}/summary` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 48 | `profile` | GET | `/api/v1/student/profile` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 48 | `profile` | GET | `/api/student/profile` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 58 | `upsert` | PUT | `/api/v1/student/profile` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 58 | `upsert` | PUT | `/api/student/profile` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 67 | `savedProfiles` | GET | `/api/v1/student/profile/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 67 | `savedProfiles` | GET | `/api/student/profile/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 72 | `saveProfileVersion` | POST | `/api/v1/student/profile/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 72 | `saveProfileVersion` | POST | `/api/student/profile/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 80 | `savedProfileDetails` | GET | `/api/v1/student/profile/saved/{savedProfileId}` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 80 | `savedProfileDetails` | GET | `/api/student/profile/saved/{savedProfileId}` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 85 | `applySavedProfile` | POST | `/api/v1/student/profile/saved/{savedProfileId}/apply` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 85 | `applySavedProfile` | POST | `/api/student/profile/saved/{savedProfileId}/apply` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 90 | `deleteSavedProfile` | DELETE | `/api/v1/student/profile/saved/{savedProfileId}` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 90 | `deleteSavedProfile` | DELETE | `/api/student/profile/saved/{savedProfileId}` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 97 | `uploadCv` | POST | `/api/v1/student/profile/cv` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 97 | `uploadCv` | POST | `/api/student/profile/cv` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 107 | `uploadTranscript` | POST | `/api/v1/student/profile/transcript` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 107 | `uploadTranscript` | POST | `/api/student/profile/transcript` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 117 | `dashboard` | GET | `/api/v1/student/dashboard` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 117 | `dashboard` | GET | `/api/student/dashboard` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 127 | `settings` | GET | `/api/v1/student/settings` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 127 | `settings` | GET | `/api/student/settings` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 137 | `updateSettings` | PUT | `/api/v1/student/settings` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 137 | `updateSettings` | PUT | `/api/student/settings` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 147 | `saveCareer` | POST | `/api/v1/student/careers/{careerId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 147 | `saveCareer` | POST | `/api/student/careers/{careerId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 158 | `saveBursary` | POST | `/api/v1/student/bursaries/{bursaryId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 158 | `saveBursary` | POST | `/api/student/bursaries/{bursaryId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 169 | `unsaveCareer` | DELETE | `/api/v1/student/careers/{careerId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 169 | `unsaveCareer` | DELETE | `/api/student/careers/{careerId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 180 | `unsaveBursary` | DELETE | `/api/v1/student/bursaries/{bursaryId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 180 | `unsaveBursary` | DELETE | `/api/student/bursaries/{bursaryId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 191 | `savedCareers` | GET | `/api/v1/student/careers/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 191 | `savedCareers` | GET | `/api/student/careers/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 201 | `bookmarkedBursaries` | GET | `/api/v1/student/bursaries/bookmarks` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 201 | `bookmarkedBursaries` | GET | `/api/student/bursaries/bookmarks` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 206 | `savedBursaries` | GET | `/api/v1/student/bursaries/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 206 | `savedBursaries` | GET | `/api/student/bursaries/saved` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 215 | `preferences` | GET | `/api/v1/student/preferences` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 215 | `preferences` | GET | `/api/student/preferences` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 220 | `updatePreferences` | PUT | `/api/v1/student/preferences` |
| `backend/src/main/java/com/edurite/student/controller/StudentController.java` | 220 | `updatePreferences` | PUT | `/api/student/preferences` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 29 | `list` | GET | `/api/v1/student/opportunities` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 29 | `list` | GET | `/api/student/opportunities` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 43 | `save` | POST | `/api/v1/student/opportunities/{type}/{opportunityId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 43 | `save` | POST | `/api/student/opportunities/{type}/{opportunityId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 54 | `unsave` | DELETE | `/api/v1/student/opportunities/{type}/{opportunityId}/save` |
| `backend/src/main/java/com/edurite/student/controller/StudentOpportunityController.java` | 54 | `unsave` | DELETE | `/api/student/opportunities/{type}/{opportunityId}/save` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 33 | `initiate` | POST | `/api/v1/payments/payfast/initiate` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 33 | `initiate` | POST | `/api/payments/payfast/initiate` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 41 | `notify` | POST | `/api/v1/payments/payfast/notify` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 41 | `notify` | POST | `/api/payments/payfast/notify` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 55 | `onReturn` | GET | `/api/v1/payments/payfast/return` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 55 | `onReturn` | GET | `/api/payments/payfast/return` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 63 | `onCancel` | GET | `/api/v1/payments/payfast/cancel` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 63 | `onCancel` | GET | `/api/payments/payfast/cancel` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 71 | `status` | GET | `/api/v1/payments/payfast/status` |
| `backend/src/main/java/com/edurite/subscription/controller/PayFastController.java` | 71 | `status` | GET | `/api/payments/payfast/status` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 26 | `callback` | POST | `/api/v1/payments/callback` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 26 | `callback` | POST | `/api/payments/callback` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 41 | `callbackGet` | GET | `/api/v1/payments/callback` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 41 | `callbackGet` | GET | `/api/payments/callback` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 46 | `providerCallbackPost` | POST | `/api/v1/payments/callbacks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 46 | `providerCallbackPost` | POST | `/api/payments/callbacks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 62 | `providerCallbackGet` | GET | `/api/v1/payments/callbacks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 62 | `providerCallbackGet` | GET | `/api/payments/callbacks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 70 | `providerWebhook` | POST | `/api/v1/payments/webhooks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/PaymentController.java` | 70 | `providerWebhook` | POST | `/api/payments/webhooks/{provider}` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 37 | `current` | GET | `/api/v1/subscriptions/me` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 37 | `current` | GET | `/api/subscriptions/me` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 46 | `plans` | GET | `/api/v1/subscriptions/plans` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 46 | `plans` | GET | `/api/subscriptions/plans` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 51 | `checkout` | POST | `/api/v1/subscriptions/checkout` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 51 | `checkout` | POST | `/api/subscriptions/checkout` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 59 | `confirm` | POST | `/api/v1/subscriptions/confirm` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 59 | `confirm` | POST | `/api/subscriptions/confirm` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 67 | `cancel` | POST | `/api/v1/subscriptions/cancel` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 67 | `cancel` | POST | `/api/subscriptions/cancel` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 75 | `verify` | POST | `/api/v1/subscriptions/verify` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 75 | `verify` | POST | `/api/subscriptions/verify` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 84 | `purchase` | POST | `/api/v1/subscriptions/purchase` |
| `backend/src/main/java/com/edurite/subscription/controller/SubscriptionController.java` | 84 | `purchase` | POST | `/api/subscriptions/purchase` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 29 | `sessions` | GET | `/api/v1/student/tutor/sessions` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 29 | `sessions` | GET | `/api/student/tutor/sessions` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 34 | `createSession` | POST | `/api/v1/student/tutor/sessions` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 34 | `createSession` | POST | `/api/student/tutor/sessions` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 39 | `session` | GET | `/api/v1/student/tutor/sessions/{sessionId}` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 39 | `session` | GET | `/api/student/tutor/sessions/{sessionId}` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 44 | `ask` | POST | `/api/v1/student/tutor/ask` |
| `backend/src/main/java/com/edurite/tutor/controller/TutorController.java` | 44 | `ask` | POST | `/api/student/tutor/ask` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 31 | `list` | GET | `/api/v1/student/university-applications` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 31 | `list` | GET | `/api/student/university-applications` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 36 | `create` | POST | `/api/v1/student/university-applications` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 36 | `create` | POST | `/api/student/university-applications` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 41 | `update` | PUT | `/api/v1/student/university-applications/{id}` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 41 | `update` | PUT | `/api/student/university-applications/{id}` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 46 | `delete` | DELETE | `/api/v1/student/university-applications/{id}` |
| `backend/src/main/java/com/edurite/universityapplication/controller/UniversityApplicationController.java` | 46 | `delete` | DELETE | `/api/student/university-applications/{id}` |
| `backend/src/main/java/com/edurite/whatsapp/controller/WhatsAppWebhookController.java` | 38 | `verify` | GET | `/api/v1/webhooks/whatsapp` |
| `backend/src/main/java/com/edurite/whatsapp/controller/WhatsAppWebhookController.java` | 38 | `verify` | GET | `/api/webhooks/whatsapp` |
| `backend/src/main/java/com/edurite/whatsapp/controller/WhatsAppWebhookController.java` | 53 | `receive` | POST | `/api/v1/webhooks/whatsapp` |
| `backend/src/main/java/com/edurite/whatsapp/controller/WhatsAppWebhookController.java` | 53 | `receive` | POST | `/api/webhooks/whatsapp` |

## Mismatches Found Before Fixes

| Issue | Evidence | Resolution |
|---|---|---|
| WRONG BASE PATH | Frontend defaulted to `/api`, while most controllers only declared `/api/v1`; nginx/Vite rewrites were required for those calls to work. | Added `/api` aliases while preserving `/api/v1`, and made nginx/Vite preserve `/api`. |
| IMPLEMENTED BUT FAILING | `/api/student/dashboard`, `/api/student/profile`, `/api/student/progress-score`, and `/api/recommendations/me` were implemented under `/api/v1/...` but not directly under `/api/...`. | Added direct mappings and route tests for both path styles. |
| 500 instead of 404 | Spring `NoResourceFoundException` for missing `/api/...` routes was caught by the generic exception handler. | Added explicit 404 handling for `NoResourceFoundException` and `NoHandlerFoundException`. |
| DATA DEFAULT | Student module services using `StudentContextService.requireStudent` could fail when an authenticated student had no profile row. | `StudentContextService` now creates the same safe default profile shape used by `StudentService`. |
| OPTIONAL ENRICHMENT | Recommendations could fail if psychometric growth-area loading threw due optional stored data. | Recommendation generation now logs and skips psychometric growth areas on optional-data errors. |
