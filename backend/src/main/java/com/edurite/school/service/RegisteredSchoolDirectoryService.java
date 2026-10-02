package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.district.entity.District;
import com.edurite.district.entity.Province;
import com.edurite.district.service.LocationService;
import com.edurite.school.dto.RegisteredSchoolDtos;
import com.edurite.school.entity.RegisteredSchool;
import com.edurite.school.repository.RegisteredSchoolRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisteredSchoolDirectoryService {

    private static final int MAX_SEARCH_RESULTS = 25;

    private final RegisteredSchoolRepository registeredSchoolRepository;
    private final LocationService locationService;

    public RegisteredSchoolDirectoryService(
            RegisteredSchoolRepository registeredSchoolRepository,
            LocationService locationService
    ) {
        this.registeredSchoolRepository = registeredSchoolRepository;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public RegisteredSchoolDtos.RegisteredSchoolSearchResponse search(UUID provinceId, UUID districtId, String search, int limit) {
        Province province = locationService.requireActiveProvince(provinceId);
        District district = locationService.requireActiveDistrict(districtId);
        if (district.getProvinceId() == null || !district.getProvinceId().equals(province.getId())) {
            throw new ResourceConflictException("Selected district does not belong to the selected province.");
        }

        String searchText = trimToNull(search);
        int resolvedLimit = Math.max(1, Math.min(limit <= 0 ? MAX_SEARCH_RESULTS : limit, MAX_SEARCH_RESULTS));
        List<RegisteredSchoolDtos.RegisteredSchoolDto> items = registeredSchoolRepository
                .searchActive(province.getId(), district.getId(), searchText, PageRequest.of(0, resolvedLimit))
                .stream()
                .map(this::toDto)
                .toList();
        return new RegisteredSchoolDtos.RegisteredSchoolSearchResponse(items);
    }

    @Transactional(readOnly = true)
    public RegisteredSchool requireActive(UUID id) {
        return registeredSchoolRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceConflictException("Registered school could not be found."));
    }

    public RegisteredSchoolDtos.RegisteredSchoolDto toDto(RegisteredSchool school) {
        return new RegisteredSchoolDtos.RegisteredSchoolDto(
                school.getId(),
                school.getSchoolName(),
                school.getEmisNumber(),
                school.getSchoolCode(),
                school.getProvinceId(),
                school.getDistrictId(),
                school.getProvince(),
                school.getDistrict(),
                school.getCircuit(),
                school.getSchoolType(),
                school.getPhysicalAddress(),
                school.getPrincipalName(),
                school.getSchoolEmail(),
                school.getContactNumber(),
                school.getSource(),
                school.getRegisteredStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
