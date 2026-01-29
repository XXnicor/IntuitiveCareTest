package com.intuitive.crawler;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
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

    public List <String> findRecentTrimesters(int count) throws IOException {
        if (count <= 0) {
            throw new IllegalArgumentException("A quantidade de trimestres deve ser maior que zero.");
        }
        List<String> yearUrls = fetchYearsUrls(count).reversed();
        
        List<String> result = new ArrayList<>();

        for (String yearUrl : yearUrls) {
         if (result.size() >=count){
            break;
         }

         List<String> trimesters = fetchTrimestersUrls(yearUrl);

        }

    }
    public  List <String> fetchYearsUrls(int count) throws IOException {
        return fetchUrlsByPattern(BASE_URL, YEAR_PATTERN);
    }
    public List<String> fetchTrimestersUrls(String yearUrl) throws IOException {
        return fetchUrlsByPattern(yearUrl, TRIMESTRE_PATTERN);
    }
    public List<String> fetchZipUrls(String trimesterUrl) throws IOException {
        return fetchUrlsByPattern(trimesterUrl, Pattern.compile(".*\\.zip$"));
    }
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