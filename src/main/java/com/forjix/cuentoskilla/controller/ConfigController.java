package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.ConfigItemId;
import com.forjix.cuentoskilla.model.ConfigCategory;
import com.forjix.cuentoskilla.model.ConfigItem;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.repository.ConfigCategoryRepository;
import com.forjix.cuentoskilla.repository.ConfigItemRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de Configuración del Sistema
 * 
 * Rutas: /api/v1/config
 * 
 * Funcionalidad:
 * - GET /category: Listar categorías de configuración
 * - POST /category: Crear categoría (solo ADMIN)
 * - GET /category/{code}/item: Listar items de categoría
 * - POST /category/{code}/item: Crear item (solo ADMIN)
 * - PUT /category/{code}/item/{id2}: Actualizar item (solo ADMIN)
 * - DELETE /category/{code}/item/{id2}: Eliminar item (solo ADMIN)
 * - GET /category/{code}/item/{id2}: Obtener item específico
 * 
 * Seguridad (OWASP):
 * - GET disponible públicamente
 * - POST/PUT/DELETE restringido a ADMIN
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private final ConfigCategoryRepository categoryRepo;
    private final ConfigItemRepository itemRepo;

    public ConfigController(ConfigCategoryRepository categoryRepo, ConfigItemRepository itemRepo) {
        this.categoryRepo = categoryRepo;
        this.itemRepo = itemRepo;
    }

    /**
     * Listar todas las categorías de configuración
     * GET /api/v1/config/category
     * Acceso: Público
     */
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<ConfigCategory>>> listCategories() {
        logger.info("GET /api/v1/config/category - Listando categorías de configuración");
        List<ConfigCategory> categories = categoryRepo.findAll();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categorías obtenidas exitosamente"));
    }

    /**
     * Crear nueva categoría de configuración
     * POST /api/v1/config/category
     * Acceso: Solo ADMIN
     */
    @PostMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigCategory>> createCategory(@RequestBody ConfigCategory category) {
        logger.info("POST /api/v1/config/category - Admin creando categoría: {}", category.getCode());
        if (categoryRepo.existsByCode(category.getCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("DUPLICATE_CODE", "El código de categoría ya existe"));
        }
        ConfigCategory saved = categoryRepo.save(category);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved, "Categoría creada exitosamente"));
    }

    /**
     * Listar items de una categoría
     * GET /api/v1/config/category/{code}/item
     * Acceso: Público
     */
    @GetMapping("/category/{code}/item")
    public ResponseEntity<ApiResponse<List<ConfigItem>>> listItems(@PathVariable String code) {
        logger.info("GET /api/v1/config/category/{}/item - Listando items de categoría", code);
        return categoryRepo.findByCode(code)
                .map(cat -> ResponseEntity.ok(ApiResponse.success(
                    itemRepo.findByCategoryIdOrderById2(cat.getId1()),
                    "Items de categoría obtenidos exitosamente"
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("CATEGORY_NOT_FOUND", "Categoría no encontrada")));
    }

    /**
     * Crear item en una categoría
     * POST /api/v1/config/category/{code}/item
     * Acceso: Solo ADMIN
     */
    @PostMapping("/category/{code}/item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigItem>> createItem(@PathVariable String code, @RequestBody ConfigItem item) {
        logger.info("POST /api/v1/config/category/{}/item - Admin creando item", code);
        return categoryRepo.findByCode(code)
                .map(cat -> {
                    item.setCategoryId(cat.getId1());
                    ConfigItem saved = itemRepo.save(item);
                    return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(saved, "Item creado exitosamente"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("CATEGORY_NOT_FOUND", "Categoría no encontrada")));
    }

    /**
     * Actualizar item de una categoría
     * PUT /api/v1/config/category/{code}/item/{id2}
     * Acceso: Solo ADMIN
     */
    @PutMapping("/category/{code}/item/{id2}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigItem>> updateItem(
            @PathVariable String code,
            @PathVariable Integer id2,
            @RequestBody ConfigItem item) {
        logger.info("PUT /api/v1/config/category/{}/item/{} - Admin actualizando item", code, id2);
        return categoryRepo.findByCode(code)
            .map(cat -> {
                ConfigItemId itemId = new ConfigItemId(cat.getId1(), id2);
                return itemRepo.findById(itemId)
                        .map(existing -> {
                            existing.setLabel(item.getLabel());
                            existing.setData(item.getData());
                            existing.setSensitive(item.getSensitive());
                            ConfigItem updated = itemRepo.save(existing);
                            return ResponseEntity.ok(ApiResponse.success(
                                updated,
                                "Item actualizado exitosamente"
                            ));
                        })
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("ITEM_NOT_FOUND", "Item no encontrado")));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("CATEGORY_NOT_FOUND", "Categoría no encontrada")));
    }

    /**
     * Eliminar item de una categoría
     * DELETE /api/v1/config/category/{code}/item/{id2}
     * Acceso: Solo ADMIN
     */
    @DeleteMapping("/category/{code}/item/{id2}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable String code, @PathVariable Integer id2) {
        logger.info("DELETE /api/v1/config/category/{}/item/{} - Admin eliminando item", code, id2);
        return categoryRepo.findByCode(code)
            .map(cat -> {
                ConfigItemId itemId = new ConfigItemId(cat.getId1(), id2);
                if (!itemRepo.existsById(itemId)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .<ApiResponse<Void>>body(ApiResponse.error("ITEM_NOT_FOUND", "Item no encontrado"));
                }
                itemRepo.deleteById(itemId);
                return ResponseEntity.ok(ApiResponse.<Void>success(null, "Item eliminado exitosamente"));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("CATEGORY_NOT_FOUND", "Categoría no encontrada")));
    }
    
    /**
     * Obtener item específico de una categoría
     * GET /api/v1/config/category/{code}/item/{id2}
     * Acceso: Público
     */
    @GetMapping("/category/{code}/item/{id2}")
    public ResponseEntity<ApiResponse<ConfigItem>> getItemByCategory(
            @PathVariable String code,
            @PathVariable Integer id2) {
        logger.info("GET /api/v1/config/category/{}/item/{} - Obteniendo item específico", code, id2);
        return categoryRepo.findByCode(code)
            .flatMap(cat -> itemRepo.findById(new ConfigItemId(cat.getId1(), id2)))
            .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Item obtenido exitosamente")))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ITEM_NOT_FOUND", "Item no encontrado")));
    }
}
