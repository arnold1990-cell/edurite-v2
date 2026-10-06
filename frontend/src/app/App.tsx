import { StudentProfileSectionsPage, StudentCareerExplorerPage, StudentLearningResourcesPage } from '@/pages/student/StudentSectionPages';
import { StudentRedirect } from '@/routes/StudentRedirect';
import { StudentAppShell } from '@/components/student/StudentAppShell';
import { StudentInstitutionsPage, StudentProgressPage, StudentFundingPage, StudentStudyOptionsPage, StudentMessagesPage, StudentNotFoundPage } from '@/pages/student/StudentSectionPages';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { DistrictAdminLayout } from '@/components/layout/DistrictAdminLayout';
import { RequireAuth } from '@/routes/RequireAuth';
import { RequireRole } from '@/routes/RequireRole';
import { RequireSchoolPortalAccess } from '@/routes/RequireSchoolPortalAccess';
import { RequireCompanyApproval } from '@/routes/RequireCompanyApproval';
import { RequireSchoolApproval } from '@/routes/RequireSchoolApproval';
import { AccountChangePasswordPage } from '@/pages/account/AccountPages';
import { AboutPage, BursariesPage, BursaryDetailsPage, CareerDetailsPage, CareersPage, CourseDetailsPage, CoursesPage, InstitutionDetailsPage, InstitutionsPage, PricingPage } from '@/pages/public/PublicPages';
import { PrivacyPolicyPage, TermsAndConditionsPage } from '@/pages/public/PolicyPages';
import { ForgotPasswordPage, LoginPage, RegisterCompanyPage, RegisterSchoolPage, RegisterStudentPage, ResetPasswordPage, VerifyEmailNoticePage, VerifyEmailPage } from '@/pages/public/AuthPages';
import { StudentCareerDetailsPage, StudentDashboardPage, StudentNotificationsPage, StudentProfilePage, StudentSettingsPage, StudentSubscriptionPage } from '@/pages/student/StudentPages';
import { AdminSchoolPortalPage } from '@/pages/student/StudentFeaturePages';
import { StudentUniversityAdmissionRequirementsPage, StudentUniversityProgrammesPage } from '@/pages/student/StudentUniversityInfoPages';
import { CompanyApplicantsPage, CompanyBursariesPage, CompanyCreateBursaryPage, CompanyDashboardPage, CompanyEditBursaryPage, CompanyNotificationsPage, CompanyPendingApprovalPage, CompanyProfilePage, CompanySettingsPage, CompanyShortlistedPage, CompanyTalentSearchPage, CompanyVerificationDocsPage } from '@/pages/company/CompanyPages';
import { AdminAnalyticsPage, AdminAuditLogsPage, AdminBursaryModerationPage, AdminCompanyReviewPage, AdminDashboardPage, AdminDistrictManagementPage, AdminNotificationTemplatesPage, AdminNotificationsPage, AdminPaymentsPage, AdminPendingApprovalsPage, AdminRolesPage, AdminSettingsPage, AdminSubscriptionsPage, AdminUsersPage } from '@/pages/admin/AdminPages';
import { SchoolPendingApprovalPage, SchoolStudentDashboardPage } from '@/pages/school/SchoolPages';
import { SchoolAdminPortalPage } from '@/pages/school/SchoolAdminCommandCenterPage';
import { TeacherPortalPage } from '@/pages/school/SchoolPortalPages';
import {
  DistrictAIInsightsPage,
  DistrictAnalyticsPage,
  DistrictCircuitCurriculumPage,
  DistrictCircuitDashboardPage,
  DistrictCircuitInterventionsPage,
  DistrictCircuitSchoolsPage,
  DistrictCircuitSupportRequestsPage,
  DistrictCircuitVisitsPage,
  DistrictCurriculumManagementPage,
  DistrictDirectorDashboardPage,
  DistrictInterventionsPage,
  DistrictReportsPage,
  DistrictSchoolDetailPage,
  DistrictSchoolsPage,
  DistrictSettingsPage,
  DistrictSubjectAdvisorAssessmentsPage,
  DistrictSubjectAdvisorDashboardPage,
  DistrictSubjectAdvisorTeachersPage,
} from '@/pages/district/DistrictPages';
import { SchoolRegistrationRequestsPage } from '@/pages/district/SchoolRegistrationRequestsPage';
import { InactivitySessionManager } from '@/components/auth/InactivitySessionManager';
import { useAuth } from '@/hooks/useAuth';

const districtPortalRoles = new Set(['DISTRICT_ADMIN', 'DISTRICT_DIRECTOR', 'CIRCUIT_MANAGER', 'SUBJECT_ADVISOR']);

const AccountRouteLayout = () => {
  const { getPrimaryRole } = useAuth();
  const primaryRole = getPrimaryRole();

  if (primaryRole && districtPortalRoles.has(primaryRole)) {
    return <DistrictAdminLayout />;
  }

  if (primaryRole === 'STUDENT') return <StudentRedirect to="/student/settings/password" />;
  return <DashboardLayout />;
};

const PayFastRedirectRelay = ({ checkoutResult }: { checkoutResult: 'processing' | 'cancel' }) => {
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const paymentReference = query.get('paymentReference')?.trim()
    || query.get('m_payment_id')?.trim()
    || query.get('reference')?.trim()
    || '';

  if (!query.get('checkoutResult')) {
    query.set('checkoutResult', checkoutResult);
  }
  if (!query.get('provider')) {
    query.set('provider', 'payfast');
  }
  if (paymentReference && !query.get('paymentReference')) {
    query.set('paymentReference', paymentReference);
  }

  const queryString = query.toString();
  return <Navigate to={queryString ? `/student/subscription?${queryString}` : '/student/subscription'} replace />;
};

export const App = () => (
  <>
    <InactivitySessionManager />
    <Routes>
    <Route element={<PublicLayout />}>
      <Route path="/" element={<Navigate to="/auth/login" replace />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/careers" element={<CareersPage />} />
      <Route path="/careers/:id" element={<CareerDetailsPage />} />
      <Route path="/courses" element={<CoursesPage />} />
      <Route path="/courses/:id" element={<CourseDetailsPage />} />
      <Route path="/institutions" element={<InstitutionsPage />} />
      <Route path="/institutions/:id" element={<InstitutionDetailsPage />} />
      <Route path="/bursaries" element={<BursariesPage />} />
      <Route path="/bursaries/:id" element={<BursaryDetailsPage />} />
      <Route path="/pricing" element={<PricingPage />} />
      <Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
      <Route path="/terms-and-conditions" element={<TermsAndConditionsPage />} />
      <Route path="/auth/login" element={<LoginPage />} />
      <Route path="/school/login" element={<LoginPage />} />
      <Route path="/teacher/login" element={<LoginPage />} />
      <Route path="/school-student/login" element={<LoginPage />} />
      <Route path="/company/login" element={<LoginPage />} />
      <Route path="/admin/login" element={<LoginPage />} />
      <Route path="/district/login" element={<LoginPage />} />
      <Route path="/auth/register/student" element={<RegisterStudentPage />} />
      <Route path="/auth/register" element={<RegisterStudentPage />} />
      <Route path="/auth/register/company" element={<RegisterCompanyPage />} />
      <Route path="/auth/register/school" element={<RegisterSchoolPage />} />
      <Route path="/school/register" element={<RegisterSchoolPage />} />
      <Route path="/register/company" element={<RegisterCompanyPage />} />
      <Route path="/company/register" element={<RegisterCompanyPage />} />
      <Route path="/auth/verify-otp" element={<VerifyEmailPage />} />
      <Route path="/auth/verify-otp/notice" element={<VerifyEmailNoticePage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/verify-email/notice" element={<VerifyEmailNoticePage />} />
      <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/company/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/admin/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/district/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/auth/reset-password" element={<ResetPasswordPage />} />
      <Route path="/company/reset-password" element={<ResetPasswordPage />} />
      <Route path="/admin/reset-password" element={<ResetPasswordPage />} />
      <Route path="/district/reset-password" element={<ResetPasswordPage />} />
      <Route path="/payments/payfast/return" element={<PayFastRedirectRelay checkoutResult="processing" />} />
      <Route path="/payments/payfast/cancel" element={<PayFastRedirectRelay checkoutResult="cancel" />} />
    </Route>

    <Route element={<RequireAuth />}>
      <Route element={<AccountRouteLayout />}>
        <Route path="/account/change-password" element={<AccountChangePasswordPage />} />
      </Route>

      <Route element={<RequireRole role="STUDENT" />}>
        <Route path="/student" element={<StudentAppShell />}>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<StudentDashboardPage />} />
          <Route path="profile" element={<StudentProfileSectionsPage />} />
          <Route path="careers" element={<StudentRedirect to="/student/career-explorer" />} />
          <Route path="recommendations/careers" element={<StudentRedirect to="/student/career-explorer?section=guidance" />} />
          <Route path="academic-profile" element={<StudentRedirect to="/student/profile?section=academic" />} />
          <Route path="documents" element={<StudentRedirect to="/student/profile?section=documents" />} />
          <Route path="qualifications" element={<StudentRedirect to="/student/profile?section=career" />} />
          <Route path="experience" element={<StudentRedirect to="/student/profile?section=career" />} />
          <Route path="recommendations/bursaries" element={<StudentRedirect to="/student/funding?section=matches" />} />
          <Route path="psychometric" element={<StudentRedirect to="/student/career-explorer?section=career-match" />} />
          <Route path="cv-builder" element={<StudentRedirect to="/student/profile?section=cv" />} />
          <Route path="ai-tutor" element={<StudentRedirect to="/student/learning?section=tutor" />} />
          <Route path="interview-prep" element={<StudentRedirect to="/student/learning?section=tutor" />} />
          <Route path="learning-centre" element={<StudentRedirect to="/student/learning?section=centre" />} />
          <Route path="rewards" element={<StudentRedirect to="/student/dashboard?section=rewards" />} />
          <Route path="careers/:id" element={<StudentCareerDetailsPage />} />
          <Route path="ai-guidance" element={<StudentRedirect to="/student/career-explorer?section=guidance" />} />
          <Route path="explore" element={<StudentRedirect to="/student/career-explorer" />} />
          <Route path="career-roadmaps" element={<StudentRedirect to="/student/career-explorer?section=career-path" />} />
          <Route path="saved" element={<StudentRedirect to="/student/funding?section=opportunities" />} />
          <Route path="applications" element={<StudentRedirect to="/student/funding?section=bursaries" />} />
          <Route path="scholarships" element={<StudentRedirect to="/student/funding?section=scholarships" />} />
          <Route path="universities" element={<StudentRedirect to="/student/study-options?section=universities" />} />
          <Route path="universities/:slug/programmes" element={<StudentUniversityProgrammesPage />} />
          <Route path="universities/:slug/admission-requirements" element={<StudentUniversityAdmissionRequirementsPage />} />
          <Route path="colleges-tvets" element={<StudentRedirect to="/student/study-options?section=colleges-tvets" />} />
          <Route path="university-applications" element={<StudentRedirect to="/student/study-options?section=applications" />} />
          <Route path="notifications" element={<StudentNotificationsPage />} />
          <Route path="subscription" element={<StudentSubscriptionPage />} />
          <Route path="settings" element={<StudentSettingsPage />} />
          <Route path="my-school" element={<StudentRedirect to="/student/profile?section=school" />} />
          <Route path="career-explorer" element={<StudentCareerExplorerPage />} />
          <Route path="study-options" element={<StudentStudyOptionsPage />} />
          <Route path="funding" element={<StudentFundingPage />} />
          <Route path="institutions" element={<StudentRedirect to="/student/study-options" />} />
          <Route path="learning" element={<StudentLearningResourcesPage />} />
          <Route path="progress" element={<StudentRedirect to="/student/dashboard" />} />
          <Route path="goals" element={<StudentRedirect to="/student/dashboard?section=goals" />} />
          <Route path="messages" element={<StudentMessagesPage />} />
          <Route path="profile/cv" element={<StudentRedirect to="/student/profile?section=cv" />} />
          <Route path="funding/saved" element={<StudentRedirect to="/student/funding?section=saved" />} />
          <Route path="institutions/universities" element={<StudentRedirect to="/student/study-options?section=universities" />} />
          <Route path="learning/tutor" element={<StudentRedirect to="/student/learning?section=tutor" />} />
          <Route path="career-explorer/interests" element={<StudentRedirect to="/student/career-explorer?section=interests" />} />
          <Route path="career-explorer/match" element={<StudentRedirect to="/student/career-explorer?section=guidance" />} />
          <Route path="progress/rewards" element={<StudentRedirect to="/student/dashboard?section=rewards" />} />
          <Route path="funding/applications" element={<StudentRedirect to="/student/funding?section=applications" />} />
          <Route path="funding/scholarships" element={<StudentRedirect to="/student/funding?section=scholarships" />} />
          <Route path="funding/matches" element={<StudentRedirect to="/student/funding?section=matches" />} />
          <Route path="institutions/colleges-tvets" element={<StudentRedirect to="/student/study-options?section=colleges-tvets" />} />
          <Route path="institutions/applications" element={<StudentRedirect to="/student/study-options?section=applications" />} />
          <Route path="settings/password" element={<AccountChangePasswordPage />} />
          <Route path="psychometric-test" element={<StudentRedirect to="/student/career-explorer?section=career-match" />} />
          <Route path="career-guidance" element={<StudentRedirect to="/student/career-explorer?section=guidance" />} />
          <Route path="opportunities" element={<StudentRedirect to="/student/funding?section=opportunities" />} />
          <Route path="bursaries" element={<StudentRedirect to="/student/funding?section=bursaries" />} />
          <Route path="scholarship-assistant" element={<StudentRedirect to="/student/funding?section=scholarships" />} />
          <Route path="*" element={<StudentNotFoundPage />} />
        </Route>
      </Route>

      <Route element={<RequireRole role="COMPANY" />}>
        <Route element={<RequireCompanyApproval />}>
          <Route element={<DashboardLayout />}>
            <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
            <Route path="/company/pending" element={<CompanyPendingApprovalPage />} />
            <Route path="/company/pending-approval" element={<CompanyPendingApprovalPage />} />
            <Route path="/company/rejected" element={<CompanyPendingApprovalPage />} />
            <Route path="/company/profile" element={<CompanyProfilePage />} />
            <Route path="/company/verification-docs" element={<CompanyVerificationDocsPage />} />
            <Route path="/company/bursaries" element={<CompanyBursariesPage />} />
            <Route path="/company/bursaries/new" element={<CompanyCreateBursaryPage />} />
            <Route path="/company/bursaries/:id/edit" element={<CompanyEditBursaryPage />} />
            <Route path="/company/applicants" element={<CompanyApplicantsPage />} />
            <Route path="/company/students" element={<CompanyTalentSearchPage />} />
            <Route path="/company/shortlisted" element={<CompanyShortlistedPage />} />
            <Route path="/company/notifications" element={<CompanyNotificationsPage />} />
            <Route path="/company/settings" element={<CompanySettingsPage />} />
          </Route>
        </Route>
      </Route>

      <Route element={<RequireRole role="ADMIN" />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/district-management" element={<AdminDistrictManagementPage />} />
          <Route path="/admin/roles" element={<AdminRolesPage />} />
          <Route path="/admin/pending-approvals" element={<AdminPendingApprovalsPage />} />
          <Route path="/admin/companies/:id" element={<AdminCompanyReviewPage />} />
          <Route path="/admin/bursaries" element={<AdminBursaryModerationPage />} />
          <Route path="/admin/subscriptions" element={<AdminSubscriptionsPage />} />
          <Route path="/admin/payments" element={<AdminPaymentsPage />} />
          <Route path="/admin/notification-templates" element={<AdminNotificationTemplatesPage />} />
          <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
          <Route path="/admin/notifications" element={<AdminNotificationsPage />} />
          <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="/admin/schools" element={<AdminSchoolPortalPage />} />
          <Route path="/admin/settings" element={<AdminSettingsPage />} />
        </Route>
      </Route>

      <Route element={<RequireRole roles={['DISTRICT_ADMIN', 'DISTRICT_DIRECTOR', 'CIRCUIT_MANAGER', 'SUBJECT_ADVISOR']} />}>
        <Route element={<DistrictAdminLayout />}>
          <Route path="/district/dashboard" element={<DistrictDirectorDashboardPage />} />
          <Route path="/district/schools" element={<DistrictSchoolsPage />} />
          <Route path="/district/school-registration-requests" element={<SchoolRegistrationRequestsPage />} />
          <Route path="/district/schools/:schoolId" element={<DistrictSchoolDetailPage />} />
          <Route path="/district/analytics" element={<DistrictAnalyticsPage />} />
          <Route path="/district/reports" element={<DistrictReportsPage />} />
          <Route path="/district/ai-insights" element={<DistrictAIInsightsPage />} />
          <Route path="/district/interventions" element={<DistrictInterventionsPage />} />
          <Route path="/district/settings" element={<DistrictSettingsPage />} />
          <Route path="/district/curriculum/:view" element={<DistrictCurriculumManagementPage />} />
          <Route path="/district/circuit/dashboard" element={<DistrictCircuitDashboardPage />} />
          <Route path="/district/circuit/schools" element={<DistrictCircuitSchoolsPage />} />
          <Route path="/district/circuit/curriculum" element={<DistrictCircuitCurriculumPage />} />
          <Route path="/district/circuit/visits" element={<DistrictCircuitVisitsPage />} />
          <Route path="/district/circuit/support-requests" element={<DistrictCircuitSupportRequestsPage />} />
          <Route path="/district/circuit/interventions" element={<DistrictCircuitInterventionsPage />} />
          <Route path="/district/advisor/dashboard" element={<DistrictSubjectAdvisorDashboardPage />} />
          <Route path="/district/advisor/teachers" element={<DistrictSubjectAdvisorTeachersPage />} />
          <Route path="/district/advisor/atp-monitoring" element={<DistrictSubjectAdvisorTeachersPage />} />
          <Route path="/district/advisor/assessments" element={<DistrictSubjectAdvisorAssessmentsPage />} />
        </Route>
      </Route>

      <Route element={<RequireRole role="SCHOOL_ADMIN" />}>
        <Route element={<RequireSchoolApproval />}>
        <Route element={<DashboardLayout />}>
          <Route path="/school/pending-approval" element={<SchoolPendingApprovalPage />} />
          <Route path="/school/registration-status" element={<SchoolPendingApprovalPage />} />
          <Route path="/school/dashboard" element={<SchoolAdminPortalPage />} />
          <Route path="/school/analytics" element={<SchoolAdminPortalPage />} />
          <Route path="/school/ai-insights" element={<SchoolAdminPortalPage />} />
          <Route path="/school/learners" element={<SchoolAdminPortalPage />} />
          <Route path="/school/enrolment" element={<SchoolAdminPortalPage />} />
          <Route path="/school/my-school-requests" element={<SchoolAdminPortalPage />} />
          <Route path="/school/teachers" element={<SchoolAdminPortalPage />} />
          <Route path="/school/classes" element={<SchoolAdminPortalPage />} />
          <Route path="/school/subjects" element={<SchoolAdminPortalPage />} />
          <Route path="/school/curriculum" element={<SchoolAdminPortalPage />} />
          <Route path="/school/curriculum-calendar" element={<SchoolAdminPortalPage />} />
          <Route path="/school/assignments" element={<SchoolAdminPortalPage />} />
          <Route path="/school/assessments" element={<SchoolAdminPortalPage />} />
          <Route path="/school/results" element={<SchoolAdminPortalPage />} />
          <Route path="/school/academic-insights" element={<SchoolAdminPortalPage />} />
          <Route path="/school/career-readiness" element={<SchoolAdminPortalPage />} />
          <Route path="/school/courses" element={<SchoolAdminPortalPage />} />
          <Route path="/school/bursaries" element={<SchoolAdminPortalPage />} />
          <Route path="/school/interventions" element={<SchoolAdminPortalPage />} />
          <Route path="/school/reports" element={<SchoolAdminPortalPage />} />
          <Route path="/school/announcements" element={<SchoolAdminPortalPage />} />
          <Route path="/school/notifications" element={<SchoolAdminPortalPage />} />
          <Route path="/school/support-requests" element={<SchoolAdminPortalPage />} />
          <Route path="/school/school-settings" element={<SchoolAdminPortalPage />} />
          <Route path="/school/settings" element={<SchoolAdminPortalPage />} />
        </Route>
        </Route>
      </Route>

      <Route element={<RequireRole role="TEACHER" />}>
        <Route element={<DashboardLayout />}>
          <Route path="/teacher/dashboard" element={<TeacherPortalPage />} />
          <Route path="/teacher/learners" element={<TeacherPortalPage />} />
          <Route path="/teacher/academic-insights" element={<TeacherPortalPage />} />
          <Route path="/teacher/career-readiness" element={<TeacherPortalPage />} />
          <Route path="/teacher/courses" element={<TeacherPortalPage />} />
          <Route path="/teacher/bursaries" element={<TeacherPortalPage />} />
          <Route path="/teacher/curriculum" element={<TeacherPortalPage />} />
          <Route path="/teacher/curriculum-calendar" element={<TeacherPortalPage />} />
          <Route path="/teacher/interventions" element={<TeacherPortalPage />} />
          <Route path="/teacher/reports" element={<TeacherPortalPage />} />
          <Route path="/teacher/settings" element={<TeacherPortalPage />} />
        </Route>
      </Route>

      <Route element={<RequireSchoolPortalAccess />}>
        <Route element={<DashboardLayout />}>
          <Route path="/school-student/dashboard" element={<SchoolStudentDashboardPage />} />
        </Route>
      </Route>
    </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  </>
);

