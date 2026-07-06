package pt.gestorflow.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.gestorflow.backend.config.ApiError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Erros de Validação (@NotNull, @Email, @NifPT)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Aqui enviamos as validações detalhadas na "message", ou podes criar um campo "validationErrors" no ApiError se quiseres ser mais chique.
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Erro de Validação: " + errors.toString(), request.getRequestURI());
    }

    // 2. Erros de Concorrência (@Version falhou)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Este registo foi alterado por outro utilizador. Por favor, recarregue a página.", request.getRequestURI());
    }

    // 3. Erro quando não encontra algo na BD (EntityNotFoundException)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // 4. Erros de Negócio (IllegalArgumentException é melhor que RuntimeException para negócio)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBusinessException(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // 5. Erros Gerais (Bugs não esperados)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Erro interno não tratado no servidor: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.", request.getRequestURI());
    }

    @ExceptionHandler(pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException.class)
    public ResponseEntity<ApiError> handleEmpresaNaoConfigurada(pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException ex, HttpServletRequest request) {
        // Retorna HTTP 412 PRECONDITION_FAILED
        return buildErrorResponse(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), request.getRequestURI());
    }

    // Método auxiliar unificado que devolve o objeto forte (ApiError)
    private ResponseEntity<ApiError> buildErrorResponse(HttpStatus status, String message, String path) {
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, path);
        return ResponseEntity.status(status).body(apiError);
    }
}