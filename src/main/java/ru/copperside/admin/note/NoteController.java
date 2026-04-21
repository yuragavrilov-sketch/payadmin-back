package ru.copperside.admin.note;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import ru.copperside.admin.audit.AuditService;
import ru.copperside.admin.common.CurrentOperator;
import ru.copperside.admin.operator.Operator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final InternalNoteRepository repository;
    private final CurrentOperator currentOperator;
    private final AuditService auditService;

    public NoteController(InternalNoteRepository repository,
                          CurrentOperator currentOperator,
                          AuditService auditService) {
        this.repository = repository;
        this.currentOperator = currentOperator;
        this.auditService = auditService;
    }

    @GetMapping
    public List<NoteDto> list(@RequestParam String entityType, @RequestParam String entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(NoteDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDto create(@Valid @RequestBody CreateRequest req, HttpServletRequest http) {
        Operator op = currentOperator.get();
        InternalNote note = new InternalNote();
        note.setOperator(op);
        note.setEntityType(req.entityType());
        note.setEntityId(req.entityId());
        note.setBody(req.body());
        InternalNote saved = repository.save(note);
        auditService.record(op, "note.create", req.entityType(), req.entityId(),
                Map.of("noteId", saved.getId().toString()), http);
        return NoteDto.from(saved);
    }

    @PutMapping("/{id}")
    public NoteDto update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest req, HttpServletRequest http) {
        Operator op = currentOperator.get();
        InternalNote note = repository.findById(id).orElseThrow(NoSuchElementException::new);
        if (!note.getOperator().getId().equals(op.getId())) {
            throw new AccessDeniedException("Можно редактировать только свои заметки");
        }
        note.setBody(req.body());
        InternalNote saved = repository.save(note);
        auditService.record(op, "note.update", note.getEntityType(), note.getEntityId(),
                Map.of("noteId", id.toString()), http);
        return NoteDto.from(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest http) {
        Operator op = currentOperator.get();
        InternalNote note = repository.findById(id).orElseThrow(NoSuchElementException::new);
        if (!note.getOperator().getId().equals(op.getId())) {
            throw new AccessDeniedException("Можно удалять только свои заметки");
        }
        repository.delete(note);
        auditService.record(op, "note.delete", note.getEntityType(), note.getEntityId(),
                Map.of("noteId", id.toString()), http);
    }

    public record CreateRequest(
            @NotBlank String entityType,
            @NotBlank String entityId,
            @NotBlank String body
    ) {}

    public record UpdateRequest(@NotBlank String body) {}

    public record NoteDto(
            UUID id,
            String operatorName,
            String entityType,
            String entityId,
            String body,
            Instant createdAt,
            Instant updatedAt
    ) {
        static NoteDto from(InternalNote n) {
            return new NoteDto(
                    n.getId(),
                    n.getOperator().getDisplayName(),
                    n.getEntityType(),
                    n.getEntityId(),
                    n.getBody(),
                    n.getCreatedAt(),
                    n.getUpdatedAt()
            );
        }
    }
}
