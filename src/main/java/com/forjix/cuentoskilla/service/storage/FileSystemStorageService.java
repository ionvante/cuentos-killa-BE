package com.forjix.cuentoskilla.service.storage;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.VoucherRepository;
import com.forjix.cuentoskilla.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
public class FileSystemStorageService implements StorageService {

    @Value("${file.upload-dir:./uploads}") // Default to ./uploads if not specified in properties
    private String uploadDir;

    private Path rootLocation;

    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    public FileSystemStorageService(VoucherRepository voucherRepository, OrderRepository orderRepository) {
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location", e);
        }
    }

    @Override
    public Voucher store(MultipartFile file, Long orderId, String originalFileName, String contentType, String ip, String dispositivo, long fileSize) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new StorageException("Order not found with id: " + orderId));

        // Sanitize filename and add a UUID to prevent collisions
        String filename = StringUtils.cleanPath(originalFileName != null ? originalFileName : ""); // Handle null originalFileName
        // Ensure filename is not empty after cleaning
        if (filename.trim().isEmpty()) {
            filename = "unnamedfile"; // Provide a default name if original is empty or was just spaces
        }
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
        
        Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                .normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            // This is a security check
            throw new StorageException("Cannot store file outside current directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }

        Voucher voucher = new Voucher();
        voucher.setOrder(order);
        voucher.setFecha(LocalDate.now());
        voucher.setHora(LocalTime.now());
        voucher.setPeso(String.valueOf(fileSize)); // Store file size as string
        voucher.setDispositivo(dispositivo);
        voucher.setIp(ip);
        voucher.setNombreArchivo(uniqueFilename); // Store the unique filename
        voucher.setTipoArchivo(contentType);
        voucher.setFilePath(destinationFile.toString()); // Add this line to store the full path

        return voucherRepository.save(voucher);
    }
}
