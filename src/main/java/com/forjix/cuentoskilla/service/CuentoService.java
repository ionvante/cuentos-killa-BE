package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CuentoService {
    private final CuentoRepository repo;
    private final com.forjix.cuentoskilla.service.storage.FirebaseStorageService firebaseStorageService;

    public CuentoService(CuentoRepository repo, com.forjix.cuentoskilla.service.storage.FirebaseStorageService firebaseStorageService) {
        this.repo = repo;
        this.firebaseStorageService = firebaseStorageService;
    }

    public List<Cuento> findAll() {
        return repo.findAll();
    }

    public Optional<Cuento> findById(Long id) {
        return repo.findById(id);
    }

    public Cuento save(Cuento cuento, org.springframework.web.multipart.MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            String fileName = "cuentos/" + java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            firebaseStorageService.upload(file, fileName);
            String fileUrl = firebaseStorageService.generateSignedUrl(fileName); // Consider storing just the path if you want to generate URLs on the fly, but for MVP saving URL is easier
            // Remove token from generated URL to store the public path if bucket is public, or store signed if short-lived.
            // Using a simple split here assuming standard Firebase URL format or keeping it as is.
            cuento.setImagenUrl(fileUrl);
        }
        return repo.save(cuento);
    }

    public Optional<Cuento> update(Long id, Cuento cuento, org.springframework.web.multipart.MultipartFile file) {
        return repo.findById(id)
                .map(existing -> {
                    cuento.setId(id);
                    if (file != null && !file.isEmpty()) {
                        String fileName = "cuentos/" + java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        firebaseStorageService.upload(file, fileName);
                        String fileUrl = firebaseStorageService.generateSignedUrl(fileName);
                        cuento.setImagenUrl(fileUrl);
                    } else {
                        cuento.setImagenUrl(existing.getImagenUrl()); // Keep existing image if not provided
                    }
                    return repo.save(cuento);
                });
    }

    public Optional<Cuento> updateEstado(Long id, Cuento cuento) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setHabilitado(cuento.isHabilitado());
                    return repo.save(existing);
                });
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
