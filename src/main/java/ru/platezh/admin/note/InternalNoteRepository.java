package ru.platezh.admin.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InternalNoteRepository extends JpaRepository<InternalNote, UUID> {
    List<InternalNote> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
}
