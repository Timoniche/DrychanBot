package com.drychan.client;

import java.net.URI;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import com.drychan.model.Keyboard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.actions.Photos;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.actions.Messages;
import com.vk.api.sdk.objects.base.Sex;
import com.vk.api.sdk.objects.groups.responses.GetMembersResponse;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.queries.docs.DocsGetMessagesUploadServerQuery;
import com.vk.api.sdk.queries.docs.DocsSaveQuery;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import static com.vk.api.sdk.objects.users.Fields.BDATE;
import static com.vk.api.sdk.objects.users.Fields.FIRST_NAME_NOM;
import static com.vk.api.sdk.objects.users.Fields.PHOTO_MAX_ORIG;
import static com.vk.api.sdk.objects.users.Fields.SEX;

/**
 * VkApiClient doesn't support new features like keyboard, so wrapper is implemented
 */
@Log4j2
@Component
public class VkApiClientWrapper {
    private static final String KEYBOARD_PARAM = "keyboard";

    private final VkApiClient vkApiClient;

    public VkApiClientWrapper(VkApiClient vkApiClient) {
        this.vkApiClient = vkApiClient;
    }

    public Messages messages() {
        return vkApiClient.messages();
    }

    public Photos photos() {
        return vkApiClient.photos();
    }

    public DocsGetMessagesUploadServerQuery docsMessagesUploadServer(GroupActor groupActor) {
        return vkApiClient.docs().getMessagesUploadServer(groupActor);
    }

    public DocsSaveQuery saveDocFile(GroupActor groupActor, String file) {
        return vkApiClient.docs().save(groupActor, file);
    }

    public String getUserVkName(GroupActor groupActor, String userId) {
        try {
            List<GetResponse> firstNameResponses = vkApiClient.users().get(groupActor)
                    .userIds(userId)
                    .fields(FIRST_NAME_NOM)
                    .execute();
            GetResponse firstNameResponse = firstNameResponses.get(0);
            return firstNameResponse.getFirstName();
        } catch (ApiException | ClientException e) {
            log.warn("Can't get user's name, userId={}", userId);
            return null;
        }
    }

    public Sex getUserVkSex(GroupActor groupActor, String userId) {
        try {
            List<GetResponse> sexResponses = vkApiClient.users().get(groupActor)
                    .userIds(userId)
                    .fields(SEX)
                    .execute();
            GetResponse sexResponse = sexResponses.get(0);
            return sexResponse.getSex();
        } catch (ApiException | ClientException e) {
            log.warn("Can't get user's name, userId={}", userId);
            return null;
        }
    }

    public Integer getUserVkAge(GroupActor groupActor, String userId) {
        try {
            List<GetResponse> birthDateResponses = vkApiClient.users().get(groupActor)
                    .userIds(userId)
                    .fields(BDATE)
                    .execute();
            GetResponse birthDateResponse = birthDateResponses.get(0);
            String birthDate = birthDateResponse.getBdate();
            if (birthDate == null) {
                log.info("Can't obtain vk bdate");
                return null;
            }
            String[] dayMonthMaybeYear = birthDate.split("\\.");
            if (dayMonthMaybeYear.length < 3) {
                log.info("User's age is hidden, userId={}", userId);
                return null;
            }
            int day = Integer.parseInt(dayMonthMaybeYear[0]);
            int month = Integer.parseInt(dayMonthMaybeYear[1]);
            int year = Integer.parseInt(dayMonthMaybeYear[2]);
            return calculateAge(day, month, year);
        } catch (ApiException | ClientException e) {
            log.warn("Can't get user's age, userId={}", userId);
            return null;
        }
    }

    public URI getUserVkPhotoUri(GroupActor groupActor, String userId) {
        try {
            List<GetResponse> userAvaResponses = vkApiClient.users().get(groupActor)
                    .userIds(userId)
                    .fields(PHOTO_MAX_ORIG)
                    .execute();
            GetResponse userAvaResponse = userAvaResponses.get(0);
            return userAvaResponse.getPhotoMaxOrig();
        } catch (ApiException | ClientException e) {
            log.warn("Can't get user's ava url, userId={}", userId);
            return null;
        }
    }

    @SuppressWarnings("unused")
    public List<Integer> getGroupMembers(GroupActor groupActor) {
        try {
            GetMembersResponse membersResponse = vkApiClient.groups()
                    .getMembers(groupActor)
                    .groupId(String.valueOf(groupActor.getGroupId()))
                    .execute();
            return membersResponse.getItems();
        } catch (ApiException | ClientException e) {
            log.warn("Can't get group's members, groupId={}", groupActor.getGroupId());
            return List.of();
        }
    }

    public static MessagesSendQuery addKeyboard(MessagesSendQuery query, Keyboard keyboard)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String keyboardJson = objectMapper.writeValueAsString(keyboard);
        return query.unsafeParam(KEYBOARD_PARAM, keyboardJson);
    }

    private int calculateAge(int day, int month, int year) {
        LocalDate birthDate = LocalDate.of(year, month, day);
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
