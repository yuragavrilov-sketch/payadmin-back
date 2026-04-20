package ru.platezh.admin.operator;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Кэширует профиль оператора из Keycloak в локальной БД при первом обращении
 * и обновляет last_seen_at + меняющиеся атрибуты при последующих.
 */
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository repository;

    @Transactional
    public Operator syncFromJwt(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                email,
                sub
        );

        return repository.findByKeycloakSub(sub)
                .map(existing -> {
                    existing.setEmail(email != null ? email : existing.getEmail());
                    existing.setDisplayName(name);
                    existing.setLastSeenAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> repository.save(Operator.builder()
                        .keycloakSub(sub)
                        .email(email != null ? email : sub + "@unknown")
                        .displayName(name)
                        .role("operator")
                        .lastSeenAt(Instant.now())
                        .build()));
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "unknown";
    }
}
