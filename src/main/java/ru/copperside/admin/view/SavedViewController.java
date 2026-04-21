package ru.copperside.admin.view;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.copperside.admin.common.CurrentOperator;
import ru.copperside.admin.operator.Operator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/views")
public class SavedViewController {

    private final SavedViewRepository repository;
    private final CurrentOperator currentOperator;

    public SavedViewController(SavedViewRepository repository, CurrentOperator currentOperator) {
        this.repository = repository;
        this.currentOperator = currentOperator;
    }

    @GetMapping
    public List<ViewDto> list(@RequestParam String page) {
        UUID operatorId = currentOperator.get().getId();
        return repository.findByOperatorIdAndPageOrderByNameAsc(operatorId, page)
                .stream().map(ViewDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ViewDto create(@Valid @RequestBody CreateRequest req) {
        Operator op = currentOperator.get();
        if (req.isDefault()) {
            unsetExistingDefault(op.getId(), req.page());
        }
        SavedView view = new SavedView();
        view.setOperator(op);
        view.setPage(req.page());
        view.setName(req.name());
        view.setFilters(req.filters() != null ? req.filters() : Map.of());
        view.setDefault(req.isDefault());
        return ViewDto.from(repository.save(view));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        Operator op = currentOperator.get();
        SavedView view = repository.findById(id).orElseThrow(NoSuchElementException::new);
        if (!view.getOperator().getId().equals(op.getId())) {
            throw new AccessDeniedException("Можно удалять только свои виды");
        }
        repository.delete(view);
    }

    private void unsetExistingDefault(UUID operatorId, String page) {
        repository.findByOperatorIdAndPageOrderByNameAsc(operatorId, page).stream()
                .filter(SavedView::isDefault)
                .forEach(v -> {
                    v.setDefault(false);
                    repository.save(v);
                });
    }

    public record CreateRequest(
            @NotBlank String page,
            @NotBlank String name,
            Map<String, Object> filters,
            boolean isDefault
    ) {}

    public record ViewDto(
            UUID id,
            String page,
            String name,
            Map<String, Object> filters,
            boolean isDefault,
            Instant createdAt
    ) {
        static ViewDto from(SavedView v) {
            return new ViewDto(
                    v.getId(),
                    v.getPage(),
                    v.getName(),
                    v.getFilters(),
                    v.isDefault(),
                    v.getCreatedAt()
            );
        }
    }
}
