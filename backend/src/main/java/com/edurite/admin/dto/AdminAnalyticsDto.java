package com.edurite.admin.dto;

import java.util.List;

public record AdminAnalyticsDto(
        long totalUsers,
        long totalStudents,
        long totalCompanies,
        long totalAdmins,
        long pendingCompanyApprovals,
        long approvedCompanies,
        long suspendedCompanies,
        long totalBursaries,
        long activeBursaries,
        long suspendedBursaries,
        long closedOrExpiredBursaries,
        long totalApplicationsSubmitted,
        List<AdminApplicationsPerBursaryDto> applicationsPerBursary,
        List<AdminRecentUserDto> recentRegistrations,
        List<AdminRecentCompanyDto> recentCompanySignups,
        List<AdminRecentBursaryDto> recentBursaryPostings,
        List<AdminMonthlyMetricDto> registrationsByMonth,
        List<AdminMonthlyMetricDto> applicationsByMonth,
        List<AdminMonthlyMetricDto> bursariesByMonth,
        List<AdminStatusCountDto> bursariesByStatus
) {
}

