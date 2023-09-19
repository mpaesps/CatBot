package com.mayara.catbot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mayara.catbot.models.FactsResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.*;

@Service
@Slf4j
public class PostService {

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bearer.token}")
    private String bearerToken;

    @Value("${twitter.api.endpoint}")
    private String twitterApiEndpoint;

    @Value("${catfacts.api.endpoint}")
    private String catFactsApiEndpoint;

    @Value("${catpics.api.endpoint}")
    private String catPicsApiEndpoint;

    @Value("${twitter.oauth}")
    private String OAUTH_STRING;

    @Scheduled(cron = "0 0 12,18 * * ?")
    public void orquestraPostNoTwitter() {

        try {
            FactsResponse factsResponse = objectMapper.readValue(retornaFatoAleatorio(), FactsResponse.class);
            fazPostNoTwitter(factsResponse.getData().get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String realizaHttpRequest(String url) throws IOException {

        log.info("Realizando chamada HTTP no endpoint: " + url);

        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }

    private String retornaFatoAleatorio() throws IOException {
        return realizaHttpRequest(catFactsApiEndpoint);
    }

    private String retornaFotoAleatoria() throws IOException {
        return realizaHttpRequest(catPicsApiEndpoint);
    }

    private void fazPostNoTwitter(String texto) throws IOException {

        log.info("Criando objeto JSON");

        JsonObject jsonBody = new JsonObject();
        jsonBody.add("text", new JsonPrimitive(texto));
        StringEntity stringEntity = new StringEntity(new Gson().toJson(jsonBody));

        log.info("Objeto JSON criado com sucesso. Texto a ser postado: " + texto);

        HttpPost httpPost = new HttpPost(twitterApiEndpoint);
        httpPost.setEntity(stringEntity);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader(HttpHeaders.AUTHORIZATION,OAUTH_STRING);

        log.info("Executando envio de tweet...");

        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();

        if(statusCode == 201) {
            log.info("Tweet postado com sucesso.");
        }

        if (statusCode != 201) {
            log.error("Falha ao postar no Twitter: Código de Status " + statusCode);
        }
    }
}
