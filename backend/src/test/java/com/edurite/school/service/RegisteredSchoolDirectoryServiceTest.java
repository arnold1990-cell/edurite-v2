package com.edurite.school.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.district.entity.District;
import com.edurite.district.entity.Province;
import com.edurite.district.service.LocationService;
import com.edurite.school.entity.RegisteredSchool;
import com.edurite.school.repository.RegisteredSchoolRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class RegisteredSchoolDirectoryServiceTest {

    private final RegisteredSchoolRepository registeredSchoolRepository = mock(RegisteredSchoolRepository.class);
    private final LocationService locationService = mock(LocationService.class);
    private final RegisteredSchoolDirectoryService service = new RegisteredSchoolDirectoryService(registeredSchoolRepository, locationService);

    @Test
    void searchFiltersByProvinceDistrictAndSearchText() {
        UUID provinceId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        Province province = province(provinceId, "Eastern Cape");
        District district = district(districtId, provinceId, "Buffalo City");
        RegisteredSchool school = school(provinceId, districtId, "Tsholomnqa SS", "200200857");
        when(locationService.requireActiveProvince(provinceId)).thenReturn(province);
        when(locationService.requireActiveDistrict(districtId)).thenReturn(district);
        when(registeredSchoolRepository.searchActive(eq(provinceId), eq(districtId), eq("tsholo"), any(Pageable.class))).thenReturn(List.of(school));

        var result = service.search(provinceId, districtId, " tsholo ", 50);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().schoolName()).isEqualTo("Tsholomnqa SS");
        assertThat(result.items().getFirst().emisNumber()).isEqualTo("200200857");
        verify(registeredSchoolRepository).searchActive(eq(provinceId), eq(districtId), eq("tsholo"), any(Pageable.class));
    }

    @Test
    void searchRejectsDistrictOutsideProvince() {
        UUID selectedProvinceId = UUID.randomUUID();
        UUID actualProvinceId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        when(locationService.requireActiveProvince(selectedProvinceId)).thenReturn(province(selectedProvinceId, "Gauteng"));
        when(locationService.requireActiveDistrict(districtId)).thenReturn(district(districtId, actualProvinceId, "Buffalo City"));

        assertThatThrownBy(() -> service.search(selectedProvinceId, districtId, null, 25))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Selected district does not belong to the selected province.");
    }

    private Province province(UUID id, String name) {
        Province province = new Province();
        province.setId(id);
        province.setName(name);
        province.setCode(name.substring(0, 2).toUpperCase());
        return province;
    }

    private District district(UUID id, UUID provinceId, String name) {
        District district = new District();
        district.setId(id);
        district.setProvinceId(provinceId);
        district.setDistrictName(name);
        district.setActive(true);
        return district;
    }

    private RegisteredSchool school(UUID provinceId, UUID districtId, String name, String emisNumber) {
        RegisteredSchool school = new RegisteredSchool();
        school.setId(UUID.randomUUID());
        school.setProvinceId(provinceId);
        school.setDistrictId(districtId);
        school.setProvince("Eastern Cape");
        school.setDistrict("Buffalo City");
        school.setSchoolName(name);
        school.setEmisNumber(emisNumber);
        school.setSource("DBE_SAMPLE");
        school.setRegisteredStatus("REGISTERED");
        return school;
    }
}
