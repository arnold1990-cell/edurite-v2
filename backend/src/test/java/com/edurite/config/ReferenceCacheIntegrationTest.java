package com.edurite.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.district.entity.District;
import com.edurite.district.entity.Province;
import com.edurite.district.repository.CircuitRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.repository.ProvinceRepository;
import com.edurite.district.service.LocationService;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.roadmap.repository.CareerProgramRequirementRepository;
import com.edurite.roadmap.repository.CareerRoadmapRepository;
import com.edurite.roadmap.repository.SavedCareerRoadmapRepository;
import com.edurite.roadmap.service.AiCareerRoadmapService;
import com.edurite.roadmap.service.CareerRoadmapService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ReferenceCacheIntegrationTest.TestConfig.class)
class ReferenceCacheIntegrationTest {
    private final LocationService locationService;
    private final CareerRoadmapService careerRoadmapService;
    private final CacheInvalidationService cacheInvalidationService;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final CircuitRepository circuitRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;
    private final CareerProgramRequirementRepository requirementRepository;
    private final CacheManager cacheManager;

    @Autowired
    ReferenceCacheIntegrationTest(
            LocationService locationService,
            CareerRoadmapService careerRoadmapService,
            CacheInvalidationService cacheInvalidationService,
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            CircuitRepository circuitRepository,
            CareerRoadmapRepository careerRoadmapRepository,
            CareerProgramRequirementRepository requirementRepository,
            CacheManager cacheManager
    ) {
        this.locationService = locationService;
        this.careerRoadmapService = careerRoadmapService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.circuitRepository = circuitRepository;
        this.careerRoadmapRepository = careerRoadmapRepository;
        this.requirementRepository = requirementRepository;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void setUp() {
        reset(provinceRepository, districtRepository, circuitRepository, careerRoadmapRepository, requirementRepository);
        List.of(
                CacheNames.LOCATION_PROVINCES,
                CacheNames.LOCATION_DISTRICTS,
                CacheNames.LOCATION_CIRCUITS,
                CacheNames.CAREER_ROADMAP_REFERENCE,
                CacheNames.UNIVERSITY_REQUIREMENTS
        ).forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    void locationReferenceListsAreCachedAfterFirstRead() {
        Province province = province("Gauteng", "GP");
        when(provinceRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(province));

        assertThat(locationService.provinces()).hasSize(1);
        assertThat(locationService.provinces()).hasSize(1);

        verify(provinceRepository, times(1)).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void locationKeysIsolateProvinceSpecificDistricts() {
        UUID provinceA = UUID.randomUUID();
        UUID provinceB = UUID.randomUUID();
        when(provinceRepository.findByIdAndActiveTrue(provinceA)).thenReturn(Optional.of(province("A", "A")));
        when(provinceRepository.findByIdAndActiveTrue(provinceB)).thenReturn(Optional.of(province("B", "B")));
        when(districtRepository.findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(provinceA)).thenReturn(List.of(district("District A", "DA")));
        when(districtRepository.findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(provinceB)).thenReturn(List.of(district("District B", "DB")));

        assertThat(locationService.districts(provinceA)).extracting("name").containsExactly("District A");
        assertThat(locationService.districts(provinceA)).extracting("name").containsExactly("District A");
        assertThat(locationService.districts(provinceB)).extracting("name").containsExactly("District B");

        verify(districtRepository, times(1)).findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(provinceA);
        verify(districtRepository, times(1)).findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(provinceB);
    }

    @Test
    void explicitInvalidationClearsLocationDistrictCache() {
        when(districtRepository.findByActiveTrueOrderByDistrictNameAsc())
                .thenReturn(List.of(district("Before", "B")))
                .thenReturn(List.of(district("After", "A")));

        assertThat(locationService.districts()).extracting("name").containsExactly("Before");
        cacheInvalidationService.evictAllAfterCommit(CacheNames.LOCATION_DISTRICTS);
        assertThat(locationService.districts()).extracting("name").containsExactly("After");

        verify(districtRepository, times(2)).findByActiveTrueOrderByDistrictNameAsc();
    }

    @Test
    void careerRoadmapReferenceListIsCachedAfterFirstRead() {
        when(careerRoadmapRepository.findByActiveTrueOrderByTitleAsc()).thenReturn(List.of());

        assertThat(careerRoadmapService.list()).isEmpty();
        assertThat(careerRoadmapService.list()).isEmpty();

        verify(careerRoadmapRepository, times(1)).findByActiveTrueOrderByTitleAsc();
    }

    @Test
    void universityRequirementCacheNormalizesCareerKey() {
        when(requirementRepository.findByCareerNameContainingIgnoreCaseOrderByVerifiedDescApsRequiredAscInstitutionNameAsc("Law")).thenReturn(List.of());

        assertThat(careerRoadmapService.requirements("Law")).isEmpty();
        assertThat(careerRoadmapService.requirements(" law ")).isEmpty();

        verify(requirementRepository, times(1)).findByCareerNameContainingIgnoreCaseOrderByVerifiedDescApsRequiredAscInstitutionNameAsc("Law");
    }

    private Province province(String name, String code) {
        Province province = new Province();
        province.setId(UUID.randomUUID());
        province.setName(name);
        province.setCode(code);
        province.setActive(true);
        return province;
    }

    private District district(String name, String code) {
        District district = new District();
        district.setId(UUID.randomUUID());
        district.setDistrictName(name);
        district.setDistrictCode(code);
        district.setActive(true);
        return district;
    }

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.LOCATION_PROVINCES,
                    CacheNames.LOCATION_DISTRICTS,
                    CacheNames.LOCATION_CIRCUITS,
                    CacheNames.CAREER_ROADMAP_REFERENCE,
                    CacheNames.UNIVERSITY_REQUIREMENTS
            );
        }

        @Bean
        CacheInvalidationService cacheInvalidationService(CacheManager cacheManager) {
            return new CacheInvalidationService(cacheManager);
        }

        @Bean
        LocationService locationService(ProvinceRepository provinceRepository, DistrictRepository districtRepository, CircuitRepository circuitRepository) {
            return new LocationService(provinceRepository, districtRepository, circuitRepository);
        }

        @Bean
        CareerRoadmapService careerRoadmapService(
                CareerRoadmapRepository repository,
                SavedCareerRoadmapRepository savedCareerRoadmapRepository,
                CareerProgramRequirementRepository requirementRepository,
                StudentProfileRepository studentProfileRepository,
                InstitutionRepository institutionRepository,
                CurrentUserService currentUserService,
                AiCareerRoadmapService aiCareerRoadmapService
        ) {
            return new CareerRoadmapService(
                    repository,
                    savedCareerRoadmapRepository,
                    requirementRepository,
                    studentProfileRepository,
                    institutionRepository,
                    currentUserService,
                    new ObjectMapper().findAndRegisterModules(),
                    aiCareerRoadmapService
            );
        }

        @Bean
        ProvinceRepository provinceRepository() {
            return mock(ProvinceRepository.class);
        }

        @Bean
        DistrictRepository districtRepository() {
            return mock(DistrictRepository.class);
        }

        @Bean
        CircuitRepository circuitRepository() {
            return mock(CircuitRepository.class);
        }

        @Bean
        CareerRoadmapRepository careerRoadmapRepository() {
            return mock(CareerRoadmapRepository.class);
        }

        @Bean
        SavedCareerRoadmapRepository savedCareerRoadmapRepository() {
            return mock(SavedCareerRoadmapRepository.class);
        }

        @Bean
        CareerProgramRequirementRepository requirementRepository() {
            return mock(CareerProgramRequirementRepository.class);
        }

        @Bean
        StudentProfileRepository studentProfileRepository() {
            return mock(StudentProfileRepository.class);
        }

        @Bean
        InstitutionRepository institutionRepository() {
            return mock(InstitutionRepository.class);
        }

        @Bean
        CurrentUserService currentUserService() {
            return mock(CurrentUserService.class);
        }

        @Bean
        AiCareerRoadmapService aiCareerRoadmapService() {
            return mock(AiCareerRoadmapService.class);
        }
    }
}
