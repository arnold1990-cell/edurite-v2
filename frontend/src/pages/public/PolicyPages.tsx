import type { ReactNode } from 'react';

const Section = ({ title, children }: { title: string; children: ReactNode }) => (
  <section className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5">
    <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
    <div className="space-y-2 text-sm leading-7 text-slate-700">{children}</div>
  </section>
);

export const PrivacyPolicyPage = () => (
  <div className="mx-auto max-w-4xl space-y-6">
    <header className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">EDURITE PRIVACY POLICY</h1>
      <p className="mt-3 text-sm text-slate-600">Effective Date: April 9, 2026</p>
      <p className="text-sm text-slate-600">Version: v1.0</p>
    </header>

    <Section title="1. Introduction">
      <p>
        EduRite ("we", "our", "us") is committed to protecting your personal information in accordance with applicable
        data protection laws, including POPIA (Protection of Personal Information Act).
      </p>
      <p>
        This Privacy Policy explains how we collect, use, store, and protect your information when you use our platform.
      </p>
    </Section>

    <Section title="2. Information We Collect">
      <p>We may collect the following:</p>
      <p className="font-semibold text-slate-800">a) Personal Information</p>
      <p>Full name, Email address, Mobile number, Date of birth, Gender, School / education details</p>
      <p className="font-semibold text-slate-800">b) Account Information</p>
      <p>Login credentials, Profile details, Preferences</p>
      <p className="font-semibold text-slate-800">c) Usage Data</p>
      <p>Pages visited, Features used, Activity on the platform</p>
      <p className="font-semibold text-slate-800">d) Psychometric &amp; Learning Data</p>
      <p>Assessment responses, Career guidance results, Learning progress</p>
    </Section>

    <Section title="3. How We Use Your Information">
      <p>
        We use your information to: Create and manage your account; Provide AI-powered career guidance; Recommend universities,
        bursaries, and opportunities; Personalize your learning experience; Improve platform performance; Communicate updates
        and notifications; Ensure security and prevent fraud.
      </p>
    </Section>

    <Section title="4. Legal Basis for Processing">
      <p>We process your data based on: Your consent, Performance of a contract, Legal obligations, Legitimate interests.</p>
    </Section>

    <Section title="5. POPIA Compliance">
      <p>
        In line with POPIA: your data is collected lawfully and transparently; only relevant data is collected; your information
        is protected against unauthorized access; you have the right to access, correct, or delete your data.
      </p>
    </Section>

    <Section title="6. Sharing of Information">
      <p>We do NOT sell your personal data.</p>
      <p>
        We may share your data with trusted service providers (hosting, email services), educational institutions (where applicable
        and with consent), and legal authorities if required by law.
      </p>
    </Section>

    <Section title="7. Data Security">
      <p>We implement encryption, secure authentication (JWT), access control, and regular monitoring.</p>
    </Section>

    <Section title="8. Data Retention">
      <p>We keep your data as long as your account is active, or as required by law.</p>
      <p>Deleted accounts may be anonymized for analytics.</p>
    </Section>

    <Section title="9. Your Rights">
      <p>You have the right to access your data, correct your data, delete your account, withdraw consent, and object to processing.</p>
    </Section>

    <Section title="10. Cookies &amp; Tracking">
      <p>EduRite may use cookies to improve user experience, track usage, and store preferences.</p>
    </Section>

    <Section title="11. Children / Minors">
      <p>If you are under 18, you may require parental/guardian consent. We take extra care in handling your data.</p>
    </Section>

    <Section title="12. Changes to This Policy">
      <p>We may update this policy from time to time. Changes will be communicated on the platform.</p>
    </Section>

    <Section title="13. Contact Us">
      <p>Email: info@edificegroup.africa</p>
      <p>Website: https://www.edurite.org</p>
    </Section>
  </div>
);

export const TermsAndConditionsPage = () => (
  <div className="mx-auto max-w-4xl space-y-6">
    <header className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">EDURITE TERMS &amp; CONDITIONS</h1>
      <p className="mt-3 text-sm text-slate-600">Effective Date: April 9, 2026</p>
    </header>

    <Section title="1. Acceptance of Terms">
      <p>By using EduRite, you agree to these Terms and Conditions.</p>
    </Section>

    <Section title="2. Services Provided">
      <p>EduRite offers career guidance, psychometric assessments, learning support, and university &amp; bursary recommendations.</p>
    </Section>

    <Section title="3. User Responsibilities">
      <p>You agree to provide accurate information, keep your login details secure, use the platform responsibly, and not misuse or abuse the system.</p>
    </Section>

    <Section title="4. Account Registration">
      <p>You must create an account to access certain features. Phone OTP verification may be required. Accounts may be suspended for violations.</p>
    </Section>

    <Section title="5. Intellectual Property">
      <p>All content on EduRite belongs to EduRite and may not be copied without permission.</p>
    </Section>

    <Section title="6. AI &amp; Recommendations Disclaimer">
      <p>
        EduRite provides guidance based on available data and AI models. We do NOT guarantee admission into universities, bursary
        approvals, or career outcomes.
      </p>
    </Section>

    <Section title="7. Payments &amp; Subscriptions">
      <p>Some features may require payment. Payments are handled via secure third-party providers. Refund policies may apply depending on services.</p>
    </Section>

    <Section title="8. Account Termination">
      <p>We may suspend or terminate accounts that violate terms or engage in fraud or misuse. Users may also delete their accounts.</p>
    </Section>

    <Section title="9. Limitation of Liability">
      <p>EduRite is not liable for decisions made based on recommendations or losses resulting from use of the platform.</p>
    </Section>

    <Section title="10. Privacy">
      <p>Use of EduRite is also governed by our Privacy Policy.</p>
    </Section>

    <Section title="11. Changes to Terms">
      <p>We may update these Terms at any time. Continued use means acceptance.</p>
    </Section>

    <Section title="12. Governing Law">
      <p>These Terms are governed by applicable laws in your operating jurisdiction.</p>
    </Section>
  </div>
);
