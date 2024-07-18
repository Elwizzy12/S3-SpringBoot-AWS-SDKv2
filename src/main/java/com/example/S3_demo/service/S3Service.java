package com.example.S3_demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service implements FileServiceImpl {

    @Value("${bucketName}")
    private String bucketName;

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String saveFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        try {
            File tempFile = convertMultipartToFile(file);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(originalFilename)
                    .build();
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, tempFile.toPath());
            return putObjectResponse.eTag();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] downloadFile(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()//download file
                .bucket(bucketName)
                .key(fileName)
                .build();
        try {
            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String deleteFile(String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        return "File deleted successfully";
    }

    @Override
    public List<String> listAllFiles() {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        return listObjectsV2Response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException {
        Path tempDir = Files.createTempDirectory("");
        File tempFile = tempDir.resolve(multipartFile.getOriginalFilename()).toFile();
        Files.copy(multipartFile.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }
}
