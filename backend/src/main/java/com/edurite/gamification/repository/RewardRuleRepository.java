package com.edurite.gamification.repository;

import com.edurite.gamification.entity.RewardRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRuleRepository extends JpaRepository<RewardRule, UUID> {
    List<RewardRule> findByActiveTrueOrderByCreatedAtAsc();

    Optional<RewardRule> findByCode(String code);
}

