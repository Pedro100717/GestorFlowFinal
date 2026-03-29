package pt.gestorflow.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NifValidator implements ConstraintValidator<NifPT, String> {

    @Override
    public boolean isValid(String nif, ConstraintValidatorContext context) {
        if (nif == null || nif.isBlank()) {
            return true; // Se for vazio, a anotação @NotBlank é que barra (se for obrigatório)
        }

        // 1. Tem de ter exatamente 9 dígitos
        if (!nif.matches("[0-9]{9}")) {
            return false;
        }

        // 2. Validar prefixos oficiais em Portugal
        char c1 = nif.charAt(0);
        char c2 = nif.charAt(1);

        boolean prefixoValido = (c1 == '1' || c1 == '2' || c1 == '3' || c1 == '5' || c1 == '6' || c1 == '8');
        if (!prefixoValido && c1 == '4' && c2 == '5') prefixoValido = true; // Cidadãos não residentes
        if (!prefixoValido && c1 == '7' && (c2 == '0' || c2 == '1' || c2 == '2' || c2 == '4' || c2 == '5' || c2 == '7' || c2 == '9')) prefixoValido = true; // Fundos, etc.
        if (!prefixoValido && c1 == '9' && (c2 == '0' || c2 == '1' || c2 == '8' || c2 == '9')) prefixoValido = true; // Condomínios, Sociedades Civis, etc.

        if (!prefixoValido) {
            return false;
        }

        // 3. Algoritmo Modulo 11
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += Character.getNumericValue(nif.charAt(i)) * (9 - i);
        }

        int resto = soma % 11;
        // Se o resto for 0 ou 1, o dígito de controlo é 0. Senão, é 11 - resto.
        int digitoControloEsperado = (resto < 2) ? 0 : (11 - resto);
        int digitoControloFornecido = Character.getNumericValue(nif.charAt(8));

        return digitoControloEsperado == digitoControloFornecido;
    }
}