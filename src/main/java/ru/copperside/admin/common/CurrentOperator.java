package ru.copperside.admin.common;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.copperside.admin.operator.Operator;
import ru.copperside.admin.operator.OperatorService;

/**
 * Достаёт текущего оператора из SecurityContext, синхронизируя профиль из JWT при необходимости.
 */
@Component
public class CurrentOperator {

    private final OperatorService operatorService;

    public CurrentOperator(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    public Operator get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No JWT in security context");
        }
        Jwt jwt = jwtAuth.getToken();
        return operatorService.syncFromJwt(jwt);
    }
}
