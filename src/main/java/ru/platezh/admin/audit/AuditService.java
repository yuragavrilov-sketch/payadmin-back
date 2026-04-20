package ru.platezh.admin.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platezh.admin.operator.Operator;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    @Transactional
    public AuditEvent record(Operator operator,
                             String action,
                             String entityType,
                             String entityId,
                             Map<String, Object> payload,
                             HttpServletRequest request) {
        AuditEvent event = AuditEvent.builder()
                .operator(operator)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .payload(payload != null ? payload : Map.of())
                .ip(request != null ? extractIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();
        return repository.save(event);
    }

    private static String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
