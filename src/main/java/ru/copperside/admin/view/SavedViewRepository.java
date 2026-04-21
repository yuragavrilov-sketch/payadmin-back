package ru.copperside.admin.view;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedViewRepository extends JpaRepository<SavedView, UUID> {
    List<SavedView> findByOperatorIdAndPageOrderByNameAsc(UUID operatorId, String page);
}
