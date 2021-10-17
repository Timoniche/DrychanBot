package com.drychan.handler;

import java.io.InputStream;
import java.util.Objects;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.dao.model.User;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.model.ObjectMessage;
import com.drychan.model.voice.MessageVoiceAttachment;
import com.drychan.service.UserService;
import com.drychan.utils.AudioUtils;
import com.drychan.utils.PhotoUtils;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.base.Sex;
import lombok.extern.log4j.Log4j2;

import static com.drychan.dao.model.User.Gender;
import static com.drychan.dao.model.User.Gender.FEMALE;
import static com.drychan.dao.model.User.Gender.MALE;
import static com.drychan.dao.model.User.Gender.genderFromVkSex;
import static com.drychan.dao.model.User.Status.DRAFT;
import static com.drychan.dao.model.User.Status.PUBLISHED;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.HELP;
import static com.drychan.handler.DraftUserProcessor.DraftStage.getStageFromUser;
import static com.drychan.handler.MessageHandler.NEXT_LINE;
import static com.drychan.model.ButtonColor.PRIMARY;
import static com.drychan.model.ButtonColor.SECONDARY;
import static com.drychan.model.Keyboard.APPROVE_LABEL;
import static com.drychan.model.Keyboard.FEMALE_LABEL;
import static com.drychan.model.Keyboard.MALE_LABEL;
import static com.drychan.model.Keyboard.NOT_AGAIN;
import static com.drychan.model.Keyboard.YEEES;
import static com.drychan.model.Keyboard.approveHelpKeyboard;
import static com.drychan.model.Keyboard.buttonOf;
import static com.drychan.model.Keyboard.genderKeyboard;
import static com.drychan.model.Keyboard.keyboardFromButton;
import static com.drychan.model.Keyboard.yesOrNotAgainKeyboard;

@Log4j2
public class DraftUserProcessor {
    private final MessageSender messageSender;
    private final UserService userService;
    private final PhotoUtils photoUtils;
    private final AudioUtils audioUtils;
    private final VkApiClientWrapper apiClient;
    private final GroupActor groupActor;

    public static final String NO_VOICE_PATH = "no";
    private static final String VERDICT_ADVICE = "В нашей ситуации писать анекдот на две страницы без ее лайка - " +
            "это бросать снасти и нырять в реку самому. " +
            "Она только начала подплывать к точке прикормки, а ты уже кидаешься. " +
            "Так не рыбачат, дружище! На данном этапе желание выцепить на встречу одну конкретную даму - глупость. ";
    private static final String VERDICT_ADVICE_FEMALE = "Чередуй флирт, шутки и реплики по теме беседы. " +
            "Человека увлекает непредсказуемое. Заставь ждать следующего сообщения как новой фигурки из киндера. " +
            "Сразу не получится, но наша задача - научиться.";
    private static final String VERDICT_SIGNATURE = "(с) Максим Вердикт";

    private volatile MessagePhotoAttachment maleAdvicePhoto;
    private volatile MessagePhotoAttachment femaleAdvicePhoto;

    public DraftUserProcessor(MessageSender messageSender, UserService userService, PhotoUtils photoUtils,
                              AudioUtils audioUtils, VkApiClientWrapper apiClient, GroupActor groupActor) {
        this.messageSender = messageSender;
        this.userService = userService;
        this.photoUtils = photoUtils;
        this.audioUtils = audioUtils;
        this.apiClient = apiClient;
        this.groupActor = groupActor;
    }

    /**
     * @return if stage was successful
     */
    public boolean processUserStage(User user, ObjectMessage message) {
        DraftStage stage = getStageFromUser(user);
        switch (Objects.requireNonNull(stage)) {
            case NO_NAME:
                return processNoName(user, message);
            case NO_GENDER:
                return processNoGender(user, message);
            case NO_AGE:
                return processNoAge(user, message);
            case NO_DESCRIPTION:
                return processNoDescription(user, message);
            case NO_PHOTO_PATH:
                return processNoPhotoPath(user, message);
            case NO_VOICE_ATTACHMENT:
                return processNoVoiceAttachment(user, message);
            case WAITING_APPROVE:
                return waitingApprove(user, message);
            default:
                throw new IllegalArgumentException("Unsupported draft user stage: " + stage);
        }
    }

    public enum DraftStage {
        NO_NAME,
        NO_GENDER,
        NO_AGE,
        NO_DESCRIPTION,
        NO_PHOTO_PATH,
        NO_VOICE_ATTACHMENT,
        WAITING_APPROVE;

        /**
         * @return null if user is not draft
         */
        public static DraftStage getStageFromUser(User user) {
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
    }

    public void askQuestionForNextStage(User user) {
        int userId = user.getUserId();
        DraftStage nextStage = getStageFromUser(user);
        switch (Objects.requireNonNull(nextStage)) {
            case NO_NAME:
                sendNameQuestion(userId);
                break;
            case NO_GENDER:
                sendGenderQuestion(userId);
                break;
            case NO_AGE:
                sendAgeQuestion(user.isMale(), userId);
                break;
            case NO_DESCRIPTION:
                sendDescriptionQuestion(userId);
                break;
            case NO_PHOTO_PATH:
                sendPhotoQuestion(userId);
                break;
            case NO_VOICE_ATTACHMENT:
                sendVoiceQuestion(userId);
                break;
            case WAITING_APPROVE:
                showProfile(user, messageSender);
                break;
            default:
                throw new IllegalArgumentException("Unsupported draft user stage: " + nextStage);
        }
    }

    public boolean processNoName(User user, ObjectMessage message) {
        String userName = message.getText();
        int userId = user.getUserId();
        if (userName.isBlank()) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Ты уверен, что твое имя на Whitespace?)")
                    .build());
            return false;
        } else {
            user.setName(userName);
            userService.saveUser(user);
            log.info("user_id={} set name to {}", userId, userName);
            Sex vkSex = apiClient.getUserVkSex(groupActor, String.valueOf(userId));
            if (user.getGender() == null && vkSex != Sex.UNKNOWN) {
                Gender gender = genderFromVkSex(vkSex);
                user.setGender(gender);
                userService.saveUser(user);
                log.info("user_id={} set gender to '{}'", userId, gender);
                sendGenderPickupAdvice(user.isMale(), userId, userName);
            }
            askQuestionForNextStage(user);
        }
        return true;
    }

    public boolean processNoGender(User user, ObjectMessage message) {
        String messageText = message.getText();
        int userId = user.getUserId();
        if (!messageText.equals(MALE_LABEL) && !messageText.equals(FEMALE_LABEL)) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Есть всего 2 гендера: " + MALE_LABEL + " и " + FEMALE_LABEL +
                            ", попробуй еще раз)")
                    .build());
            return false;
        } else {
            boolean isMale = messageText.equals(MALE_LABEL);
            if (isMale) {
                user.setGender(MALE);
            } else {
                user.setGender(FEMALE);
            }
            userService.saveUser(user);
            log.info("user_id={} set gender to {}", userId, messageText);
            sendGenderPickupAdvice(user.isMale(), userId, user.getName());
            askQuestionForNextStage(user);
        }
        return true;
    }

    public boolean processNoAge(User user, ObjectMessage message) {
        String messageText = message.getText();
        int userId = user.getUserId();
        try {
            int age = Integer.parseInt(messageText);
            user.setAge(age);
            userService.saveUser(user);
            log.info("user_id={} set age to {}", userId, age);
            askQuestionForNextStage(user);
        } catch (NumberFormatException ex) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Столько не живут)")
                    .build());
            return false;
        }
        return true;
    }

    public boolean processNoDescription(User user, ObjectMessage message) {
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
            askQuestionForNextStage(user);
        }
        return true;
    }

    public boolean processNoPhotoPath(User user, ObjectMessage message) {
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
        askQuestionForNextStage(user);
        return true;
    }

    public boolean processNoVoiceAttachment(User user, ObjectMessage message) {
        int userId = user.getUserId();
        var maybeAudioAttachment = message.findAudioAttachment();
        MessageVoiceAttachment audioAttachment = maybeAudioAttachment.orElse(null);
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
        MessageVoiceAttachment reuploadedVoice = audioUtils.reuploadAudio(audioAttachment);
        user.setVoicePath(reuploadedVoice.getAttachmentPath());
        userService.saveUser(user);
        log.info("user_id={} set voice path to {}", userId, reuploadedVoice.getAttachmentPath());
        askQuestionForNextStage(user);
        return true;
    }

    public boolean waitingApprove(User user, ObjectMessage message) {
        String messageText = message.getText();
        if (messageText.equals(APPROVE_LABEL)) {
            publishUser(user, userService);
            return true;
        }
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(user.getUserId())
                .message("Подтверди анкету, нажав на " + APPROVE_LABEL + ", или набери " + HELP.getCommand()
                        + " для выдачи списка команд")
                .keyboard(approveHelpKeyboard(true))
                .build());
        return false;
    }

    public void sendNameQuestion(int userId) {
        String vkFirstName = apiClient.getUserVkName(groupActor, String.valueOf(userId));
        var messageBuilder = MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Как тебя зовут?)");
        if (vkFirstName != null) {
            messageBuilder.keyboard(keyboardFromButton(buttonOf(PRIMARY, vkFirstName), true));
        }
        messageSender.send(messageBuilder.build());
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
                .keyboard(approveHelpKeyboard(true))
                .build());
    }

    private static void publishUser(User user, UserService userService) {
        int userId = user.getUserId();
        user.setStatus(PUBLISHED);
        userService.saveUser(user);
        log.info("user_id={} is published", userId);
    }

    public void sendGenderQuestion(int userId) {
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Прекрасное имя! Теперь укажи свой пол)")
                .keyboard(genderKeyboard(true))
                .build());
    }

    public void sendAgeQuestion(boolean isMale, int userId) {
        String genderDependentQuestion;
        if (isMale) {
            genderDependentQuestion =
                    "Сколько тебе лет? Надеюсь, ты пришел не пикапить школьниц\uD83D\uDD1E)";
        } else {
            genderDependentQuestion = "У девушки, конечно, невежливо спрашивать возраст, но я рискну)";
        }
        var messageBuilder = MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message(genderDependentQuestion);
        Integer vkAge = apiClient.getUserVkAge(groupActor, String.valueOf(userId));
        if (vkAge != null) {
            messageBuilder.keyboard(keyboardFromButton(buttonOf(SECONDARY, String.valueOf(vkAge)), true));
        }
        messageSender.send(messageBuilder.build());
    }

    public void sendDescriptionQuestion(int userId) {
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Придумаешь остроумное описание?")
                .build());
    }

    public void sendPhotoQuestion(int userId) {
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Теперь нужна красивая фото4ка!")
                .build());
    }

    public void sendVoiceQuestion(int userId) {
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Хочешь записать голосовое сообщение? " +
                        NEXT_LINE +
                        "Например, можешь спеть)")
                .keyboard(yesOrNotAgainKeyboard(true))
                .build());
    }

    private void sendGenderPickupAdvice(boolean isMale, int userId, String name) {
        MessageSender.MessageSendQuery.MessageSendQueryBuilder messageBuilder;

        if (isMale) {
            messageBuilder = MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Псс, парень!" + NEXT_LINE
                            + NEXT_LINE
                            + VERDICT_ADVICE
                            + NEXT_LINE
                            + VERDICT_SIGNATURE);
            if (getMaleAdvicePhoto() != null) {
                messageBuilder.photoAttachmentPath(getMaleAdvicePhoto().getAttachmentPath());
            }
        } else {
            messageBuilder = MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Добро пожаловать, " + name + "!" + NEXT_LINE
                            + NEXT_LINE
                            + VERDICT_ADVICE_FEMALE
                            + NEXT_LINE
                            + VERDICT_SIGNATURE);
            if (getFemaleAdvicePhoto() != null) {
                messageBuilder.photoAttachmentPath(getFemaleAdvicePhoto().getAttachmentPath());
            }
        }

        messageSender.send(messageBuilder.build());
    }

    //todo: refactor to lazy photo loader
    private MessagePhotoAttachment getMaleAdvicePhoto() {
        String malePhoto = "deerForMale.jpg";
        MessagePhotoAttachment result = maleAdvicePhoto;
        if (result == null) {
            maleAdvicePhoto = result = photoUtils.reuploadPhoto(getBufferedReaderFromResourceFile(malePhoto));
        }
        return result;
    }

    private MessagePhotoAttachment getFemaleAdvicePhoto() {
        String femalePhoto = "deerForFemale.jpg";
        MessagePhotoAttachment result = femaleAdvicePhoto;
        if (result == null) {
            femaleAdvicePhoto = result = photoUtils.reuploadPhoto(getBufferedReaderFromResourceFile(femalePhoto));
        }
        return result;
    }

    private InputStream getBufferedReaderFromResourceFile(String fileName) {
        return getClass().getResourceAsStream("/photos/" + fileName);
    }
}
