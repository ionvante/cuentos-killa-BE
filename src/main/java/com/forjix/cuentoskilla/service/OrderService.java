package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final Set<String> TIPOS_DIRECCION_VALIDOS = Set.of("CASA", "TRABAJO", "FAMILIAR", "OTRO");

    private final OrderRepository orderRepo;
    private final CuentoRepository cuentoRepo;
    private final UserRepository userRepo;
    private final DireccionRepository direccionRepo;
    private final MercadoPagoService mercadoPagoService;
    private final EmailService emailService;
    private final BoletaService boletaService;

    @Autowired
    public OrderService(OrderRepository orderRepo,
                        CuentoRepository cuentoRepo,
                        UserRepository userRepo,
                        DireccionRepository direccionRepo,
                        MercadoPagoService mercadoPagoService,
                        EmailService emailService,
                        BoletaService boletaService) {
        this.orderRepo = orderRepo;
        this.cuentoRepo = cuentoRepo;
        this.userRepo = userRepo;
        this.direccionRepo = direccionRepo;
        this.mercadoPagoService = mercadoPagoService;
        this.emailService = emailService;
        this.boletaService = boletaService;
    }

    private void populateOrderItemDetails(OrderItem item) {
        if (item.getCuento() != null) {
            Cuento cuento = cuentoRepo.findById(item.getCuento().getId())
                    .orElseThrow(() -> new RuntimeException("Cuento not found for item: " + item.getId()));
            item.setNombre(cuento.getTitulo());
            item.setImagen_url(cuento.getImagenUrl());
            BigDecimal precioUnitario = BigDecimal.valueOf(item.getPrecio_unitario());
            item.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())));
        }
    }

    private void populateOrderItems(Order order) {
        if (order != null && order.getItems() != null) {
            order.getItems().forEach(this::populateOrderItemDetails);
        }
    }

    private PedidoItemDTO mapToItemDTO(OrderItem item) {
        PedidoItemDTO dto = new PedidoItemDTO();
        dto.setCuentoId(item.getCuento().getId());
        dto.setNombreCuento(item.getNombre());
        dto.setImagenUrl(item.getImagen_url());
        dto.setPrecioUnitario(BigDecimal.valueOf(item.getPrecio_unitario()));
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private PedidoDTO mapToPedidoDTO(Order order) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(order.getId());
        dto.setIdCapital(order.getId());
        if (order.getCreatedAt() != null) {
            dto.setFecha(order.getCreatedAt().toString());
        }
        if (order.getUser() != null) {
            dto.setNombre(order.getUser().getNombre());
            dto.setCorreo(order.getUser().getEmail());
            dto.setTelefono(order.getUser().getTelefono());
            dto.setUserId(order.getUser().getId());
            dto.setCorreoUsuario(order.getUser().getEmail());
            dto.setDocumentoTipo(order.getUser().getDocumentoTipo());
            dto.setDocumentoNumero(order.getUser().getDocumentoNumero());
        }
        dto.setDireccionId(order.getDireccionId());
        dto.setDireccion(order.getDireccion());
        dto.setTipoDireccion(order.getTipoDireccion());
        dto.setTipoEntrega(order.getTipoEntrega());
        dto.setDepartamento(order.getDepartamento());
        dto.setProvincia(order.getProvincia());
        dto.setDistrito(order.getDistrito());
        dto.setCalle(order.getCalle());
        dto.setReferencia(order.getReferencia());
        dto.setCodigoPostal(order.getCodigoPostal());
        dto.setEstado(order.getEstado().toString());
        dto.setTotal(order.getTotal() != null ? order.getTotal().doubleValue() : 0);
        dto.setItems(order.getItems().stream().map(this::mapToItemDTO).collect(Collectors.toList()));
        return dto;
    }

    public PedidoDTO convertToPedidoDTO(Order order) {
        return mapToPedidoDTO(order);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> getOrders(long userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<Order> orders = orderRepo.findAll();
        orders.forEach(this::populateOrderItems);
        return orders.stream().map(this::mapToPedidoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> getOrdersByUser(long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<Order> orders = orderRepo.findByUser_Id(user.getId());
        orders.forEach(this::populateOrderItems);
        return orders.stream().map(this::mapToPedidoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdAndUser(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to access this order.");
        }
        populateOrderItems(order);
        return order;
    }

    @Transactional
    public Order save(PedidoDTO pedidoDTO, User authenticatedUser) {
        Order order = new Order();
        order.setUser(authenticatedUser);
        order.setCreatedAt(LocalDateTime.now());
        order.setEstado(OrderStatus.PAGO_PENDIENTE);

        aplicarSnapshotDireccion(order, pedidoDTO, authenticatedUser);

        List<OrderItem> items = pedidoDTO.getItems().stream().map(dto -> {
            Cuento cuento = cuentoRepo.findById(dto.getCuentoId())
                    .orElseThrow(() -> new RuntimeException("Cuento no encontrado con ID: " + dto.getCuentoId()));

            OrderItem item = new OrderItem();
            item.setCuento(cuento);
            item.setCantidad(dto.getCantidad());
            item.setPrecio_unitario(cuento.getPrecio());
            item.setOrder(order);
            item.setNombre(cuento.getTitulo());
            item.setImagen_url(cuento.getImagenUrl());
            BigDecimal precioUnitario = BigDecimal.valueOf(cuento.getPrecio());
            item.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(dto.getCantidad())));
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        return orderRepo.save(order);
    }

    @Transactional
    public String initiatePayment(long orderId, long userId) throws MPException, MPApiException {
        Order order = getOrderByIdAndUser(orderId, userId);

        if (order.getEstado() != OrderStatus.PAGO_PENDIENTE) {
            throw new IllegalStateException("Order is not in PENDIENTE state, cannot initiate payment.");
        }

        PedidoDTO pedidoDTO = mapToPedidoDTO(order);
        return mercadoPagoService.createPaymentPreference(pedidoDTO, order.getId()).getInitPoint();
    }

    @Transactional(readOnly = true)
    public OrderStatus getOrderStatus(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to access this order.");
        }
        return order.getEstado();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String motivo, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        User adminUser = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (adminUser.getRole() != Rol.ADMIN) {
            throw new SecurityException("User not authorized to change order status.");
        }

        order.setEstado(newStatus);
        if (motivo != null && !motivo.isEmpty()) {
            order.setMotivoRechazo(motivo);
        }
        orderRepo.save(order);

        if (newStatus == OrderStatus.PAGO_VERIFICADO) {
            try {
                Boleta boleta = boletaService.generarBoletaSiCorresponde(order.getId());
                if (boleta.getEstadoGeneracion() == BoletaGeneracionEstado.ERROR) {
                    logger.warn("Pedido {} quedo en PAGO_VERIFICADO pero boleta en ERROR. intentos={}, ultimoError={}",
                            order.getId(), boleta.getIntentos(), boleta.getUltimoError());
                }
            } catch (Exception ex) {
                logger.warn("Pedido {} quedo en PAGO_VERIFICADO pero boleta no pudo procesarse: {}",
                        order.getId(), ex.getMessage(), ex);
            }
        }

        emailService.enviarNotificacionCambioEstado(order, newStatus);
    }

    @Transactional
    public void processWebhookPayment(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (order.getEstado() != OrderStatus.ENTREGADO && order.getEstado() != OrderStatus.PAGADO
                && order.getEstado() != OrderStatus.VERIFICADO) {
            order.setEstado(OrderStatus.PAGADO);
            orderRepo.save(order);
            emailService.enviarNotificacionCambioEstado(order, OrderStatus.PAGADO);
        }
    }

    @Transactional
    public boolean deleteOrderByIdAndUser(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElse(null);
        if (order == null) {
            return false;
        }
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to delete this order.");
        }
        if (order.getEstado() == OrderStatus.PAGADO) {
            throw new IllegalStateException("Cannot delete an already paid order.");
        }
        orderRepo.delete(order);
        return true;
    }

    private void aplicarSnapshotDireccion(Order order, PedidoDTO pedidoDTO, User authenticatedUser) {
        Direccion direccionGuardada = null;
        if (pedidoDTO.getDireccionId() != null) {
            direccionGuardada = direccionRepo.findById(pedidoDTO.getDireccionId())
                    .orElseThrow(() -> new NoSuchElementException("Direccion no encontrada"));
            if (direccionGuardada.getUsuario() == null
                    || !authenticatedUser.getId().equals(direccionGuardada.getUsuario().getId())) {
                throw new SecurityException("Direccion no pertenece al usuario autenticado");
            }
        }

        order.setDireccionId(pedidoDTO.getDireccionId());
        order.setTipoDireccion(normalizeTipoDireccion(firstNonBlank(pedidoDTO.getTipoDireccion(),
                direccionGuardada != null ? direccionGuardada.getTipoDireccion() : null)));
        order.setTipoEntrega(pedidoDTO.getTipoEntrega());
        order.setDepartamento(firstNonBlank(pedidoDTO.getDepartamento(),
                direccionGuardada != null ? direccionGuardada.getDepartamento() : null));
        order.setProvincia(firstNonBlank(pedidoDTO.getProvincia(),
                direccionGuardada != null ? direccionGuardada.getProvincia() : null));
        order.setDistrito(firstNonBlank(pedidoDTO.getDistrito(),
                direccionGuardada != null ? direccionGuardada.getDistrito() : null));
        order.setCalle(firstNonBlank(pedidoDTO.getCalle(),
                direccionGuardada != null ? direccionGuardada.getCalle() : null));
        order.setReferencia(firstNonBlank(pedidoDTO.getReferencia(),
                direccionGuardada != null ? direccionGuardada.getReferencia() : null));
        order.setCodigoPostal(firstNonBlank(pedidoDTO.getCodigoPostal(),
                direccionGuardada != null ? direccionGuardada.getCodigoPostal() : null));

        String direccionTexto = firstNonBlank(
                pedidoDTO.getDireccion(),
                formatDireccion(order.getCalle(), order.getReferencia(), order.getDistrito(), order.getProvincia(),
                        order.getDepartamento(), order.getCodigoPostal())
        );
        order.setDireccion(direccionTexto);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String formatDireccion(String calle,
                                   String referencia,
                                   String distrito,
                                   String provincia,
                                   String departamento,
                                   String codigoPostal) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, calle);
        addIfPresent(parts, referencia);
        addIfPresent(parts, distrito);
        addIfPresent(parts, provincia);
        addIfPresent(parts, departamento);
        addIfPresent(parts, codigoPostal);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private String normalizeTipoDireccion(String tipoDireccion) {
        if (tipoDireccion == null || tipoDireccion.isBlank()) {
            return null;
        }
        String normalizado = tipoDireccion.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_DIRECCION_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Tipo de direccion invalido");
        }
        return normalizado;
    }
}
