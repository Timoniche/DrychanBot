package com.drychan.configuration;

import com.google.gson.GsonBuilder;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class VkConfiguration {

    private static final int RETRY_ATTEMPTS = 5;

    @Bean
    public GroupActor groupActor(
            @Value("${vk.token}") String token,
            @Value("${group.id}") String groupIdAsString
    ) {
        try {
            int groupId = Integer.parseInt(groupIdAsString);
            return new GroupActor(groupId, token);
        } catch (NumberFormatException ex) {
            log.error("groupId {} must be int", groupIdAsString);
            throw new AssertionError();
        }
    }

    @Bean
    public VkApiClient vkApiClient() {
        HttpTransportClient client = new HttpTransportClient();
        return new VkApiClient(client, (new GsonBuilder()).disableHtmlEscaping().create(), RETRY_ATTEMPTS);
    }
}
