package com.example.S3_demo.controller;

import com.example.S3_demo.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        return s3Service.saveFile(file);
    }

    @GetMapping("/download/{filename}")
    public byte[] download(@PathVariable("filename") String filename) {
        return s3Service.downloadFile(filename);
    }

    @DeleteMapping("/delete/{filename}") // Corrected endpoint path
    public String deleteFile(@PathVariable("filename") String filename) {
        return s3Service.deleteFile(filename);
    }

    @GetMapping("/files") // Corrected endpoint path
    public List<String> getAllFiles() {
        return s3Service.listAllFiles();
    }
}
