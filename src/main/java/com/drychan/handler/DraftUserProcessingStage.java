package com.drychan.handler;

import java.util.Objects;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.dao.model.User;
import com.drychan.model.Button;
import com.drychan.model.ButtonAction;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.model.ObjectMessage;
import com.drychan.model.audio.MessageAudioAttachment;
import com.drychan.service.UserService;
import com.drychan.utils.AudioUtils;
import com.drychan.utils.PhotoUtils;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.base.Sex;
import lombok.extern.log4j.Log4j2;

import static com.drychan.dao.model.User.Status.DRAFT;
import static com.drychan.dao.model.User.Status.PUBLISHED;
import static com.drychan.handler.DefaultCommands.HELP;
import static com.drychan.model.ButtonColor.SECONDARY;
import static com.drychan.model.Keyboard.APPROVE;
import static com.drychan.model.Keyboard.FEMALE;
import static com.drychan.model.Keyboard.MALE;
import static com.drychan.model.Keyboard.NOT_AGAIN;
import static com.drychan.model.Keyboard.TEXT_BUTTON_TYPE;
import static com.drychan.model.Keyboard.YEEES;
import static com.drychan.model.Keyboard.approveHelpKeyboard;
import static com.drychan.model.Keyboard.approveKeyboard;
import static com.drychan.model.Keyboard.genderKeyboard;
import static com.drychan.model.Keyboard.keyboardFromButton;
import static com.drychan.model.Keyboard.yesOrNotAgainKeyboard;

@Log4j2
//todo: refactor with switch + make methods "question before ..."
public enum DraftUserProcessingStage {
    NO_NAME {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (messageText.isBlank()) {
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Ты уверен, что твое имя на Whitespace?)")
                        .build());
                return false;
            } else {
                user.setName(messageText);
                userService.saveUser(user);
                log.info("user_id={} set name to {}", userId, messageText);
                Sex vkSex = apiClient.getUserVkSex(actor, String.valueOf(userId));
                if (vkSex == Sex.UNKNOWN) {
                    messageSender.send(MessageSender.MessageSendQuery.builder()
                            .userId(userId)
                            .message("Прекрасное имя! Теперь укажи свой пол)")
                            .keyboard(genderKeyboard(true))
                            .build());
                } else {
                    boolean isMale = vkSex == Sex.MALE;
                    Character gender = isMale ? 'm' : 'f';
                    user.setGender(gender);
                    userService.saveUser(user);
                    log.info("user_id={} set gender to '{}'", userId, gender);
                    ageQuestion(isMale, userId, messageSender, apiClient, actor);
                }
            }
            return true;
        }
    },
    NO_GENDER {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (!messageText.equals(MALE) && !messageText.equals(FEMALE)) {
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Есть всего 2 гендера: " + MALE + " и " + FEMALE +
                                ", попробуй еще раз)")
                        .build());
                return false;
            } else {
                boolean isMale = messageText.equals(MALE);
                if (isMale) {
                    user.setGender('m');
                } else {
                    user.setGender('f');
                }
                userService.saveUser(user);
                log.info("user_id={} set gender to {}", userId, messageText);
                ageQuestion(isMale, userId, messageSender, apiClient, actor);
            }
            return true;
        }
    },
    NO_AGE {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            String messageText = message.getText();
            int userId = user.getUserId();
            try {
                int age = Integer.parseInt(messageText);
                user.setAge(age);
                userService.saveUser(user);
                log.info("user_id={} set age to {}", userId, age);
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Придумаешь остроумное описание?")
                        .build());
            } catch (NumberFormatException ex) {
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Столько не живут)")
                        .build());
                return false;
            }
            return true;
        }
    },
    NO_DESCRIPTION {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (messageText.isBlank()) {
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Хм, немногословно) Попробуй еще раз!")
                        .build());
                return false;
            } else {
                user.setDescription(messageText);
                userService.saveUser(user);
                log.info("user_id={} set description to {}", userId, messageText);
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Теперь нужна красивая фото4ка!")
                        .build());
            }
            return true;
        }
    },
    NO_PHOTO_PATH {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            int userId = user.getUserId();
            var maybePhotoAttachment = message.findAnyPhotoAttachment();
            MessagePhotoAttachment photoAttachment = maybePhotoAttachment.orElse(null);
            if (photoAttachment == null) {
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Не вижу твоей фотки, try one more time")
                        .build());
                return false;
            }
            if (photoAttachment.getAccessKey() != null) {
                photoAttachment = photoUtils.reuploadPhoto(photoAttachment);
                if (photoAttachment == null) {
                    messageSender.send(MessageSender.MessageSendQuery.builder()
                            .userId(userId)
                            .message("Не удалось загрузить фото, try one more time")
                            .build());
                    return false;
                }
            }
            user.setPhotoPath(photoAttachment.getAttachmentPath());
            userService.saveUser(user);
            log.info("user_id={} set photo_path to {}", userId, photoAttachment.getAttachmentPath());
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Хочешь записать голосовое сообщение? " +
                            NEXT_LINE +
                            "Например, можешь спеть)")
                    .keyboard(yesOrNotAgainKeyboard(true))
                    .build());
            return true;
        }
    },
    NO_VOICE_ATTACHMENT {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            int userId = user.getUserId();
            var maybeAudioAttachment = message.findAudioAttachment();
            MessageAudioAttachment audioAttachment = maybeAudioAttachment.orElse(null);
            if (audioAttachment == null) {
                String messageText = message.getText();
                if (messageText.equals(YEEES)) {
                    messageSender.send(MessageSender.MessageSendQuery.builder()
                            .userId(userId)
                            .message("Жду твое голосовое сообщение!")
                            .build());
                    return false;
                }
                if (messageText.equals(NOT_AGAIN)) {
                    user.setVoicePath(NO_VOICE_PATH);
                    userService.saveUser(user);
                    log.info("user_id={} set NO voice path", userId);
                    showProfile(user, messageSender);
                    return true;
                }
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Не удалось загрузить аудиосообщение, попробуй еще раз)")
                        .build());
                return false;
            }
            MessageAudioAttachment reuploadedVoice = audioUtils.reuploadAudio(audioAttachment);
            user.setVoicePath(reuploadedVoice.getAttachmentPath());
            userService.saveUser(user);
            log.info("user_id={} set voice path to {}", userId, reuploadedVoice.getAttachmentPath());
            showProfile(user, messageSender);
            return true;

        }
    },
    WAITING_APPROVE {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                 VkApiClientWrapper apiClient, GroupActor actor) {
            String messageText = message.getText();
            if (messageText.equals(APPROVE)) {
                publishUser(user, userService);
                return true;
            }
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(user.getUserId())
                    .message("Подтверди анкету, нажав на " + APPROVE + ", или набери " + HELP.getCommand()
                            + " для выдачи списка команд")
                    .keyboard(approveHelpKeyboard(true))
                    .build());
            return false;
        }
    };

    private static final String NEXT_LINE = System.lineSeparator();
    public static final String NO_VOICE_PATH = "no";

    /**
     * @return if stage was successful
     */
    abstract boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                      ObjectMessage message, PhotoUtils photoUtils, AudioUtils audioUtils,
                                      VkApiClientWrapper apiClient, GroupActor actor);

    /**
     * @return null if user is not draft
     */
    public static DraftUserProcessingStage getStageFromUser(User user) {
        Objects.requireNonNull(user);
        if (user.getName() == null) {
            return NO_NAME;
        } else if (user.getGender() == null) {
            return NO_GENDER;
        } else if (user.getAge() == null) {
            return NO_AGE;
        } else if (user.getDescription() == null) {
            return NO_DESCRIPTION;
        } else if (user.getPhotoPath() == null) {
            return NO_PHOTO_PATH;
        } else if (user.getVoicePath() == null) {
            return NO_VOICE_ATTACHMENT;
        } else if (user.getStatus() == DRAFT) {
            return WAITING_APPROVE;
        }
        return null;
    }

    private static void showProfile(User user, MessageSender messageSender) {
        int userId = user.getUserId();
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Ваша анкета:" +
                        NEXT_LINE +
                        NEXT_LINE + user.getName() + ", " + user.getAge() +
                        NEXT_LINE + user.getDescription())
                .photoAttachmentPath(user.getPhotoPath())
                .voicePath(user.getVoicePath())
                .keyboard(approveKeyboard(true))
                .build());
    }

    private static void publishUser(User user, UserService userService) {
        int userId = user.getUserId();
        user.setStatus(PUBLISHED);
        userService.saveUser(user);
        log.info("user_id={} is published", userId);
    }

    private static void ageQuestion(boolean isMale, int userId, MessageSender messageSender, VkApiClientWrapper apiClient,
                             GroupActor actor) {
        String genderDependentQuestion;
        if (isMale) {
            genderDependentQuestion =
                    "Сколько тебе лет, парень? Надеюсь, ты пришел не пикапить школьниц\uD83D\uDD1E)";
        } else {
            genderDependentQuestion = "У девушки, конечно, невежливо спрашивать возраст, но я рискну)";
        }
        var messageBuilder = MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message(genderDependentQuestion);
        Integer vkAge = apiClient.getUserVkAge(actor, String.valueOf(userId));
        if (vkAge != null) {
            messageBuilder.keyboard(keyboardFromButton(new Button(SECONDARY.getColor(), ButtonAction.builder()
                    .type(TEXT_BUTTON_TYPE)
                    .label(String.valueOf(vkAge))
                    .build()), true));
        }
        messageSender.send(messageBuilder.build());
    }
}
