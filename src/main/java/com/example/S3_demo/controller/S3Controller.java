package com.example.S3_demo.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.S3_demo.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api")
public class S3Controller {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public void upload(@RequestParam("file") MultipartFile file) {
        try (OutputStream outputStream = s3Service.saveFile(file)) {
            file.getInputStream().transferTo(outputStream);
            logger.debug("File uploaded successfully: {}", file.getOriginalFilename());
        } catch (Exception e) {
            logger.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @GetMapping("/download/{filename}")
    public byte[] download(@PathVariable("filename") String filename) {
        return s3Service.downloadFile(filename);
    }

    @DeleteMapping("/delete/{filename}")
    public String deleteFile(@PathVariable("filename") String filename) {
        return s3Service.deleteFile(filename);
    }

    @GetMapping("/files")
    public List<String> getAllFiles() {
        return s3Service.listAllFiles();
    }
}
