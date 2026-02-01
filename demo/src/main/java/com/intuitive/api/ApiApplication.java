package com.intuitive.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da API Spring Boot.
 * 
 * Para rodar:
 * mvn spring-boot:run
 * 
 * Endpoints disponíveis:
 * - GET http://localhost:8080/api/health
 * - GET http://localhost:8080/api/operadoras?page=1&limit=20
 * - GET http://localhost:8080/api/estatisticas/top5
 * - GET http://localhost:8080/api/estatisticas/media-conta
 */
@SpringBootApplication
public class ApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}