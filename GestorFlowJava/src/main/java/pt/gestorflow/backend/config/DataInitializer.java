package pt.gestorflow.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pt.gestorflow.backend.model.TxIva;
import pt.gestorflow.backend.repository.TxIvaRepository;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TxIvaRepository txIvaRepository;

    @Override
    public void run(String... args) throws Exception {

        // Verifica se a tabela JÁ tem dados. Se tiver, não faz nada.
        if (txIvaRepository.count() == 0) {
            System.out.println("⚠️ A iniciar inserção de Taxas de IVA...");

            TxIva normal = new TxIva(null, "Taxa Normal", new BigDecimal("23.00"));
            TxIva intermedia = new TxIva(null, "Taxa Intermédia", new BigDecimal("13.00"));
            TxIva reduzida = new TxIva(null, "Taxa Reduzida", new BigDecimal("6.00"));
            TxIva isenta = new TxIva(null, "Isento", new BigDecimal("0.00"));

            txIvaRepository.saveAll(Arrays.asList(normal, intermedia, reduzida, isenta));

            System.out.println("✅ Taxas de IVA inseridas com sucesso!");
        }
    }
}