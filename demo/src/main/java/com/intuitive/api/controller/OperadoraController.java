package com.intuitive.api.controller;

import com.intuitive.api.dto.OperadoraDTO;
import com.intuitive.api.repository.OperadoraRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller para endpoints de operadoras. Trade-off: Controller direto
 * sem Service Layer - Simplicidade para MVP. Em produção, adicionar camada de
 * serviço para lógica de negócio.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")  // Permite acesso do Vue.js (localhost:5173)
public class OperadoraController {

    private final OperadoraRepository repository;

    public OperadoraController(OperadoraRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista operadoras com paginação e busca.
     *
     * GET /api/operadoras?page=1&limit=20&q=termo
     *
     * @param page número da página (padrão: 1)
     * @param limit registros por página (padrão: 20)
     * @param q termo de busca (opcional)
     * @return JSON com data, total e page
     */
    @GetMapping("/operadoras")
    public ResponseEntity<Map<String, Object>> listarOperadoras(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String q
    ) {
        // Validação básica
        if (page < 1) {
            page = 1;
        }
        if (limit < 1 || limit > 100) {
            limit = 20;
        }

        List<OperadoraDTO> operadoras;
        int total;

        if (q != null && !q.trim().isEmpty()) {
            operadoras = repository.findByQuery(q.trim(), page, limit);
            total = repository.countByQuery(q.trim());
        } else {
            operadoras = repository.findAllPaginado(page, limit);
            total = repository.countTotal();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", operadoras);
        response.put("total", total);
        response.put("page", page);
        response.put("limit", limit);
        response.put("totalPages", (int) Math.ceil((double) total / limit));

        return ResponseEntity.ok(response);
    }

    /**
     * Busca operadora por CNPJ com histórico de despesas.
     *
     * GET /api/operadoras/{cnpj}
     *
     * @param cnpj CNPJ da operadora
     * @return JSON com dados da operadora e histórico
     */
    @GetMapping("/operadoras/{cnpj}")
    public ResponseEntity<OperadoraDTO> buscarOperadora(@PathVariable String cnpj) {
        OperadoraDTO operadora = repository.findByCnpjWithHistory(cnpj);

        if (operadora == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(operadora);
    }

    /**
     * Busca detalhes completos da operadora incluindo histórico estruturado.
     *
     * GET /api/operadoras/{cnpj}/detalhes
     *
     * @param cnpj CNPJ da operadora
     * @return JSON com dados cadastrais completos e histórico de despesas
     */
    @GetMapping("/operadoras/{cnpj}/detalhes")
    public ResponseEntity<com.intuitive.api.dto.OperadoraDetalhadaDTO> buscarDetalhesOperadora(@PathVariable String cnpj) {
        com.intuitive.api.dto.OperadoraDetalhadaDTO detalhes = repository.findDetalhesCompletos(cnpj);

        if (detalhes == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detalhes);
    }

    /**
     * Retorna estatísticas: Top 5 operadoras com maiores despesas.
     *
     * GET /api/estatisticas/top5
     *
     * @return JSON com lista das top 5
     */
    @GetMapping("/estatisticas/top5")
    public ResponseEntity<Map<String, Object>> top5Despesas() {
        List<OperadoraDTO> top5 = repository.findTop5Despesas();

        Map<String, Object> response = new HashMap<>();
        response.put("data", top5);
        response.put("title", "Top 5 Operadoras - Maiores Despesas");

        return ResponseEntity.ok(response);
    }

    /**
     * Retorna média de gastos por código de conta.
     *
     * GET /api/estatisticas/media-conta
     *
     * @return JSON com estatísticas por conta
     */
    @GetMapping("/estatisticas/media-conta")
    public ResponseEntity<Map<String, Object>> mediaPorConta() {
        List<OperadoraRepository.EstatisticaContaDTO> stats = repository.findMediaPorConta();

        Map<String, Object> response = new HashMap<>();
        response.put("data", stats);
        response.put("title", "Média de Gastos por Código de Conta");

        return ResponseEntity.ok(response);
    }

    /**
     * Health check da API.
     *
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "API Intuitive Care rodando");
        return ResponseEntity.ok(status);
    }
}
