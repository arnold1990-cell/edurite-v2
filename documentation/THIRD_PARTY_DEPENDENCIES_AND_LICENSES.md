# Third-Party Dependencies and Licenses

## Scope Inspected
- `frontend/package.json`
- `backend/pom.xml`
- `docker-compose.yml`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `backend/src/main/resources/application*.yml`
- root `.env` (keys only; secret values intentionally excluded)

## npm Dependencies (frontend)
| Dependency | Purpose | Possible License | Commercial Model |
|---|---|---|---|
| react, react-dom | SPA UI runtime | MIT | Open-source |
| react-router-dom | Client-side routing | MIT | Open-source |
| @tanstack/react-query | Data fetching/cache | MIT | Open-source |
| axios | HTTP client | MIT | Open-source |
| react-hook-form | Form handling | MIT | Open-source |
| @hookform/resolvers | Form resolver adapters | MIT | Open-source |
| zod | Runtime schema validation | MIT | Open-source |
| framer-motion | Motion/animation | MIT | Open-source |
| lucide-react | Icon set | ISC | Open-source |
| tailwindcss | Utility CSS framework | MIT | Open-source |
| vite, @vitejs/plugin-react | Frontend build/dev tooling | MIT | Open-source |
| typescript | Static typing | Apache-2.0 | Open-source |
| postcss, autoprefixer | CSS processing | MIT | Open-source |
| @types/node, @types/react, @types/react-dom | Type definitions | MIT | Open-source |

## Maven Dependencies (backend)
| Dependency | Purpose | Possible License | Commercial Model |
|---|---|---|---|
| spring-boot-starter-web | REST API runtime | Apache-2.0 | Open-source |
| spring-boot-starter-security | Authentication/authorization | Apache-2.0 | Open-source |
| spring-boot-starter-data-jpa | ORM/repository support | Apache-2.0 | Open-source |
| spring-boot-starter-data-redis | Redis integration | Apache-2.0 | Open-source |
| spring-boot-starter-validation | Bean validation | Apache-2.0 | Open-source |
| spring-boot-starter-cache | Cache abstraction | Apache-2.0 | Open-source |
| spring-boot-starter-actuator | Health/ops endpoints | Apache-2.0 | Open-source |
| flyway-core, flyway-database-postgresql | DB migrations | Apache-2.0 | Open-source (core) |
| postgresql (driver) | PostgreSQL JDBC driver | PostgreSQL License | Open-source |
| springdoc-openapi-starter-webmvc-ui | OpenAPI/Swagger UI | Apache-2.0 | Open-source |
| lombok | Boilerplate reduction | MIT | Open-source |
| mapstruct | DTO/entity mapping | Apache-2.0 | Open-source |
| jjwt-api/impl/jackson | JWT creation/parsing | Apache-2.0 | Open-source |
| okhttp | Outbound HTTP client | Apache-2.0 | Open-source |
| gson | JSON serialization | Apache-2.0 | Open-source |
| jsoup | HTML parsing/crawling | MIT | Open-source |
| commons-codec | Encoding utilities | Apache-2.0 | Open-source |
| testcontainers, junit-jupiter, spring-boot-starter-test, spring-security-test | Test stack | Mixed OSS (Apache-2.0/EPL/etc.) | Open-source |

## Docker Images
| Image | Usage | Possible License | Commercial Model |
|---|---|---|---|
| postgres:16 | Primary database | PostgreSQL License | Open-source |
| redis:7 | Cache/data service | BSD-style (Redis OSS) | Open-source |
| nginx:1.27-alpine | Frontend serving/reverse proxy | BSD-2-Clause | Open-source |
| node:20-alpine | Frontend build stage | Node.js license + image terms | Open-source |
| maven:3.9.9-eclipse-temurin-21 | Backend build stage | Apache-2.0 + image terms | Open-source |
| eclipse-temurin:21-jre | Backend runtime stage | GPLv2+Classpath Exception + image terms | Open-source |

## External APIs / Services Found
| Service | Evidence | Type |
|---|---|---|
| Google OAuth | Spring OAuth + frontend config | Freemium/managed service |
| Twilio Verify | OTP service implementation | Paid (usage-based) |
| Gemini API | Primary AI provider config/service | Paid/subscription/usage |
| OpenAI API | Fallback AI provider | Paid/usage |
| OpenRouter | Alternate AI provider | Paid/usage |
| PayFast | Payment checkout/webhooks | Paid (transaction-based) |
| PayPal | Alternate payment provider | Paid (transaction-based) |
| Adzuna | Jobs search integration | API plan-based |
| YouTube Data API | Learning/video retrieval | Quota-based (Google Cloud) |
| WhatsApp webhook endpoint | Messaging integration path | Depends on provider/BSP |

## Cloud Services Found
- AWS EC2 is strongly indicated by deployment scripts/workflow.
- GitHub Actions is used for CI and deployment orchestration.

## Notable Findings and Gaps
- `.env.example`: Not currently found in codebase, despite README references.
- Runtime `.env` contains real-looking secrets in local workspace; secret management should be moved to secure vault/CI secret stores with immediate rotation.
- License certainty: “Possible License” values are best-effort based on common upstream licensing and should be validated in a formal legal review.
