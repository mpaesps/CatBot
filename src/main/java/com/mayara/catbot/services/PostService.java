package com.mayara.catbot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayara.catbot.entities.FactsResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class PostService {

    @Autowired
    private ObjectMapper mapper;

    //@Scheduled(cron = "0 0 12,18 * * ?")
   @Scheduled(cron = "0 * * * * ?")
    public void realizaPostNoTwitter() throws IOException {

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        HttpGet httpGet = new HttpGet("https://meowfacts.herokuapp.com/");

        HttpResponse response = httpClient.execute(httpGet);

        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity);

        FactsResponse factsResponse = mapper.readValue(responseBody, FactsResponse.class);
        System.out.println(factsResponse.getData().get(0));
    }
}
