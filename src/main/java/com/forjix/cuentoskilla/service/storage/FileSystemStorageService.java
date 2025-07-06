package com.forjix.cuentoskilla.service.storage;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.VoucherRepository;
import com.forjix.cuentoskilla.service.StorageService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);

    @Value("${file.upload-dir:./uploads}") // Default to ./uploads if not specified in properties
    private String uploadDir;

    @Value("${upload.max-size:5242880}")
    private long maxFileSize;

    private Path rootLocation;

    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    public FileSystemStorageService(VoucherRepository voucherRepository,
                                   OrderRepository orderRepository,
                                   MeterRegistry meterRegistry) {
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            logger.info("Storage root location initialized at: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location", e);
        }
    }

    @Override
    public Voucher store(MultipartFile file, Long orderId, String originalFileName,
                         String contentType, String ip, String dispositivo, long fileSize) {
        if (file.isEmpty()) {
            meterRegistry.counter("voucher_upload_fail").increment();
            throw new StorageException("Failed to store empty file.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new StorageException("Order not found with id: " + orderId));

        // Sanitize filename and add a UUID to prevent collisions
        String filename = StringUtils.cleanPath(
                Paths.get(originalFileName != null ? originalFileName : "").getFileName().toString());
        // Ensure filename is not empty after cleaning
        if (filename.trim().isEmpty()) {
            filename = "unnamedfile"; // Provide a default name if original is empty or was just spaces
        }

        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null || !(extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")
                || extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("pdf"))) {
            meterRegistry.counter("voucher_upload_fail").increment();
            throw new StorageException("INVALID_FILE");
        }

        if (fileSize > maxFileSize) {
            meterRegistry.counter("voucher_upload_fail").increment();
            throw new StorageException("MAX_UPLOAD_SIZE_EXCEEDED");
        }
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;

        Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                .normalize().toAbsolutePath();
        logger.info("Storing voucher. rootLocation: {} destination: {}", this.rootLocation.toAbsolutePath(), destinationFile); 

        if (!destinationFile.getParent().equals(this.rootLocation)) {
            // This is a security check
            throw new StorageException("Cannot store file outside current directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            meterRegistry.counter("voucher_upload_fail").increment();
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
        voucher.setFilePath(destinationFile.toString()); // Store the full path

        Voucher saved = voucherRepository.save(voucher);

        order.setEstado(OrderStatus.PAGO_ENVIADO);
        orderRepository.save(order);

        meterRegistry.counter("voucher_upload_success").increment();

        return saved;
    }
}
