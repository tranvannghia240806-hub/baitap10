package vn.iotstar.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleSecurityException(Exception exception) {
        ProblemDetail errorDetail = null;

        if (exception instanceof ErrorResponse errorResponse) {
            return errorResponse.getBody();
        }
        if (exception instanceof HttpMessageNotReadableException) {
            ProblemDetail bad = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
            bad.setProperty("description", "The request body is not valid JSON");
            return bad;
        }

        exception.printStackTrace();

        if (exception instanceof BadCredentialsException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
            errorDetail.setProperty("description", "The username or password is incorrect");
            return errorDetail;
        }
        if (exception instanceof AccountStatusException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
            errorDetail.setProperty("description", "The account is locked");
        }
        if (exception instanceof AccessDeniedException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
            errorDetail.setProperty("description", "You are not authorized to access this resource");
        }
        if (exception instanceof SignatureException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
            errorDetail.setProperty("description", "The JWT signature is invalid");
        }
        if (exception instanceof ExpiredJwtException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
            errorDetail.setProperty("description", "The JWT token has expired");
        }
        if (errorDetail == null && exception instanceof JwtException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
            errorDetail.setProperty("description", "The JWT token is invalid");
        }
        if (errorDetail == null && exception instanceof UsernameNotFoundException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
            errorDetail.setProperty("description", "The user of this token no longer exists");
        }
        if (errorDetail == null && exception instanceof DataIntegrityViolationException) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email already exists");
            errorDetail.setProperty("description", "Email is already registered");
        }
        if (errorDetail == null) {
            errorDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
            errorDetail.setProperty("description", "Unknown internal server error.");
        }
        return errorDetail;
    }
}