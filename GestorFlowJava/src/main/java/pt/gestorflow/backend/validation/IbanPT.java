package pt.gestorflow.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {}) // Não precisa de classe lógica, usamos o Pattern do Java
@Pattern(regexp = "^PT50[0-9]{21}$", message = "O IBAN deve começar por PT50 e ter 21 dígitos numéricos")
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IbanPT {
    String message() default "IBAN inválido (Formato: PT50...)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}