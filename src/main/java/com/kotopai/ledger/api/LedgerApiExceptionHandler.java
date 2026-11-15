package com.kotopai.ledger.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kotopai.ledger.service.LedgerOperationException;

@RestControllerAdvice(basePackages = "com.kotopai.ledger.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LedgerApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidBody(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Some fields are invalid");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(LedgerOperationException.class)
	ProblemDetail operationRefused(LedgerOperationException exception) {
		HttpStatus status = switch (exception.getReason()) {
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case CONFLICT -> HttpStatus.CONFLICT;
		};
		return problem(status, status.getReasonPhrase(), exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail badRequest(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail notAuthenticated(AuthenticationException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Please log in");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
