package com.drychan.handler;

import java.util.Objects;

import com.drychan.dao.model.User;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.model.ObjectMessage;
import com.drychan.service.UserService;
import com.drychan.utils.PhotoUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public enum DraftUserProcessingStage {
    NO_NAME {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (messageText.isBlank()) {
                messageSender.send(userId, "Ты уверен, что твое имя на Whitespace?)");
                return false;
            } else {
                user.setName(messageText);
                userService.saveUser(user);
                log.info("user_id={} set name to {}", userId, messageText);
                messageSender.send(userId, "Прекрасное имя! Теперь укажи свой пол) [м/ж]");
            }
            return true;
        }
    },
    NO_GENDER {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (!messageText.equalsIgnoreCase("м") && !messageText.equalsIgnoreCase("ж")) {
                messageSender.send(userId, "Есть всего 2 гендера, м и ж, попробуй еще раз)");
                return false;
            } else {
                boolean isMale = messageText.equalsIgnoreCase("м");
                if (isMale) {
                    user.setGender('m');
                } else {
                    user.setGender('f');
                }
                userService.saveUser(user);
                log.info("user_id={} set gender to {}", userId, messageText);
                String genderDependentQuestion;
                if (isMale) {
                    genderDependentQuestion = "Сколько тебе лет, парень? Надеюсь, ты пришел не пикапить школьниц)";
                } else {
                    genderDependentQuestion = "У девушки, конечно, невежливо спрашивать возраст, но я рискну)";
                }
                messageSender.send(userId, genderDependentQuestion);
            }
            return true;
        }
    },
    NO_AGE {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils) {
            String messageText = message.getText();
            int userId = user.getUserId();
            try {
                int age = Integer.parseInt(messageText);
                user.setAge(age);
                userService.saveUser(user);
                log.info("user_id={} set age to {}", userId, age);
                messageSender.send(userId, "Придумаешь остроумное описание?");
            } catch (NumberFormatException ex) {
                messageSender.send(userId, "Столько не живут)");
                return false;
            }
            return true;
        }
    },
    NO_DESCRIPTION {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils) {
            String messageText = message.getText();
            int userId = user.getUserId();
            if (messageText.isBlank()) {
                messageSender.send(userId, "Хм, немногословно) Попробуй еще раз!");
                return false;
            } else {
                user.setDescription(messageText);
                userService.saveUser(user);
                log.info("user_id={} set description to {}", userId, messageText);
                messageSender.send(userId, "Теперь нужна красивая фото4ка!");
            }
            return true;
        }
    },
    NO_PHOTO_PATH {
        @Override
        boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                 ObjectMessage message, PhotoUtils photoUtils) {
            int userId = user.getUserId();
            var maybePhotoAttachment = message.findAnyPhotoAttachment();
            MessagePhotoAttachment photoAttachment = maybePhotoAttachment.orElse(null);
            if (photoAttachment == null) {
                messageSender.send(userId, "Не вижу твоей фотки, try one more time");
                return false;
            }
            if (photoAttachment.getAccessKey() != null) {
                photoAttachment = photoUtils.reuploadPhoto(photoAttachment);
                if (photoAttachment == null) {
                    messageSender.send(userId, "Не удалось загрузить фото, try one more time");
                    return false;
                }
            }
            user.setPhotoPath(photoAttachment.toString());
            user.setStatus(User.Status.published);
            userService.saveUser(user);
            log.info("user_id={} set photo_path to {}", userId, photoAttachment.toString());
            log.info("user_id={} is published", userId);
            messageSender.send(userId, "Ваша анкета:" +
                            NEXT_LINE +
                            NEXT_LINE + user.getName() + ", " + user.getAge() +
                            NEXT_LINE + user.getDescription(),
                    user.getPhotoPath(), null);
            return true;
        }
    };

    private static final String NEXT_LINE = System.lineSeparator();

    /**
     * @return if stage was successful
     */
    abstract boolean processUserStage(User user, MessageSender messageSender, UserService userService,
                                      ObjectMessage message, PhotoUtils photoUtils);

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
        }
        return null;
    }
}
