package com.edurite.school.controller;

import com.edurite.school.dto.SchoolLinkDtos;
import com.edurite.school.service.MySchoolService;
import com.edurite.school.service.SchoolAccessService;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/v1/school-admin/my-school-requests",
        "/api/school-admin/my-school-requests",
        "/api/v1/school-admin/learner-join-requests",
        "/api/school-admin/learner-join-requests"
})
public class SchoolAdminMySchoolRequestController {

    private final SchoolAccessService schoolAccessService;
    private final MySchoolService mySchoolService;

    public SchoolAdminMySchoolRequestController(SchoolAccessService schoolAccessService, MySchoolService mySchoolService) {
        this.schoolAccessService = schoolAccessService;
        this.mySchoolService = mySchoolService;
    }

    @GetMapping
    public List<SchoolLinkDtos.SchoolJoinRequestItemDto> requests(
            Principal principal,
            @RequestParam(required = false) String status
    ) {
        SchoolAccessService.AccessContext context = context(principal);
        return mySchoolService.listSchoolRequests(context.schoolId(), status);
    }

    @PostMapping("/{requestId}/approve")
    public SchoolLinkDtos.SchoolJoinRequestItemDto approve(Principal principal, @PathVariable UUID requestId) {
        SchoolAccessService.AccessContext context = context(principal);
        return mySchoolService.approveRequest(context.schoolId(), context.userId(), requestId);
    }

    @PostMapping("/{requestId}/reject")
    public SchoolLinkDtos.SchoolJoinRequestItemDto reject(Principal principal, @PathVariable UUID requestId) {
        SchoolAccessService.AccessContext context = context(principal);
        return mySchoolService.rejectRequest(context.schoolId(), context.userId(), requestId);
    }

    private SchoolAccessService.AccessContext context(Principal principal) {
        return schoolAccessService.requireSchoolContext(principal, Set.of(SchoolAccessService.ROLE_SCHOOL_ADMIN));
    }
}
