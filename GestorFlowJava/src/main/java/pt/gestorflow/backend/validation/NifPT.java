package pt.gestorflow.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NifValidator.class) // Quem faz a lógica é a classe NifValidator
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NifPT {
    String message() default "NIF inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}