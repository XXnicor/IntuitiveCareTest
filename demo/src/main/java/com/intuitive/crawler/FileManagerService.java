package com.intuitive.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Serviço responsavel por baixar e descompactar arquivos ZIP da ANS.
 * 
 * Trade-offs: Usa streaming (NIO.2) para evitar carregar arquivos grandes na memoria.
 * ALternativa rejeitada: carregar bytes [] inteiro (causa OOM em arquivos > 100MB).
 */

public class FileManagerService {

    private static final int TIMEOUT_SECONDS = 30;
    private final HttpClient httpClient;

    public FileManagerService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Baixa um arquivo da URL fornecida e salva no caminho de destino.
     * 
     * @param url URL do arquivo a ser baixado
     * @param destination Caminho de destino (incluindo nome do arquivo)
     * @return Path do arquivo baixado
     * @throws IOException Se ocorrer erro de rede ou IO
     */

     public Path downloadFile(String url, Path destination) throws IOException {
     
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("A URL não pode ser nula ou vazia.");
        }
    
        if (destination == null) {
            throw new IllegalArgumentException("O caminho de destino não pode ser nulo.");
        }
        
        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<InputStream> response;

        try{
            response = httpClient.send(
                request,
                 HttpResponse.BodyHandlers.ofInputStream());
           } 

    catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Download interrompido", e);
    }

    if(response.statusCode() != 200){
        throw new IOException("Falha ao baixar arquivo. Código de status: " + response.statusCode());
    }

    try (InputStream inputStream = response.body()) {
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    return destination;
}

/**
     * Descompacta um arquivo ZIP para um diretório de destino.
     * 
     * @param zipFile Caminho do arquivo ZIP
     * @param outputDir Diretório onde os arquivos serão extraídos
     * @throws IOException Se ocorrer erro ao ler o ZIP ou escrever arquivos
     */
    public void unzipFile(Path zipFile, Path outputDir) throws IOException {
        if (zipFile == null || !Files.exists(zipFile)) {
            throw new IllegalArgumentException("O arquivo ZIP não pode ser nulo e deve existir.");
        }

        if (outputDir == null) {
            throw new IllegalArgumentException("O diretório de saída não pode ser nulo.");
        }
            Files.createDirectories(outputDir);
    

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFilePath = outputDir.resolve(entry.getName()).normalize();

                if (!newFilePath.startsWith(outputDir)) {
                    throw new IOException("Entrada de ZIP com caminho inválido: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    Files.createDirectories(newFilePath.getParent());
                    Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
   
}



