package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.ConfigCategory;
import com.forjix.cuentoskilla.config.ConfigItem;
import com.forjix.cuentoskilla.config.ConfigItemId;
import com.forjix.cuentoskilla.repository.ConfigCategoryRepository;
import com.forjix.cuentoskilla.repository.ConfigItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin
public class ConfigController {

    private final ConfigCategoryRepository categoryRepo;
    private final ConfigItemRepository itemRepo;

    public ConfigController(ConfigCategoryRepository categoryRepo, ConfigItemRepository itemRepo) {
        this.categoryRepo = categoryRepo;
        this.itemRepo = itemRepo;
    }

    @GetMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConfigCategory> listCategories() {
        return categoryRepo.findAll();
    }

    @PostMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(@RequestBody ConfigCategory category) {
        if (categoryRepo.existsByCode(category.getCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "code already exists"));
        }
        ConfigCategory saved = categoryRepo.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/category/{code}/item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ConfigItem>> listItems(@PathVariable String code) {
        return categoryRepo.findByCode(code)
                .map(cat -> ResponseEntity.ok(itemRepo.findByCategoryIdOrderById2(cat.getId1())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/category/{code}/item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createItem(@PathVariable String code, @RequestBody ConfigItem item) {
        return categoryRepo.findByCode(code)
                .map(cat -> {
                    item.setCategoryId(cat.getId1());
                    ConfigItem saved = itemRepo.save(item);
                    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/category/{code}/item/{id2}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigItem> updateItem(@PathVariable String code, @PathVariable Integer id2,
                                                 @RequestBody ConfigItem item) {
        return categoryRepo.findByCode(code).map(cat -> {
            ConfigItemId itemId = new ConfigItemId(cat.getId1(), id2);
            return itemRepo.findById(itemId)
                    .map(existing -> {
                        existing.setLabel(item.getLabel());
                        existing.setData(item.getData());
                        existing.setSensitive(item.getSensitive());
                        return ResponseEntity.ok(itemRepo.save(existing));
                    })
                    .orElse(ResponseEntity.notFound().build());
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/category/{code}/item/{id2}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable String code, @PathVariable Integer id2) {
        return categoryRepo.findByCode(code).map(cat -> {
            ConfigItemId itemId = new ConfigItemId(cat.getId1(), id2);
            if (!itemRepo.existsById(itemId)) {
                return ResponseEntity.notFound().build();
            }
            itemRepo.deleteById(itemId);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
