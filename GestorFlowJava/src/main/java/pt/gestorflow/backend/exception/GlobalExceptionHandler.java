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

import java.util.HashMap;
import java.util.Map;

@Slf4j // Excelente: O Lombok já cria o logger por ti!
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Erros de Validação
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // REGISTO DE LOG (Nível: WARN)
        log.warn("Falha de validação no {} {}: {}", request.getMethod(), request.getRequestURI(), errors);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Erro de Validação: Verifique os campos submetidos.", request.getRequestURI());
    }

    // 2. Erros de Concorrência
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrência detectado no {} {}", request.getMethod(), request.getRequestURI());

        return buildErrorResponse(HttpStatus.CONFLICT, "Este registo foi alterado por outro utilizador. Por favor, recarregue a página.", request.getRequestURI());
    }

    // 3. Entidade não encontrada
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado no {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // 4. Erros de Negócio
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBusinessException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Regra de negócio quebrada no {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // 5. Empresa Não Configurada
    @ExceptionHandler(pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException.class)
    public ResponseEntity<ApiError> handleEmpresaNaoConfigurada(pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException ex, HttpServletRequest request) {
        log.warn("Tentativa de acesso sem empresa configurada no {} {}", request.getMethod(), request.getRequestURI());

        return buildErrorResponse(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), request.getRequestURI());
    }

    // 6. Erros Gerais (Bugs não esperados - NÍVEL ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
        // Aqui mantemos o nível ERROR e passamos a exceção completa para ter a stacktrace no JSON
        log.error("Erro interno não tratado no {} {}", request.getMethod(), request.getRequestURI(), ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.", request.getRequestURI());
    }

    // Método auxiliar unificado
    private ResponseEntity<ApiError> buildErrorResponse(HttpStatus status, String message, String path) {
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, path);
        return ResponseEntity.status(status).body(apiError);
    }
}