package com.edurite.auth.config;

import com.edurite.learning.entity.LearningCategory;
import com.edurite.learning.entity.LearningOutcomeMapping;
import com.edurite.learning.entity.LearningResource;
import com.edurite.learning.repository.LearningCategoryRepository;
import com.edurite.learning.repository.LearningOutcomeMappingRepository;
import com.edurite.learning.repository.LearningResourceRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class LearningCentreDataSeeder {

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "edurite.seed", name = "enabled", havingValue = "true")
    ApplicationRunner learningCentreSeedRunner(
            LearningCategoryRepository categoryRepository,
            LearningResourceRepository resourceRepository,
            LearningOutcomeMappingRepository mappingRepository
    ) {
        return args -> {
            if (resourceRepository.count() > 0) {
                return;
            }

            LearningCategory communication = new LearningCategory();
            communication.setName("Communication");
            communication.setSlug("communication");
            communication.setDescription("Improve confidence, writing, and presentation skills.");
            categoryRepository.save(communication);

            LearningCategory analytics = new LearningCategory();
            analytics.setName("Data & Analysis");
            analytics.setSlug("data-analysis");
            analytics.setDescription("Build analytical and problem-solving capacity.");
            categoryRepository.save(analytics);

            LearningResource communicationResource = new LearningResource();
            communicationResource.setCategoryId(communication.getId());
            communicationResource.setTitle("Professional communication foundations");
            communicationResource.setSummary("A practical guide for written and verbal communication in study and workplace contexts.");
            communicationResource.setUrl("https://www.coursera.org/learn/wharton-communication-skills");
            communicationResource.setResourceType("COURSE");
            communicationResource.setDifficulty("BEGINNER");
            communicationResource.setEstimatedMinutes(180);
            communicationResource.setTags("communication,presentation,writing");
            resourceRepository.save(communicationResource);

            LearningResource analyticsResource = new LearningResource();
            analyticsResource.setCategoryId(analytics.getId());
            analyticsResource.setTitle("Data analysis with spreadsheets");
            analyticsResource.setSummary("Learn to structure data, run analysis, and explain findings clearly.");
            analyticsResource.setUrl("https://www.khanacademy.org/math/statistics-probability");
            analyticsResource.setResourceType("VIDEO");
            analyticsResource.setDifficulty("BEGINNER");
            analyticsResource.setEstimatedMinutes(220);
            analyticsResource.setTags("data,analytics,critical-thinking");
            resourceRepository.save(analyticsResource);

            LearningOutcomeMapping communicationMapping = new LearningOutcomeMapping();
            communicationMapping.setOutcomeKey("communication");
            communicationMapping.setOutcomeLabel("Communication");
            communicationMapping.setResourceId(communicationResource.getId());
            communicationMapping.setPriority(1);
            mappingRepository.save(communicationMapping);

            LearningOutcomeMapping analyticsMapping = new LearningOutcomeMapping();
            analyticsMapping.setOutcomeKey("analytical");
            analyticsMapping.setOutcomeLabel("Analytical thinking");
            analyticsMapping.setResourceId(analyticsResource.getId());
            analyticsMapping.setPriority(1);
            mappingRepository.save(analyticsMapping);
        };
    }
}

