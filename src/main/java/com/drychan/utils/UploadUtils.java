package com.drychan.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class UploadUtils {
    public static HttpEntity uploadFileByUrl(URI uploadUrl, File file) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
                "file",
                new FileInputStream(file),
                ContentType.APPLICATION_OCTET_STREAM,
                file.getName()
        );
        HttpEntity multipart = builder.build();
        HttpPost uploadFile = new HttpPost(uploadUrl);
        uploadFile.setEntity(multipart);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(uploadFile);
        return response.getEntity();
    }
}
