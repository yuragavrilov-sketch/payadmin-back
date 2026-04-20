package ru.platezh.admin.operator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorRepository extends JpaRepository<Operator, UUID> {
    Optional<Operator> findByKeycloakSub(String keycloakSub);
}
