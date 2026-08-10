package pt.gestorflow.backend.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor // 🚀 O Lombok cria o construtor do TemplateEngine por ti
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    public byte[] generatePdf(String templateName, Map<String, Object> variables) {
        log.debug("A iniciar geração de PDF usando o template: {}", templateName);
        long startTime = System.currentTimeMillis(); // ⏱️ Iniciamos o cronómetro

        Context context = new Context();
        context.setVariables(variables);

        try {
            String htmlContent = templateEngine.process(templateName, context);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(htmlContent, null);
                builder.toStream(outputStream);
                builder.run();

                byte[] pdfBytes = outputStream.toByteArray();

                // ⏱️ Telemetria de performance: Essencial para detetar lentidão no servidor
                log.debug("PDF gerado com sucesso a partir do template '{}'. Tamanho: {} bytes. Tempo de execução: {}ms",
                        templateName, pdfBytes.length, (System.currentTimeMillis() - startTime));

                return pdfBytes;
            }
        } catch (Exception e) {
            // 🚨 CONTEXTO TOTAL: Sabemos qual foi o template que falhou e quais foram as variáveis enviadas (apenas os nomes, para manter a segurança)
            log.error("Erro crítico ao tentar gerar o PDF do template '{}'. Variáveis fornecidas: {}", templateName, variables.keySet(), e);
            throw new IllegalStateException("Ocorreu um erro interno ao gerar o documento PDF.", e);
        }
    }
}