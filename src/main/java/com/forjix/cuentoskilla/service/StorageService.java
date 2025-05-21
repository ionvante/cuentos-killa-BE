package com.forjix.cuentoskilla.service;

import org.springframework.web.multipart.MultipartFile;
import com.forjix.cuentoskilla.model.Voucher; // Import Voucher

public interface StorageService {
    Voucher store(MultipartFile file, Long orderId, String originalFileName, String contentType, String ip, String dispositivo, long fileSize);
    // Add other methods like load, delete if needed in future
}
