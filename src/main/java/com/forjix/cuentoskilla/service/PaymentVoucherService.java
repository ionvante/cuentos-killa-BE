package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.PaymentVoucher;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.PaymentVoucherRepository;
import com.forjix.cuentoskilla.service.storage.FirebaseStorageService;
import com.forjix.cuentoskilla.service.storage.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentVoucherService {

    private final PaymentVoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final FirebaseStorageService storageService;

    @Value("${upload.max-size:5242880}")
    private long maxSize;

    public PaymentVoucherService(PaymentVoucherRepository voucherRepository,
                                 OrderRepository orderRepository,
                                 FirebaseStorageService storageService) {
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
        this.storageService = storageService;
    }

    public PaymentVoucher upload(Long orderId, MultipartFile file) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new StorageException("Order not found"));

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null) throw new StorageException("INVALID_FILE");
        String mime = file.getContentType();
        if (!("image/png".equals(mime) || "image/jpeg".equals(mime) || "application/pdf".equals(mime))) {
            throw new StorageException("INVALID_FILE");
        }
        if (file.getSize() > maxSize) {
            throw new StorageException("MAX_UPLOAD_SIZE_EXCEEDED");
        }

        String filename = System.currentTimeMillis() + "." + ext;
        String path = "vouchers/" + orderId + "/" + filename;
        storageService.upload(file, path);

        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setOrder(order);
        voucher.setFilename(filename);
        voucher.setMimeType(mime);
        voucher.setSize(file.getSize());
        voucher.setFirebasePath(path);
        voucher.setUploadDate(LocalDateTime.now());

        PaymentVoucher saved = voucherRepository.save(voucher);

        order.setEstado(OrderStatus.PAGO_ENVIADO);
        orderRepository.save(order);

        return saved;
    }

    public Optional<PaymentVoucher> findByOrder(Long orderId) {
        return voucherRepository.findByOrder_Id(orderId);
    }

    public String generateSignedUrl(PaymentVoucher voucher) {
        return storageService.generateSignedUrl(voucher.getFirebasePath());
    }

    public void delete(PaymentVoucher voucher) {
        storageService.delete(voucher.getFirebasePath());
        voucherRepository.delete(voucher);
    }
}
