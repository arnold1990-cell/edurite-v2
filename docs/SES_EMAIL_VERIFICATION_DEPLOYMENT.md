# SES email verification deployment

Run the following Bash commands from the reviewed release checkout on EC2. No deployment was performed during this change.

Local validation (2026-10-07): Java 21 Maven `clean verify` completed with BUILD SUCCESS; 332 tests reported, zero failures/errors, 10 skipped because Docker/database integration prerequisites were unavailable. All five SES service tests and all ten auth/security MVC tests passed. The Docker image build was attempted but could not connect to the local Docker daemon. Production SES/IMDS access has not been exercised by this checkout; complete the gates below before deployment.

## Configuration and prerequisites

The backend uses SES v2 2.38.4, region `af-south-1`, sender `info@edurite.co.za`, and links under `https://edurite.co.za/verify-email`. Set these three non-secret values in the production `.env`:

```dotenv
APP_EMAIL_VERIFICATION_AWS_REGION=af-south-1
APP_EMAIL_VERIFICATION_FROM=info@edurite.co.za
APP_EMAIL_VERIFICATION_BASE_URL=https://edurite.co.za
```

Include `https://edurite.co.za` in the existing `APP_CORS_ALLOWED_ORIGINS` if the frontend calls the API across origins. Do not replace other required origins. Do not print `.env` or expanded Compose configuration. Do not enable AWS HTTP wire/debug logging or request-body logging: the SES request contains the verification link. No SMTP configuration or mail dependency remains necessary for this feature; the repository had no other SMTP sender.

Attach `EduRiteC2Role` to the EC2 instance and allow `ses:SendEmail` for the verified SES identity in `af-south-1`. Confirm SES production access, sending quota, identity status and recipient suppression status. API acceptance is not a guarantee of inbox delivery.

The Spring-managed SDK client explicitly uses `DefaultCredentialsProvider`. Do not set static AWS credentials or mount an AWS credentials file. Earlier providers in the default chain can override the instance role. Ensure IMDS is not disabled via SDK environment/system properties, and that container networking/firewall/proxy settings permit `169.254.169.254` (bypass proxies for metadata) and outbound HTTPS to SES.

Host CLI success alone does not verify container access. AWS recommends IMDSv2 with a response hop limit of 2 for containers. Check the instance's metadata options in EC2: endpoint enabled, tokens required, response hop limit 2. If a change is needed, have an authorized operator apply it to the confirmed instance; do not weaken IMDSv2 or introduce keys.

Sources: [Java default credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html), [EC2 metadata options](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-options.html).

## Build and deployment

First run the complete Maven checks with Java 21 and a working Docker daemon (for Testcontainers). The temporary Maven container receives no production environment file:

```bash
docker run --rm --mount type=bind,src="$PWD/backend",dst=/app \
  --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock \
  --add-host=host.docker.internal:host-gateway \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /app maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
docker compose config --quiet
docker compose build backend
```

Stop if any required checks fail or integration tests are unexpectedly skipped. The existing Dockerfile uses Java 21 and packages with `-DskipTests`, so the separate test gate is required. Ensure normal database backups exist before release startup (the existing application runs Flyway; this change adds no migration).

Before replacing the running backend, check IMDS from a one-off container using the new image and the backend Compose network. This overrides the entrypoint, starts no application, and only reads the role name; it does not fetch or print credentials:

```bash
docker compose run --rm --no-deps --entrypoint sh backend -ec '
  metadata=http://169.254.169.254
  token=$(curl --noproxy "*" -fsS --connect-timeout 2 --max-time 5 -X PUT \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 60" "$metadata/latest/api/token")
  role=$(printf "header = \"X-aws-ec2-metadata-token: %s\"\n" "$token" | \
    curl --config - --noproxy "*" -fsS --connect-timeout 2 --max-time 5 \
    "$metadata/latest/meta-data/iam/security-credentials/")
  unset token
  test "$role" = EduRiteC2Role
  printf "IMDSv2 reachable; expected EC2 role attached\n"
'
```

Only after these gates pass, deploy the backend:

```bash
docker compose up -d --no-deps backend
docker compose ps backend
docker compose exec -T backend curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

If the frontend nginx holds the old backend IP, validate and reload its configuration after backend readiness (no Compose restart):

```bash
docker compose exec -T frontend nginx -t
docker compose exec -T frontend nginx -s reload
```

## Post-deployment checks

Health, including through the public reverse proxy:

```bash
docker compose exec -T backend curl -fsS http://127.0.0.1:8080/actuator/health/liveness
docker compose exec -T backend curl -fsS http://127.0.0.1:8080/actuator/health/readiness
curl -fsS https://edurite.co.za/actuator/health
```

If the public proxy intentionally does not expose actuator, use the two internal checks; a public 404 alone is not a backend health failure.

Use an existing, unverified, active test account you control. This resend command intentionally sends email and updates that account's verification fields; do not use an arbitrary customer's address. Unknown/already-verified accounts also return the generic success response, and requests within 60 seconds do not send again.

```bash
read -r -p 'Unverified test account email: ' TEST_EMAIL
printf '{"email":"%s"}' "$TEST_EMAIL" | curl -fsS \
  -H 'Content-Type: application/json' --data-binary @- \
  https://edurite.co.za/api/v1/auth/email-verification/resend
docker compose logs --since 10m --no-color backend | grep 'Email verification SES'
```

Expected log: `Email verification SES accepted`. Rejections log only HTTP status, AWS error code and request ID. Client failures point to role/IMDS/network checks without logging exception messages or credentials. An acceptance log precedes the database commit; also check the fields below.

Read-only verification field query (no hash or token is printed):

```bash
docker compose exec -T -e TEST_EMAIL="$TEST_EMAIL" postgres sh -c \
  'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v test_email="$TEST_EMAIL"' <<'SQL'
BEGIN READ ONLY;
SELECT email_verified, email_verification_required,
       email_verification_hash IS NOT NULL AS has_verification_hash,
       length(email_verification_hash) AS verification_hash_length,
       email_verification_sent_at, email_verification_expires_at,
       email_verification_expires_at - email_verification_sent_at AS validity
FROM users WHERE lower(email) = lower(:'test_email');
COMMIT;
SQL
```

Before clicking: required=true, verified=false, hash length=64, validity=00:30:00. Open the received link privately in the frontend and repeat the query: required=false, verified=true, hash/expiry null; sent timestamp remains as send history. A second use must fail. Both POST endpoint aliases remain supported: `/api/v1/auth/email-verification/{resend,verify}` and `/api/auth/email-verification/{resend,verify}`. Verify JSON is `{"email":"…","token":"…"}`; do not paste live tokens into logs, shell history or support messages.

SES sends and database commits are not atomic: an email may be accepted before a later database failure. A resend after the throttle interval recovers that existing limitation. No production database data was changed during implementation.
