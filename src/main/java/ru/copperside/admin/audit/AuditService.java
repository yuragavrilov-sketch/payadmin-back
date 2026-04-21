package ru.copperside.admin.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.copperside.admin.operator.Operator;

import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditEvent record(Operator operator,
                             String action,
                             String entityType,
                             String entityId,
                             Map<String, Object> payload,
                             HttpServletRequest request) {
        AuditEvent event = new AuditEvent();
        event.setOperator(operator);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setPayload(payload != null ? payload : Map.of());
        if (request != null) {
            event.setIp(extractIp(request));
            event.setUserAgent(request.getHeader("User-Agent"));
        }
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
