package com.kotopai.outing.api;

import com.kotopai.outing.domain.IllegalOutingTransitionException;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.service.GuestOperationException;
import com.kotopai.outing.service.OutingOperationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns domain refusals into consistent JSON problem responses (RFC 9457) so the UI can show a precise message and
 * the controllers stay free of try/catch.
 */
@RestControllerAdvice(basePackages = "com.kotopai.outing.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OutingApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Some fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(Exception e) {
        String detail = e instanceof IllegalArgumentException ? e.getMessage() : "Request body is missing or malformed";
        return problem(HttpStatus.BAD_REQUEST, "Bad request", detail);
    }

    @ExceptionHandler(EngineValidationException.class)
    ProblemDetail engineValidation(EngineValidationException e) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "The outing cannot be calculated yet", e.getMessage());
        List<Map<String, String>> issues = e.getResult().issues().stream()
            .map(i -> Map.of("code", i.code(), "message", i.message())).toList();
        problem.setProperty("issues", issues);
        return problem;
    }

    @ExceptionHandler(IllegalOutingTransitionException.class)
    ProblemDetail illegalTransition(IllegalOutingTransitionException e) {
        return problem(HttpStatus.CONFLICT, "Not allowed in the current status", e.getMessage());
    }

    @ExceptionHandler(OutingOperationException.class)
    ProblemDetail outingRefused(OutingOperationException e) {
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_A_FRIEND, UNKNOWN_GUEST -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DRAFT_ONLY -> HttpStatus.CONFLICT;
        };
        return problem(status, status.getReasonPhrase(), e.getMessage());
    }

    @ExceptionHandler(GuestOperationException.class)
    ProblemDetail guestRefused(GuestOperationException e) {
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_NAME, IN_USE -> HttpStatus.CONFLICT;
        };
        return problem(status, status.getReasonPhrase(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail notAuthenticated(AuthenticationException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Please log in");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
