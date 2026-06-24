# EduRite Modules (Phase 0)

The backend is partitioned into packages representing business and platform modules.

## Platform modules

- `config`: TODO central app configuration, beans, and profile-specific wiring.
- `security`: TODO Spring Security setup, authentication providers, and access rules.
- `common`: TODO shared primitives (exceptions, response envelopes, utilities).

## Business modules

- `auth`: TODO sign-up, sign-in, token lifecycle, and credential recovery workflows.
- `user`: TODO user profile management and account lifecycle.
- `student`: TODO student-specific features (skills, learning goals, recommendations).
- `company`: TODO employer-side features (job posts, candidate pipelines, outreach).

## Module interaction guidelines

- Keep module internals private by default.
- Interact through clearly defined service interfaces.
- Prevent cyclic dependencies between modules.
- Promote shared behavior into `common` only when truly cross-cutting.
