package com.drychan.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@Log4j2
public class PhotoUtils {

    public static InputStream streamFromBestPhotoUrl(String bestPhotoUrl) throws IOException {
        BufferedImage img = ImageIO.read(new URL(bestPhotoUrl));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        return new ByteArrayInputStream(os.toByteArray());
    }
    public static HttpEntity uploadPhotoByUrl(String uploadUrl, InputStream photoStream) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(uploadUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);

            //todo: rewrite with streams
            File tmpFile = File.createTempFile("avatar", ".jpg");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                IOUtils.copy(photoStream, out);
            }

            builder.addBinaryBody(
                    "file",
                    new FileInputStream(tmpFile),
                    ContentType.APPLICATION_OCTET_STREAM,
                    tmpFile.getName()
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);

            if (!tmpFile.delete()) {
                log.warn("can't delete tmpfile {}", tmpFile.toString());
            }
            return response.getEntity();
        } catch (IOException ex) {
            log.warn("No response from url {}", uploadUrl);
            return null;
        }
    }
}
