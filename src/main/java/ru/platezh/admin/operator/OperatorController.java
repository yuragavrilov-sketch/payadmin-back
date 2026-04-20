package ru.platezh.admin.operator;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.platezh.admin.common.CurrentOperator;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OperatorController {

    private final CurrentOperator currentOperator;

    @GetMapping("/me")
    public OperatorDto me() {
        Operator op = currentOperator.get();
        return new OperatorDto(op.getId(), op.getEmail(), op.getDisplayName(), op.getRole(), op.getLastSeenAt());
    }

    public record OperatorDto(UUID id, String email, String displayName, String role, Instant lastSeenAt) {}
}
