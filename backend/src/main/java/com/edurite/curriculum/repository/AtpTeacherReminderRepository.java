package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.AtpTeacherReminder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtpTeacherReminderRepository extends JpaRepository<AtpTeacherReminder, UUID> {
    List<AtpTeacherReminder> findByTeacherIdAndReminderDateBetweenOrderByReminderDateAsc(UUID teacherId, OffsetDateTime from, OffsetDateTime to);
    List<AtpTeacherReminder> findBySchoolIdAndReminderDateBetweenOrderByReminderDateAsc(UUID schoolId, OffsetDateTime from, OffsetDateTime to);
    List<AtpTeacherReminder> findBySchoolIdAndTeacherIdIsNullAndReminderDateBetweenOrderByReminderDateAsc(UUID schoolId, OffsetDateTime from, OffsetDateTime to);
    List<AtpTeacherReminder> findBySchoolIdAndTeacherIdIsNullAndStatusIgnoreCaseOrderByReminderDateAsc(UUID schoolId, String status);
    List<AtpTeacherReminder> findByStatusIgnoreCaseAndReminderDateBetweenOrderByReminderDateAsc(String status, OffsetDateTime from, OffsetDateTime to);
    Optional<AtpTeacherReminder> findByAtpCalendarItemIdAndSchoolIdAndTeacherIdAndReminderType(UUID atpCalendarItemId, UUID schoolId, UUID teacherId, String reminderType);
    Optional<AtpTeacherReminder> findByAtpCalendarItemIdAndSchoolIdAndTeacherIdIsNullAndReminderType(UUID atpCalendarItemId, UUID schoolId, String reminderType);
    long countByStatusIgnoreCase(String status);
    long countBySchoolIdAndTeacherIdIsNullAndStatusIgnoreCase(UUID schoolId, String status);
}
