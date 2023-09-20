package com.mayara.catbot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mayara.catbot.models.FactsResponse;
import com.mayara.catbot.models.PicturesResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import com.google.gson.JsonArray;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PostService {

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${twitter.api.endpoint}")
    private String twitterApiEndpoint;

    @Value("${catfacts.api.endpoint}")
    private String catFactsApiEndpoint;

    @Value("${catpics.api.endpoint}")
    private String catPicsApiEndpoint;

    @Value("${twitter.oauth}")
    private String OAUTH_STRING;

    @Scheduled(fixedRate = 10000)
    public void orquestraPostNoTwitter() {

        try {
            FactsResponse factsResponse = objectMapper.readValue(retornaFatoAleatorio(), FactsResponse.class);

            List<PicturesResponse> picturesResponse = objectMapper.readValue(retornaFotoAleatoria(), new TypeReference<List<PicturesResponse>>() {
            });

            fazPostNoTwitter(factsResponse.getData().get(0), picturesResponse.get(0).getUrl());
        } catch (IOException | TwitterException | InterruptedException e) {
            throw new RuntimeException(e);
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

    private void fazPostNoTwitter(String texto, String urlImagem) throws TwitterException, IOException, InterruptedException {

        Twitter twitter = Twitter.getInstance();
        fazDownloadImagem(urlImagem);

        String mediaId = String.valueOf(twitter.v1().tweets().uploadMedia(new File("image.jpg")).getMediaId());

        HttpPost httpPost = new HttpPost(twitterApiEndpoint);
        httpPost.setEntity(criaObjetoJsonPost(texto, mediaId));

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, OAUTH_STRING);

        log.info("Executando envio de tweet...");

        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 201) {
            log.info("Tweet postado com sucesso.");
        }

        if (statusCode != 201) {
            log.error("Falha ao postar no Twitter: Código de Status " + statusCode);
        }
    }


    public void fazDownloadImagem(String url) throws IOException {

        HttpGet httpGet = new HttpGet(url);

        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();

        InputStream inputStream = entity.getContent();
        OutputStream outputStream = new FileOutputStream("image.jpg");

        int bytesRead;
        byte[] buffer = new byte[1024];

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

    }

    private StringEntity criaObjetoJsonPost(String texto, String mediaId) throws UnsupportedEncodingException {

        log.info("Criando objeto JSON");

        JsonObject jsonBody = new JsonObject();
        jsonBody.add("text", new JsonPrimitive(texto));

        JsonObject mediaObject = new JsonObject();
        JsonArray mediaIdsArray = new JsonArray();
        mediaIdsArray.add(new JsonPrimitive(mediaId));
        mediaObject.add("media_ids", mediaIdsArray);

        jsonBody.add("media", mediaObject);

        log.info("Objeto JSON criado com sucesso: " + jsonBody);

        return new StringEntity(new Gson().toJson(jsonBody));

    }

}


