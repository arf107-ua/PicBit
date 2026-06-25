package com.imagepipeline.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

@Configuration
@EnableJpaRepositories(basePackages = "com.imagepipeline.api.repository.pg")
public class SupabaseConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket}")
    private String storageBucket;

    // RestTemplate preconfigurado con las cabeceras de Supabase
    // Se usa en StorageService para subir/bajar archivos via Storage API REST
    @Bean
    public RestTemplate supabaseRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    public String getSupabaseUrl()     { return supabaseUrl; }
    public String getSupabaseKey()     { return supabaseKey; }
    public String getStorageBucket()   { return storageBucket; }
}
