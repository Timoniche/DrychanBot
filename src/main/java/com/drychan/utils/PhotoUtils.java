package com.drychan.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;

public class PhotoUtils {

    private static final String PHOTO_2560 = "photo_2560";
    private static final String PHOTO_1280 = "photo_1280";
    private static final String PHOTO_604 = "photo_604";

    public static String getBestLinkToLoadFrom(JsonObject photoObject) {
        String bestQualityPhotoUrl = "";
        if (photoObject.get(PHOTO_2560) != null) {
            bestQualityPhotoUrl = photoObject.get(PHOTO_2560).getAsString();
        } else if (photoObject.get(PHOTO_1280) != null) {
            bestQualityPhotoUrl = photoObject.get(PHOTO_1280).getAsString();
        } else if (photoObject.get(PHOTO_604) != null) {
            bestQualityPhotoUrl = photoObject.get(PHOTO_604).getAsString();
        }
        return bestQualityPhotoUrl;
    }

    public static InputStream streamFromPhotoUrl(String photoUrl) throws IOException {
        BufferedImage img = ImageIO.read(new URL(photoUrl));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        return new ByteArrayInputStream(os.toByteArray());
    }
}
