# Password reset using Amazon SES: review report

Status: implemented locally and tested; awaiting review and deployment approval. No commit, deployment, production credential/infrastructure change, user deletion, subscription removal, role change, table drop, or database reset was performed. PostgreSQL checks created new fixtures in separate local QA databases on port 55439; existing databases and records were preserved.

## Root cause and implementation

The original `AuthService.requestPasswordResetOtp()` resolved an email/username/phone identifier, then required `users.phone_number` and called Twilio Verify SMS. Missing phone numbers produced "Account exists, but no phone number is registered." The original `resetPasswordWithOtp()` also looked up the account by phone. The frontend accepted email initially but demanded a phone number to complete recovery.

Both original methods now use normalized email and the SES-backed recovery service. `ForgotPasswordOtpRequest` requires email; `ResetPasswordWithOtpRequest` requires email, a six-digit code, and the new password. The former phone/username recovery lookup helpers were removed. Twilio remains available for existing registration phone verification and authenticated password-change functionality; school EMIS/mobile recovery is preserved separately.

Existing registration email links, confirmation, and resend continue to work. This follow-up did not replace the registration implementation. Existing roles, approval state, verification state, subscriptions, and passwords are not changed by the migrations.

## API and frontend

All endpoints support `/api/v1/auth` and `/api/auth` aliases:

| Path after auth prefix | Request | Behavior |
| --- | --- | --- |
| `/forgot-password/request-otp` | `email` | Original endpoint; generic response, asynchronous lookup and SES send |
| `/forgot-password/reset` | `email`, `code`, `newPassword` | Original endpoint; atomically validates/consumes OTP, hashes password, revokes prior tokens |
| `/forgot-password/email/request-otp` | `email` | Same recovery service, retained alias from earlier implementation |
| `/forgot-password/email/verify-otp` | `email`, `code` | Staged frontend step; consumes OTP and returns an opaque reset credential |
| `/forgot-password/email/reset` | `email`, `resetToken`, `newPassword`, `confirmPassword` | Completes staged reset; validates and consumes reset credential |

The branded forgot-password page now sends email through the original request endpoint. The responsive form flow is email -> send code -> check email -> six-digit code -> verification -> new/confirm password -> success -> portal login. Resend is available with a countdown. OTPs and reset credentials stay in component memory, outside URLs and persistent storage. Successful reset clears cached client authentication, so revoked credentials cannot trigger a redirect away from login; rejected resets preserve the current client session.

## Security and session revocation

- SecureRandom generates six ASCII digits, independently of the server locale.
- Only a BCrypt OTP hash is stored; expiry is 10 minutes.
- Dedicated password-recovery storage binds the OTP to its user and purpose. Registration links cannot authorize a password reset.
- Five failed comparisons exhaust the OTP. Failed-attempt transactions commit their counters, including through the original reset endpoint.
- Successful verification consumes the OTP. The staged reset credential uses 32 random bytes, a SHA-256 stored hash, a 10-minute expiry, and one-time consumption.
- User-row pessimistic locks serialize resend, verification, and reset completion.
- Passwords use the existing PasswordEncoder. DTO validation enforces the BCrypt 72-byte input limit.
- Database-backed request limits allow one request per minute and five per hour per account, including SES failures. The existing Redis IP rate rules cover recovery; their existing enable/fail-open configuration is unchanged.
- Request responses do not reveal account existence. Account lookup/delivery run asynchronously; invalid verification paths perform dummy BCrypt work to reduce timing differences. Public OTP errors are generic.
- SES/client failures log safe status/context only. Codes, request bodies, links, passwords, credentials, and Twilio response bodies are not logged. Failed sends preserve the last accepted code while counting toward throttling.

Authentication is stateless JWT. An additive `users.token_version` defaults to zero; successful email reset increments it in the same transaction as the password update. All JWT issuance overloads include this version. The normal authentication filter receives the current version through `VersionedUserDetails`; refresh validation uses the same persisted value. Earlier access and refresh tokens are rejected after reset. Legacy JWTs without a version claim remain valid at version zero and become invalid after that account resets. New login tokens remain valid. Revocation applies on subsequent authentication/refresh checks; it does not terminate requests already executing.

## Database migrations

- `V81__email_password_recovery.sql`: creates only the dedicated `email_password_resets` table with its unique user association, OTP/token hashes, expiry, attempt count, and throttle fields.
- `V82__password_recovery_token_revocation.sql`: adds `users.token_version BIGINT NOT NULL DEFAULT 0`.

Both are additive. Neither updates/deletes existing passwords, users, roles, verification flags, or subscriptions. Apply through the reviewed migration process before starting the updated backend. No existing application database was migrated during this task.

## Amazon SES findings and permissions

Delivery uses the existing AWS SDK `SesV2Client` and SES v2 SendEmail API, not SMTP. Existing `EmailVerificationConfiguration` is reused without credential or infrastructure changes.

| Setting | Existing default | Override |
| --- | --- | --- |
| Region | `af-south-1` | `APP_EMAIL_VERIFICATION_AWS_REGION` |
| Sender | `info@edurite.co.za` | `APP_EMAIL_VERIFICATION_FROM` |
| Verification-link origin | `https://edurite.co.za` | `APP_EMAIL_VERIFICATION_BASE_URL` |

The existing DefaultCredentialsProvider supports workload IAM roles and the standard AWS credential chain. No AWS keys are hardcoded or exposed to React.

The runtime role needs **`ses:SendEmail`**, scoped to the verified sender email/domain identity in `af-south-1`; a `ses:FromAddress` condition can restrict sending to `info@edurite.co.za`. This simple-message implementation does not call SendRawEmail or require SES administration permissions. See the [AWS SES v2 authorization reference](https://docs.aws.amazon.com/service-authorization/latest/reference/list_sesv2.html) and [SendEmail API](https://docs.aws.amazon.com/ses/latest/APIReference-V2/API_SendEmail.html).

Confirm that the sender identity is verified in the selected region. If the account remains in the SES sandbox, recipient restrictions apply; live user delivery requires appropriate production access. See [AWS sandbox restrictions](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html). The deployed role, environment overrides, sender identity, and SES sandbox status were not changed or independently verified.

## Validation

Final selected backend suites:

- `AuthServiceVerificationFlowUnitTest`: original email endpoints, all 11 tested roles, accounts with/without phones, new-password login, old-password rejection, old-refresh-token rejection, and existing authentication regressions.
- `EmailPasswordRecoveryServiceTest`: valid/invalid/expired/reused codes and tokens, unknown accounts, resend limits, SES API/client failures, purpose separation, generic rejection work, and committing failed attempts.
- `EmailRecoveryPostgresTest`: applies actual V81/V82 SQL against fresh fixture tables, validates JPA mappings, checks persistent failed-attempt counters through direct OTP reset, and confirms exactly one concurrent successful reset and token-version increment.
- `JwtPasswordResetRevocationTest`: old access/refresh rejection, legacy token compatibility, newly issued tokens, issuance overloads, and the actual authentication filter.
- `EmailVerificationServiceTest`, `AuthServiceSchoolRegistrationTest`, `TwilioOtpServiceTest`, `AuthServiceNoMailDependencyTest`, and `AuthAndAiSecurityWebMvcTest`: registration verification/resend/expiry, role/approval preservation, existing authentication, public aliases, and email-only request validation.

Results:

- **76 backend/PostgreSQL tests passed**, zero failures/errors/skips. Log: `build/email-reset-final-tests.log`; reports: `backend/target/surefire-reports/`.
- **158 frontend tests passed** in 22 suites. Includes the recovery forms and request/verify/reset API contract, plus clearing client authentication only after successful reset. Log: `build/email-reset-frontend-tests.log`.
- **TypeScript and Vite production build passed**. Log: `build/email-reset-frontend-build.log`. Existing Browserslist-age and bundle-size warnings remain.
- **Frontend UTF-8 validation and `git diff --check` passed**.

Commands used:

```powershell
$env:RECOVERY_TEST_JDBC_URL='jdbc:postgresql://127.0.0.1:55439/recovery_qa_finalreview'
mvn -f backend/pom.xml '-Dtest=EmailRecoveryPostgresTest,EmailPasswordRecoveryServiceTest,EmailVerificationServiceTest,AuthServiceVerificationFlowUnitTest,AuthServiceSchoolRegistrationTest,TwilioOtpServiceTest,AuthServiceNoMailDependencyTest,AuthAndAiSecurityWebMvcTest,JwtPasswordResetRevocationTest' test
npm --prefix frontend test -- --run
npm --prefix frontend run build
npm --prefix frontend run validate:encoding
```

The dedicated PostgreSQL QA cluster has been stopped. Its newly created test records and files were retained; nothing was dropped or reset.

The PostgreSQL test is opt-in through `RECOVERY_TEST_JDBC_URL`, restricted to fresh local `recovery_qa` or `recovery_qa_<suffix>` databases on `127.0.0.1:55439`. It deliberately does not clean/drop existing tables or records. SES is mocked in automated tests; no production emails or account-password changes were made.

## Files changed

- `backend/src/main/java/com/edurite/auth/controller/EmailPasswordRecoveryController.java`
- `backend/src/main/java/com/edurite/auth/dto/ForgotPasswordOtpRequest.java`
- `backend/src/main/java/com/edurite/auth/dto/ResetPasswordWithOtpRequest.java`
- `backend/src/main/java/com/edurite/auth/entity/EmailPasswordReset.java`
- `backend/src/main/java/com/edurite/auth/repository/EmailPasswordResetRepository.java`
- `backend/src/main/java/com/edurite/auth/service/AuthService.java`
- `backend/src/main/java/com/edurite/auth/service/EmailPasswordRecoveryService.java`
- `backend/src/main/java/com/edurite/auth/service/EmailVerificationService.java`
- `backend/src/main/java/com/edurite/auth/service/TwilioOtpService.java`
- `backend/src/main/java/com/edurite/security/service/CustomUserDetailsService.java`
- `backend/src/main/java/com/edurite/security/service/JwtService.java`
- `backend/src/main/java/com/edurite/security/service/VersionedUserDetails.java`
- `backend/src/main/java/com/edurite/user/entity/User.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V81__email_password_recovery.sql`
- `backend/src/main/resources/db/migration/V82__password_recovery_token_revocation.sql`
- `backend/src/test/java/com/edurite/auth/AuthServiceVerificationFlowUnitTest.java`
- `backend/src/test/java/com/edurite/auth/service/AuthServiceSchoolRegistrationTest.java`
- `backend/src/test/java/com/edurite/auth/service/EmailPasswordRecoveryServiceTest.java`
- `backend/src/test/java/com/edurite/auth/service/EmailRecoveryPostgresTest.java`
- `backend/src/test/java/com/edurite/auth/service/EmailVerificationServiceTest.java`
- `backend/src/test/java/com/edurite/security/config/AuthAndAiSecurityWebMvcTest.java`
- `backend/src/test/java/com/edurite/security/service/JwtPasswordResetRevocationTest.java`
- `docs/email-recovery-verification-review.md`
- `frontend/src/pages/public/AuthPages.tsx`
- `frontend/src/pages/public/PasswordRecovery.test.tsx`
- `frontend/src/services/authService.recovery.test.ts`
- `frontend/src/services/authService.ts`

The file list includes the earlier implementation and this follow-up. Pre-existing `.cicd-backup/` and `docs/v80-deployment-investigation.md` were untouched. `OtpService`, `EmailVerificationConfiguration`, controllers/repositories, and existing migrations were inspected; no change was needed to the SES client configuration or Twilio interface.

## Deployment readiness

Ready for code review; automated validation passed. No deployment is authorized or performed. Apply V81/V82 first, then coordinate backend/frontend release; all backend instances must enforce token versions for consistent revocation. The original recovery API now requires email instead of phone/accountIdentifier, so any external callers must use the new request shape.

Staging still needs a live SES inbox-delivery check using the actual runtime role, verified sender, region, and configured link origin. Browser/mobile visual QA remains outstanding because no browser is connected in this session. Static rendering, API-contract tests, and the production build do not replace that visual check. The unrelated full backend suite was not run.
