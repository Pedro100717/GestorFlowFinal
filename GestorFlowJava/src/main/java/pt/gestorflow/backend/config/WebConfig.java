package pt.gestorflow.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Diz ao Spring Boot que tudo o que for pedido em /uploads/ deve ser lido da pasta local
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}