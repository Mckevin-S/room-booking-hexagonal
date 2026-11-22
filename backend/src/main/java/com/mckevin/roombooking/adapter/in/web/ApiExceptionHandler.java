package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.application.ForbiddenException;
import com.mckevin.roombooking.application.InvalidCredentialsException;
import com.mckevin.roombooking.application.NotFoundException;
import com.mckevin.roombooking.domain.shared.BusinessRuleViolation;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erreurs au format Problem Details (RFC 7807), avec un code métier stable quand il y en a un. */
@RestControllerAdvice
class ApiExceptionHandler {

    /** Conflits avec d'autres réservations : 409 plutôt que 422, le client peut réessayer sur un autre créneau. */
    static final Set<String> CONFLICTS = Set.of("SLOT_TAKEN", "ORGANIZER_UNAVAILABLE");

    @ExceptionHandler(BusinessRuleViolation.class)
    ProblemDetail businessRule(BusinessRuleViolation e) {
        HttpStatus status = CONFLICTS.contains(e.code()) ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        ProblemDetail problem = problem(status, "Règle métier non respectée", e.getMessage());
        problem.setProperty("code", e.code());
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Introuvable", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return problem(HttpStatus.FORBIDDEN, "Accès refusé", e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentification refusée", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Modification concurrente",
                "La réservation vient d'être modifiée par ailleurs. Rechargez la page.");
        problem.setProperty("code", "CONCURRENT_UPDATE");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(" ; "));
        return problem(HttpStatus.BAD_REQUEST, "Champs invalides", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide",
                e instanceof IllegalArgumentException ? e.getMessage() : "La requête est mal formée ou incomplète.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
