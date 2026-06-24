package com.edurite.district.controller;

import com.edurite.district.dto.LocationDtos;
import com.edurite.district.service.LocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/locations", "/api/locations"})
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/provinces")
    public List<LocationDtos.LocationOptionDto> provinces() {
        return locationService.provinces();
    }

    @GetMapping("/districts")
    public List<LocationDtos.LocationOptionDto> districts() {
        return locationService.districts();
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public List<LocationDtos.LocationOptionDto> districts(@PathVariable UUID provinceId) {
        return locationService.districts(provinceId);
    }

    @GetMapping("/districts/{districtId}/circuits")
    public List<LocationDtos.LocationOptionDto> circuits(@PathVariable UUID districtId) {
        return locationService.circuits(districtId);
    }
}
