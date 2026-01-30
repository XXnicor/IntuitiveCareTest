package com.intuitive.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AnsCrawlerService {

    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/";
    private static final int TIMEOUT_MILLISECONDS = 10_000;
    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{4}/$");
    private static final Pattern TRIMESTRE_PATTERN = Pattern.compile("^[1-4]T\\d{4}/$");
    
    /**
    *
    * Estrategia adotada :
    * - Busca os anos em ordem decrescente (2025,2024,...)
    * - Para cada ano busca os trimestres em ordem decrescente (4T2025,3T2025,...)
    * - Retorna 1 arquivo .zip por trimestre
    *
    * TRADE-OFF: Busca todos os anos/trimestres (eager loading).
    * Para N pequeno (~3), é mais simples que lazy loading.
    * 
    * @param count Número de trimestres recentes a serem retornados.
    * @return Lista de URLs dos arquivos .zip dos trimestres recentes.
    * @throws IOException Se ocorrer um erro ao buscar os dados.
    **/
    
    public List <String> findRecentTrimesters(int count) throws IOException {
        if (count <= 0) {
            throw new IllegalArgumentException("A quantidade de trimestres deve ser maior que zero.");
        }

        // Buscar e ordenar os anos em ordem decrescente (2025,2024, ...)
        List<String> yearUrls = fetchYearsUrls()
        .stream()
        .sorted(Comparator.reverseOrder())
        .toList();
        
        List<String> result = new ArrayList<>();
        
        int trimestersCount = 0;
        
        //Iterar anos (mais recentes primeiro)
        for (String yearUrl : yearUrls) {
         if (trimestersCount >= count) {
            break;
         }
         
         //Buscar trimestres e ordenar (4T,3T,...)
         List<String> trimesters = fetchTrimestersUrls(yearUrl)
         .stream()
         .sorted(Comparator.reverseOrder())
         .toList();

         //Iterar trimestres (mais recentes primeiro)
        for (String trimesterUrl : trimesters) {

            if (trimestersCount >= count) {
                break;
            }
            
            //Buscar arquivos .zip (ex.: 4T2025.zip)
            List<String> zipUrls = fetchZipUrls(trimesterUrl);
          

            // Adicionar primeiro .zip (geralmente só tem 1 por trimestre)
            if (!zipUrls.isEmpty()) {
                result.add(zipUrls.get(0));
                //Decisão: contamos apenas trimestres que tem arquivos .zip
                trimestersCount++;
            }
         }
        }
        return result;
    }

    public  List <String> fetchYearsUrls() throws IOException {
        return fetchUrlsByPattern(BASE_URL, YEAR_PATTERN);
    }

    public List<String> fetchTrimestersUrls(String yearUrl) throws IOException {
        return fetchUrlsByPattern(yearUrl, TRIMESTRE_PATTERN);
    }

    public List<String> fetchZipUrls(String trimesterUrl) throws IOException {
        return fetchUrlsByPattern(trimesterUrl, Pattern.compile(".*\\.zip$"));
    }

    // Adotamos um Pattern para permitir flexibilidade na busca de diferentes tipos de URLs
    private List<String> fetchUrlsByPattern(String baseUrl, Pattern pattern) throws IOException {
        List<String> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(baseUrl)
                    .timeout(TIMEOUT_MILLISECONDS)
                    .get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String href = link.attr("href");

                if (pattern.matcher(href).matches()) {
                    String absoluteUrl = link.absUrl("href");
                    results.add(absoluteUrl);
                }
            }

        } catch (IOException e) {
            throw new IOException("Erro ao buscar URLs no site da ANS: " + e.getMessage(), e);
        }
        return results;
    }
}