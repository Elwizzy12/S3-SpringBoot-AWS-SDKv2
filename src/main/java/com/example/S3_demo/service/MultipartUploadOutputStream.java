package com.example.S3_demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MultipartUploadOutputStream extends OutputStream {

    private static final Logger logger = LoggerFactory.getLogger(MultipartUploadOutputStream.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String key;
    private final long partSize;
    private final List<CompletedPart> completedParts;
    private ByteArrayOutputStream buffer;
    private final String uploadId;
    private int partNumber;

    public MultipartUploadOutputStream(S3Client s3Client, String bucketName, String key, String uploadId, long partSize) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.key = key;
        this.uploadId = uploadId;
        this.partSize = partSize;
        this.completedParts = new ArrayList<>();
        this.buffer = new ByteArrayOutputStream();
        this.partNumber = 1;
        logger.debug("MultipartUploadOutputStream initialized for bucket {}, key {}, uploadId {}, partSize {}",
                bucketName, key, uploadId, partSize);
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
        if (buffer.size() >= partSize) {
            uploadPart();
        }
    }

    @Override
    public void flush() throws IOException {
        if (buffer.size() > 0) {
            uploadPart();
        }
        super.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            if (buffer.size() > 0) {
                uploadPart();
            }
        } catch (IOException e) {
            logger.error("Error during upload part", e);
            throw new IOException("Failed during upload part", e);
        } finally {
            completeMultipartUpload();
            super.close();
        }
    }

    private void uploadPart() throws IOException {
        byte[] partData = buffer.toByteArray();
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber++)
                .build();
        try {
            UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partData));
            completedParts.add(CompletedPart.builder()
                    .partNumber(partNumber - 1)
                    .eTag(uploadPartResponse.eTag())
                    .build());
            buffer.reset();
            logger.debug("Uploaded part {} with ETag {}", partNumber - 1, uploadPartResponse.eTag());
        } catch (S3Exception e) {
            logger.error("Failed to upload part {}", partNumber - 1, e);
            throw new IOException("Failed to upload part", e);
        }
    }

    private void completeMultipartUpload() {
        try {
            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build();
            s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            logger.debug("Multipart upload completed for key {}", key);
        } catch (S3Exception e) {
            logger.error("Failed to complete multipart upload for key {}", key, e);
            abortMultipartUpload();
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }

    private void abortMultipartUpload() {
        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .build();
            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
            logger.debug("Multipart upload aborted for key {}", key);
        } catch (S3Exception e) {
            logger.error("Failed to abort multipart upload for key {}", key, e);
            throw new RuntimeException("Failed to abort multipart upload", e);
        }
    }
}
