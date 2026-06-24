package com.edurite.subscription.repository;

import com.edurite.subscription.entity.PricingPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, UUID> {
    List<PricingPlan> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<PricingPlan> findByCodeAndActiveTrue(String code);
}

