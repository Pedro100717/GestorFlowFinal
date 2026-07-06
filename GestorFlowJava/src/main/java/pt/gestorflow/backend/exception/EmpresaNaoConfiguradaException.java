package pt.gestorflow.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// O Spring Boot converte automaticamente esta exceção num HTTP 412
@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
public class EmpresaNaoConfiguradaException extends RuntimeException {
    public EmpresaNaoConfiguradaException(String mensagem) {
        super(mensagem);
    }
}