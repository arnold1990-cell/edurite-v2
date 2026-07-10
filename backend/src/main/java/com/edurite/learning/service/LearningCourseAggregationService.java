package com.edurite.learning.service;

import com.edurite.learning.entity.LearningResource;
import com.edurite.learning.repository.LearningResourceRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningCourseAggregationService {

    private static final int MIN_COURSES_PER_CATEGORY = 5;
    private static final Set<String> REQUIRED_CATEGORIES = Set.of(
            "Mathematics",
            "Science",
            "Coding",
            "Business",
            "Engineering",
            "AI & Technology",
            "Accounting",
            "Languages",
            "Exam Preparation",
            "Career Guidance"
    );

    private final LearningResourceRepository learningResourceRepository;

    public LearningCourseAggregationService(LearningResourceRepository learningResourceRepository) {
        this.learningResourceRepository = learningResourceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureCoursesAvailable() {
        if (hasMinimumCoverage()) {
            return;
        }
        refreshApprovedCourses();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshSummary refreshApprovedCourses() {
        OffsetDateTime fetchedAt = OffsetDateTime.now();
        Map<String, SeedCourse> deduplicated = new LinkedHashMap<>();
        for (SeedCourse course : seedCourses()) {
            deduplicated.put(dedupeKey(course), course);
        }

        int created = 0;
        int updated = 0;
        for (SeedCourse course : deduplicated.values()) {
            Optional<LearningResource> existing = learningResourceRepository
                    .findFirstByTitleIgnoreCaseAndProviderIgnoreCaseAndCourseUrlIgnoreCase(
                            course.title(),
                            course.provider(),
                            course.courseUrl()
                    );
            LearningResource resource = existing.orElseGet(LearningResource::new);
            boolean isNew = resource.getId() == null;
            apply(resource, course, fetchedAt);
            learningResourceRepository.save(resource);
            if (isNew) {
                created += 1;
            } else {
                updated += 1;
            }
        }

        return new RefreshSummary(deduplicated.size(), created, updated, fetchedAt);
    }

    private boolean hasMinimumCoverage() {
        List<LearningResource> resources = learningResourceRepository.findByActiveTrueAndIsFreeTrueOrderByLastFetchedAtDescCreatedAtDesc();
        Map<String, Long> counts = resources.stream()
                .filter(resource -> resource.getResourceType() != null && resource.getResourceType().equalsIgnoreCase("Course"))
                .filter(LearningResource::isFree)
                .filter(resource -> resource.getCategory() != null && !resource.getCategory().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(LearningResource::getCategory, java.util.stream.Collectors.counting()));
        return REQUIRED_CATEGORIES.stream().allMatch(category -> counts.getOrDefault(category, 0L) >= MIN_COURSES_PER_CATEGORY);
    }

    private void apply(LearningResource resource, SeedCourse course, OffsetDateTime fetchedAt) {
        resource.setTitle(course.title());
        resource.setProvider(course.provider());
        resource.setCategory(course.category());
        resource.setSubject(course.subject());
        resource.setDescription(course.description());
        resource.setSummary(course.description());
        resource.setCourseUrl(course.courseUrl());
        resource.setUrl(course.courseUrl());
        resource.setThumbnailUrl(course.thumbnailUrl());
        resource.setLevel(course.level());
        resource.setDifficulty(course.level());
        resource.setLanguage(course.language());
        resource.setFree(course.isFree());
        resource.setSourceType(course.sourceType());
        resource.setResourceType("Course");
        resource.setEstimatedMinutes(course.estimatedMinutes());
        resource.setTags(String.join(",", List.of(course.category(), course.subject(), course.provider())));
        resource.setLastFetchedAt(fetchedAt);
        resource.setActive(true);
    }

    private String dedupeKey(SeedCourse course) {
        return normalize(course.title()) + "|" + normalize(course.provider()) + "|" + normalize(course.courseUrl());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<SeedCourse> seedCourses() {
        return List.of(
                new SeedCourse("Khan Academy Algebra 1", "Khan Academy", "Mathematics", "Algebra", "Work through equations, expressions, and algebra foundations with guided videos and practice.", "https://www.khanacademy.org/math/algebra", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 360),
                new SeedCourse("Khan Academy High School Geometry", "Khan Academy", "Mathematics", "Geometry", "Triangles, circles, proofs, and measurement concepts with step-by-step practice.", "https://www.khanacademy.org/math/geometry-home", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 320),
                new SeedCourse("Siyavula Grade 10 Mathematics", "Siyavula", "Mathematics", "Mathematics", "South African open mathematics textbook and practice for Grade 10 learners.", "https://www.siyavula.com/read/maths/grade-10", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 300),
                new SeedCourse("Siyavula Grade 12 Mathematics", "Siyavula", "Mathematics", "Mathematics", "Senior mathematics explanations, worked examples, and revision support aligned to local curricula.", "https://www.siyavula.com/read/maths/grade-12", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 320),
                new SeedCourse("MIT OCW Single Variable Calculus", "MIT OpenCourseWare", "Mathematics", "Calculus", "Open calculus lectures, notes, and assignments for stronger analytical problem solving.", "https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/", null, "Advanced", "English", true, "CURATED_PUBLIC_LINK", 480),

                new SeedCourse("Khan Academy High School Biology", "Khan Academy", "Science", "Biology", "Cells, genetics, ecology, and body systems explained in learner-friendly lessons.", "https://www.khanacademy.org/science/high-school-biology", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 300),
                new SeedCourse("Khan Academy High School Physics", "Khan Academy", "Science", "Physics", "Motion, forces, energy, and waves with practice and worked examples.", "https://www.khanacademy.org/science/high-school-physics", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 340),
                new SeedCourse("Khan Academy High School Chemistry", "Khan Academy", "Science", "Chemistry", "Atomic structure, reactions, and matter topics supported by quizzes and explanations.", "https://www.khanacademy.org/science/high-school-chemistry", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 340),
                new SeedCourse("DBE Cloud Physical Sciences", "DBE Cloud", "Science", "Physical Sciences", "Department of Basic Education science resources for revision and classroom practice.", "https://dbecloud.org.za/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("Mindset Learn Science Revision", "Mindset Learn", "Science", "Science", "Video-rich South African science revision resources for exam preparation and concept reinforcement.", "https://learn.mindset.africa/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 240),

                new SeedCourse("freeCodeCamp Responsive Web Design", "freeCodeCamp", "Coding", "Web Development", "Hands-on HTML and CSS curriculum with projects for beginner coders.", "https://www.freecodecamp.org/learn/2022/responsive-web-design/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 600),
                new SeedCourse("freeCodeCamp JavaScript Algorithms and Data Structures", "freeCodeCamp", "Coding", "JavaScript", "Practical JavaScript, debugging, and algorithm exercises with certification-style projects.", "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures-v8/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 720),
                new SeedCourse("W3Schools Learn Python", "W3Schools", "Coding", "Python", "Interactive Python basics with syntax examples, quick tests, and exercises.", "https://www.w3schools.com/python/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("W3Schools Learn SQL", "W3Schools", "Coding", "SQL", "Practice database queries and table operations through interactive SQL tutorials.", "https://www.w3schools.com/sql/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("GeeksforGeeks Data Structures Tutorial", "GeeksforGeeks", "Coding", "Data Structures", "Reference and guided examples for arrays, stacks, queues, trees, and graphs.", "https://www.geeksforgeeks.org/data-structures/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 300),

                new SeedCourse("Alison Entrepreneurship Fundamentals", "Alison", "Business", "Entrepreneurship", "Learn idea validation, business planning, and startup basics through free modules.", "https://alison.com/tag/entrepreneurship", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("OpenLearn Introduction to Project Management", "OpenLearn", "Business", "Project Management", "Develop planning, coordination, and project delivery skills with OpenLearn.", "https://www.open.edu/openlearn/money-business/introduction-project-management/content-section-0", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Diploma in Business Management", "Alison", "Business", "Business Management", "Explore core business operations, teamwork, communication, and management practice.", "https://alison.com/course/diploma-in-business-management-and-entrepreneurship", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 360),
                new SeedCourse("OpenLearn Marketing in the 21st Century", "OpenLearn", "Business", "Marketing", "Understand modern marketing, customers, and digital business communication.", "https://www.open.edu/openlearn/money-business/marketing-21st-century/content-section-0", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Customer Service Skills", "Alison", "Business", "Customer Service", "Build practical communication and service skills for business and workplace readiness.", "https://alison.com/tag/customer-service", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),

                new SeedCourse("MIT OCW Engineering Mechanics", "MIT OpenCourseWare", "Engineering", "Engineering Mechanics", "Open engineering fundamentals covering statics, motion, and forces.", "https://ocw.mit.edu/courses/2-001-mechanics-materials-i-fall-2006/", null, "Advanced", "English", true, "CURATED_PUBLIC_LINK", 420),
                new SeedCourse("Cisco Networking Basics", "Cisco Networking Academy", "Engineering", "Networking", "Introductory networking concepts, devices, addressing, and troubleshooting.", "https://www.netacad.com/courses/networking-basics", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 360),
                new SeedCourse("MIT OCW Introduction to Civil Engineering Systems", "MIT OpenCourseWare", "Engineering", "Civil Engineering", "Study infrastructure systems, engineering decisions, and design thinking.", "https://ocw.mit.edu/courses/1-017-computing-and-data-analysis-for-environmental-applications-fall-2003/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 300),
                new SeedCourse("Cisco Introduction to Cybersecurity", "Cisco Networking Academy", "Engineering", "Cybersecurity", "Learn digital safety, networks, and technical protection concepts from Cisco.", "https://www.netacad.com/courses/introduction-to-cybersecurity", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("MIT OCW Introduction to Computer System Engineering", "MIT OpenCourseWare", "Engineering", "Systems Engineering", "Open systems engineering materials covering hardware, software, and design integration.", "https://ocw.mit.edu/courses/6-033-computer-system-engineering-spring-2018/", null, "Advanced", "English", true, "CURATED_PUBLIC_LINK", 420),

                new SeedCourse("Microsoft Learn Get Started with Artificial Intelligence on Azure", "Microsoft Learn", "AI & Technology", "Artificial Intelligence", "Microsoft Learn path covering AI concepts, responsible AI, and Azure AI services.", "https://learn.microsoft.com/en-us/training/paths/get-started-with-artificial-intelligence-on-azure/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("Microsoft Learn Get Started with Machine Learning", "Microsoft Learn", "AI & Technology", "Machine Learning", "Build machine learning foundations and core terminology through interactive learning modules.", "https://learn.microsoft.com/en-us/training/paths/introduction-to-machine-learning/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("Microsoft Learn Fundamentals of Data Analytics", "Microsoft Learn", "AI & Technology", "Data Analytics", "Learn data concepts, analytics workflows, and reporting foundations.", "https://learn.microsoft.com/en-us/training/paths/data-analytics-microsoft/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 220),
                new SeedCourse("GeeksforGeeks Artificial Intelligence Tutorial", "GeeksforGeeks", "AI & Technology", "Artificial Intelligence", "Overview of AI concepts, models, and applications presented in accessible tutorials.", "https://www.geeksforgeeks.org/artificial-intelligence-tutorial/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 200),
                new SeedCourse("freeCodeCamp Machine Learning with Python", "freeCodeCamp", "AI & Technology", "Machine Learning", "Practical Python-based machine learning lessons and project-based exercises.", "https://www.freecodecamp.org/learn/machine-learning-with-python/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 480),

                new SeedCourse("Alison Financial Accounting Fundamentals", "Alison", "Accounting", "Accounting", "Core accounting principles for journals, ledgers, statements, and reporting.", "https://alison.com/tag/accounting", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 240),
                new SeedCourse("Khan Academy Financial Accounting", "Khan Academy", "Accounting", "Financial Accounting", "Read financial statements and understand core accounting transactions and principles.", "https://www.khanacademy.org/economics-finance-domain/core-finance/accounting-and-financial-statemts", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 260),
                new SeedCourse("OpenLearn Bookkeeping and Accounting", "OpenLearn", "Accounting", "Bookkeeping", "Practical bookkeeping and accounting basics for learners building finance readiness.", "https://www.open.edu/openlearn/money-business/introduction-bookkeeping-and-accounting/content-section-0", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Diploma in Accounting", "Alison", "Accounting", "Accounting", "Broader accounting coverage including costing, controls, and reporting skills.", "https://alison.com/course/diploma-in-accounting-revised", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 360),
                new SeedCourse("Khan Academy Accounting and Financial Statements", "Khan Academy", "Accounting", "Financial Statements", "Use worked examples to understand income statements, balance sheets, and cash flow.", "https://www.khanacademy.org/economics-finance-domain/core-finance/accounting-and-financial-statemts/financial-statements-tutorial", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 200),

                new SeedCourse("OpenLearn English Grammar and Style", "OpenLearn", "Languages", "English", "Improve writing clarity, grammar, and communication through self-paced lessons.", "https://www.open.edu/openlearn/languages/english-language/english-grammar-style/content-section-0", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Academic Reading and Note Taking", "Alison", "Languages", "English", "Reading strategies, note making, and academic study communication skills for learners.", "https://alison.com/tag/english", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 150),
                new SeedCourse("Khan Academy Grammar", "Khan Academy", "Languages", "Grammar", "Strengthen grammar, punctuation, and sentence construction with practice.", "https://www.khanacademy.org/humanities/grammar", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("OpenLearn Effective Communication", "OpenLearn", "Languages", "Communication", "Develop clearer reading, listening, and written communication for study and work.", "https://www.open.edu/openlearn/money-business/leadership-management/communication-skills/content-section-0", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Writing for Business", "Alison", "Languages", "Writing", "Write clear emails, reports, and formal communication with practical examples.", "https://alison.com/tag/writing", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),

                new SeedCourse("WCED ePortal Grade 12 Revision", "WCED ePortal", "Exam Preparation", "Exam Preparation", "Western Cape revision support, past papers, and learner resources for Grade 12 preparation.", "https://wcedeportal.co.za/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("DBE Cloud Revision Support", "DBE Cloud", "Exam Preparation", "Exam Preparation", "DBE learning resources that support revision, content review, and practice preparation.", "https://dbecloud.org.za/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Mindset Learn Exam Revision", "Mindset Learn", "Exam Preparation", "Exam Preparation", "Video-based revision resources for South African learners preparing for assessments.", "https://learn.mindset.africa/", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Siyavula Practice and Tests", "Siyavula", "Exam Preparation", "Exam Preparation", "Use open textbook exercises and practice to reinforce exam readiness in core subjects.", "https://www.siyavula.com/read", null, "Intermediate", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Khan Academy Test Prep Skills", "Khan Academy", "Exam Preparation", "Study Skills", "Build revision habits, practice routines, and confidence for academic testing.", "https://www.khanacademy.org/college-careers-more/learnstorm-growth-mindset-activities-us", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 120),

                new SeedCourse("OpenLearn Career Planning for High School Learners", "OpenLearn", "Career Guidance", "Career Guidance", "Career planning, goal setting, and transition advice for learners preparing for study and work.", "https://www.open.edu/openlearn/money-business/careers/career-planning/content-section-overview", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 120),
                new SeedCourse("Alison Career Discovery and Skills Pathways", "Alison", "Career Guidance", "Career Guidance", "Explore practical career planning and transferable skills development through free learning content.", "https://alison.com/tag/career-development", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 150),
                new SeedCourse("Microsoft Learn Career Essentials in Generative AI", "Microsoft Learn", "Career Guidance", "Digital Careers", "Understand emerging AI careers and the practical skills learners can start building now.", "https://learn.microsoft.com/en-us/training/paths/career-essentials-in-generative-ai/", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("OpenLearn Succeeding in the Workplace", "OpenLearn", "Career Guidance", "Work Readiness", "Learn professional habits, workplace communication, and self-management for career readiness.", "https://www.open.edu/openlearn/money-business/succeeding-the-workplace/content-section-0", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180),
                new SeedCourse("Alison Job Search Skills", "Alison", "Career Guidance", "Work Readiness", "Strengthen CV, interview, and job-search planning skills with free self-paced modules.", "https://alison.com/tag/job-search", null, "Beginner", "English", true, "CURATED_PUBLIC_LINK", 180)
        );
    }

    public record RefreshSummary(int total, int created, int updated, OffsetDateTime refreshedAt) {
    }

    private record SeedCourse(
            String title,
            String provider,
            String category,
            String subject,
            String description,
            String courseUrl,
            String thumbnailUrl,
            String level,
            String language,
            boolean isFree,
            String sourceType,
            int estimatedMinutes
    ) {
    }
}
