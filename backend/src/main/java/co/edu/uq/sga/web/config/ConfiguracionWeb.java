package co.edu.uq.sga.web.config;

import co.edu.uq.sga.web.seguridad.InterceptorAutenticacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorAutenticacion interceptor;
    private final String[] origenes;

    public ConfiguracionWeb(InterceptorAutenticacion interceptor,
                            @Value("${sga.cors.origenes:http://localhost:4200}") String[] origenes) {
        this.interceptor = interceptor;
        this.origenes = origenes;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
