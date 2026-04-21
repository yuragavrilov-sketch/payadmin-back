package ru.copperside.admin.operator;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Кэширует профиль оператора из Keycloak в локальной БД при первом обращении
 * и обновляет last_seen_at + меняющиеся атрибуты при последующих.
 */
@Service
public class OperatorService {

    private final OperatorRepository repository;

    public OperatorService(OperatorRepository repository) {
        this.repository = repository;
    }

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
                .orElseGet(() -> {
                    Operator op = new Operator();
                    op.setKeycloakSub(sub);
                    op.setEmail(email != null ? email : sub + "@unknown");
                    op.setDisplayName(name);
                    op.setRole("operator");
                    op.setLastSeenAt(Instant.now());
                    return repository.save(op);
                });
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "unknown";
    }
}
