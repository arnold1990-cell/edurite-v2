package com.edurite.district.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.district.dto.LocationDtos;
import com.edurite.district.entity.Circuit;
import com.edurite.district.entity.District;
import com.edurite.district.entity.Province;
import com.edurite.district.repository.CircuitRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.district.repository.ProvinceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final CircuitRepository circuitRepository;

    public LocationService(
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            CircuitRepository circuitRepository
    ) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.circuitRepository = circuitRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationDtos.LocationOptionDto> provinces() {
        return provinceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationDtos.LocationOptionDto> districts() {
        return districtRepository.findByActiveTrueOrderByDistrictNameAsc().stream()
                .map(this::toOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationDtos.LocationOptionDto> districts(UUID provinceId) {
        requireActiveProvince(provinceId);
        return districtRepository.findByProvinceIdAndActiveTrueOrderByDistrictNameAsc(provinceId).stream()
                .map(this::toOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationDtos.LocationOptionDto> circuits(UUID districtId) {
        requireActiveDistrict(districtId);
        return circuitRepository.findByDistrictIdAndActiveTrueOrderByNameAsc(districtId).stream()
                .map(this::toOption)
                .toList();
    }

    public Province requireActiveProvince(UUID provinceId) {
        return provinceRepository.findByIdAndActiveTrue(provinceId)
                .orElseThrow(() -> new ResourceConflictException("Selected province could not be found"));
    }

    public District requireActiveDistrict(UUID districtId) {
        return districtRepository.findByIdAndActiveTrue(districtId)
                .orElseThrow(() -> new ResourceConflictException("Selected district could not be found"));
    }

    public Circuit requireActiveCircuit(UUID circuitId, UUID districtId) {
        return circuitRepository.findByIdAndDistrictIdAndActiveTrue(circuitId, districtId)
                .orElseThrow(() -> new ResourceConflictException("Selected circuit could not be found"));
    }

    private LocationDtos.LocationOptionDto toOption(Province province) {
        return new LocationDtos.LocationOptionDto(province.getId(), province.getName(), province.getCode());
    }

    private LocationDtos.LocationOptionDto toOption(District district) {
        return new LocationDtos.LocationOptionDto(district.getId(), district.getDistrictName(), district.getDistrictCode());
    }

    private LocationDtos.LocationOptionDto toOption(Circuit circuit) {
        return new LocationDtos.LocationOptionDto(circuit.getId(), circuit.getName(), circuit.getCode());
    }
}
