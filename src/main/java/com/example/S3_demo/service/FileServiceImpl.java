package com.example.S3_demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

public interface FileServiceImpl {
    OutputStream saveFile(MultipartFile file);

    byte[] downloadFile(String fileName);

    String deleteFile(String fileName);

    List<String> listAllFiles();
}
