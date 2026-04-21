package ru.copperside.admin.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<AuditDto> list(
            @RequestParam String entityType,
            @RequestParam String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return repository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, PageRequest.of(page, size))
                .map(AuditDto::from);
    }

    public record AuditDto(
            Long id,
            String operatorName,
            String action,
            String entityType,
            String entityId,
            Map<String, Object> payload,
            Instant createdAt
    ) {
        static AuditDto from(AuditEvent e) {
            return new AuditDto(
                    e.getId(),
                    e.getOperator().getDisplayName(),
                    e.getAction(),
                    e.getEntityType(),
                    e.getEntityId(),
                    e.getPayload(),
                    e.getCreatedAt()
            );
        }
    }
}
