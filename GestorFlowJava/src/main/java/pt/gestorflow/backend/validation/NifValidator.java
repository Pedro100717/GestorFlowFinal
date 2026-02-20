package pt.gestorflow.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NifValidator implements ConstraintValidator<NifPT, String> {

    @Override
    public boolean isValid(String nif, ConstraintValidatorContext context) {
        if (nif == null || nif.isBlank()) {
            return true; // Se for null, o @NotNull que valida, não nós. Aqui só validamos o formato SE existir.
        }

        // 1. Tem de ter 9 dígitos numéricos
        if (!nif.matches("[0-9]{9}")) {
            return false;
        }

        // 2. O primeiro dígito tem de ser válido (1,2,3 pessoas, 5,6 empresas, etc.)
        char primeiroDigito = nif.charAt(0);
        if ("1235689".indexOf(primeiroDigito) == -1) {
            // Nota: 45, 70, 71, 72, 74, 75, 77, 79, 90, 91, 98, 99 são prefixos validos em PT
            // Simplificação para o exemplo: aceitamos os mais comuns.
        }

        // 3. Algoritmo Modulo 11 (O cálculo oficial das Finanças)
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += Character.getNumericValue(nif.charAt(i)) * (9 - i);
        }

        int resto = soma % 11;
        int digitoControloCalculado = (resto < 2) ? 0 : 11 - resto;
        int digitoControloReal = Character.getNumericValue(nif.charAt(8));

        return digitoControloCalculado == digitoControloReal;
    }
}