package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrlProperty;

    @org.springframework.scheduling.annotation.Async
    public void enviarNotificacionCambioEstado(Order order, OrderStatus nuevoEstado) {
        if (mailSender == null || fromEmail == null || fromEmail.isEmpty()) {
            System.out.println("[EmailService] Mail no configurado, omitiendo envío para pedido #" + order.getId());
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage mensaje = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    mensaje, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());

            String asunto = "Actualización de tu pedido #" + order.getId() + " - Cuentos de Killa";

            // Construir HTML Base
            StringBuilder html = new StringBuilder();
            html.append(
                    "<div style='background-color: #F9F4EE; padding: 40px 20px; font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif;'>");
            html.append(
                    "  <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>");

            // Header
            html.append("    <div style='background-color: #b37b42; padding: 25px; text-align: center;'>");
            html.append(
                    "      <h1 style='margin: 0; color: #ffffff; font-size: 24px; font-weight: bold; letter-spacing: 1px;'>Cuentos de Killa</h1>");
            html.append("    </div>");

            // Body
            html.append("    <div style='padding: 40px 30px; color: #4A3C31;'>");
            html.append("      <h2 style='margin-top: 0; font-size: 20px; color: #b37b42;'>¡Hola, "
                    + order.getUser().getNombre() + "! 🌙</h2>");

            html.append("      <p style='font-size: 16px; line-height: 1.6; margin-bottom: 25px;'>");
            html.append("        Queríamos avisarte que hay una actualización en el estado de tu pedido <strong>#"
                    + order.getId() + "</strong>.");
            html.append("      </p>");

            // Status Box
            html.append(
                    "      <div style='background-color: #FDF9F5; border-left: 4px solid #b37b42; padding: 15px 20px; margin-bottom: 30px; border-radius: 0 8px 8px 0;'>");
            html.append(
                    "        <p style='margin: 0; font-size: 14px; color: #8C6A4A; text-transform: uppercase; font-weight: bold; letter-spacing: 0.5px;'>Estado actual</p>");
            html.append("        <h3 style='margin: 5px 0 0 0; color: #b37b42; font-size: 22px;'>"
                    + getNuevoEstadoAmigable(nuevoEstado) + "</h3>");
            html.append("      </div>");

            // Dynamic Message
            html.append("      <p style='font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>");
            if (nuevoEstado == OrderStatus.PAGO_PENDIENTE) {
                html.append(
                        "¡Tu pedido ha sido registrado con éxito! Aún estamos a la espera de tu pago o de la subida de tu comprobante en la plataforma para empezar a prepararlo.");
            } else if (nuevoEstado == OrderStatus.PAGO_ENVIADO) {
                html.append(
                        "Hemos recibido tu comprobante de pago. Nuestro equipo lo revisará en breve para confirmar tu pedido.");
            } else if (nuevoEstado == OrderStatus.PAGADO || nuevoEstado == OrderStatus.PAGO_VERIFICADO
                    || nuevoEstado == OrderStatus.VERIFICADO) {
                html.append(
                        "¡Genial! Hemos confirmado tu pago. Ya estamos trabajando con mucha ilusión en la preparación de tus mágicos cuentos.");
            } else if (nuevoEstado == OrderStatus.EMPAQUETADO) {
                html.append("¡Tus cuentos ya están empaquetados y listos para salir de nuestro taller!");
            } else if (nuevoEstado == OrderStatus.ENVIADO) {
                html.append("¡Buenas noticias! Tu pedido ya está en camino. Muy pronto la magia llegará a tus manos.");
            } else if (nuevoEstado == OrderStatus.ENTREGADO) {
                html.append(
                        "¡Tu pedido ha sido entregado exitosamente! Esperamos que disfrutes mucho de la magia de Cuentos de Killa.");
            } else if (nuevoEstado == OrderStatus.PAGO_RECHAZADO) {
                html.append(
                        "Hemos tenido un inconveniente al verificar el pago de tu pedido. Por favor, revisa el motivo a continuación y contáctanos si necesitas ayuda.");
            } else {
                html.append(
                        "El estado de tu pedido ha cambiado. Puedes ver los detalles haciendo clic en el botón de abajo.");
            }
            html.append("      </p>");

            // Resumen de Artículos (Tabla estilo Frontend)
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                html.append(
                        "      <div style='background-color: #ffffff; border: 1px solid #F0E6DD; border-radius: 8px; margin-bottom: 30px; padding: 25px;'>");
                html.append(
                        "        <h3 style='margin: 0 0 20px 0; color: #b37b42; font-size: 18px; border-bottom: 1px dashed #F0E6DD; padding-bottom: 15px;'>🛍️ Artículos del Pedido</h3>");

                html.append(
                        "        <table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>");
                for (com.forjix.cuentoskilla.model.OrderItem item : order.getItems()) {
                    html.append("          <tr>");
                    html.append("            <td width='70' style='padding-bottom: 15px;'>");
                    html.append("              <img src='" + item.getImagen_url()
                            + "' style='width: 60px; height: 60px; object-fit: cover; border-radius: 6px; border: 1px solid #F0E6DD;' alt='"
                            + item.getNombre() + "'/>");
                    html.append("            </td>");
                    html.append("            <td style='padding-bottom: 15px; vertical-align: middle;'>");
                    html.append("              <p style='margin: 0; font-weight: bold; color: #4A3C31;'>"
                            + item.getNombre() + "</p>");
                    html.append("              <p style='margin: 5px 0 0 0; font-size: 13px; color: #8C6A4A;'>x"
                            + item.getCantidad() + " unidad(es) &nbsp;&nbsp;|&nbsp;&nbsp; PEN"
                            + String.format(java.util.Locale.US, "%.2f", item.getPrecio_unitario()) + " c/u</p>");
                    html.append("            </td>");
                    html.append(
                            "            <td width='90' align='right' style='padding-bottom: 15px; vertical-align: middle;'>");
                    html.append("              <p style='margin: 0; font-weight: bold; color: #4A3C31;'>PEN"
                            + String.format(java.util.Locale.US, "%.2f", item.getSubtotal()) + "</p>");
                    html.append("            </td>");
                    html.append("          </tr>");
                }
                html.append("        </table>");

                html.append(
                        "        <table width='100%' cellpadding='0' cellspacing='0' style='border-top: 1px dashed #F0E6DD; margin-top: 10px; padding-top: 20px;'>");
                html.append("          <tr>");
                html.append("            <td align='right' style='padding-top: 20px;'>");
                html.append(
                        "              <span style='font-size: 16px; color: #8C6A4A; margin-right: 15px;'>Total del Pedido</span>");
                html.append("              <span style='font-size: 20px; color: #b37b42; font-weight: bold;'>PEN"
                        + String.format(java.util.Locale.US, "%.2f", order.getTotal()) + "</span>");
                html.append("            </td>");
                html.append("          </tr>");
                html.append("        </table>");
                html.append("      </div>");
            }

            // Rejection Info
            if (nuevoEstado == OrderStatus.PAGO_RECHAZADO && order.getMotivoRechazo() != null
                    && !order.getMotivoRechazo().isEmpty()) {
                html.append(
                        "      <div style='background-color: #FFF3F3; padding: 15px; border-radius: 8px; margin-bottom: 30px; border: 1px solid #FFCDD2;'>");
                html.append("        <p style='margin: 0; color: #D32F2F;'><strong>Motivo del rechazo:</strong><br/>"
                        + order.getMotivoRechazo() + "</p>");
                html.append("      </div>");
            }

            // Call to Action
            String urlPedido = frontendUrlProperty + "/pedidos/" + order.getId();

            html.append("      <div style='text-align: center; margin-top: 40px; margin-bottom: 20px;'>");
            html.append("        <a href='" + urlPedido
                    + "' style='background-color: #b37b42; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;'>Ver Detalle de mi Pedido</a>");
            html.append("      </div>");

            if (nuevoEstado == OrderStatus.PAGO_RECHAZADO) {
                String wspMensaje = "Hola equipo de Cuentos de Killa, tengo una consulta sobre mi pedido rechazado #"
                        + order.getId();
                String wspEnlace = "https://wa.me/51999999999?text=" + java.net.URLEncoder.encode(wspMensaje, "UTF-8");
                html.append("      <div style='text-align: center; margin-top: 15px;'>");
                html.append("        <a href='" + wspEnlace
                        + "' style='color: #25D366; text-decoration: none; font-weight: bold; font-size: 15px;'>✉️ Contactar Soporte por WhatsApp</a>");
                html.append("      </div>");
            }

            html.append("    </div>"); // End Body

            // Footer
            html.append(
                    "    <div style='background-color: #FDF9F5; padding: 20px; text-align: center; border-top: 1px solid #F0E6DD;'>");
            html.append("      <p style='margin: 0; font-size: 14px; color: #8C6A4A;'>");
            html.append("        Gracias por dejar entrar la magia a tu hogar.<br/>");
            html.append("        <strong>El equipo de Cuentos de Killa</strong>");
            html.append("      </p>");
            html.append("    </div>");

            html.append("  </div>");
            html.append("</div>");

            helper.setSubject(asunto);
            helper.setText(html.toString(), true); // true indica que es HTML

            mailSender.send(mensaje);
            System.out.println("[EmailService] Correo HTML enviado a " + order.getUser().getEmail() + " (Estado: "
                    + nuevoEstado + ")");
        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando correo HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getNuevoEstadoAmigable(OrderStatus estado) {
        switch (estado) {
            case GENERADO:
                return "Pedido Generado";
            case PAGO_PENDIENTE:
                return "Pago Pendiente";
            case PAGO_ENVIADO:
                return "Comprobante Enviado";
            case PAGADO:
                return "Pago Verificado";
            case VERIFICADO:
                return "Pago Verificado";
            case EMPAQUETADO:
                return "Pedido en Empaquetado";
            case ENVIADO:
                return "Pedido Enviado";
            case ENTREGADO:
                return "Pedido Entregado";
            case PAGO_VERIFICADO:
                return "Pago Verificado";
            case PAGO_RECHAZADO:
                return "Pago Rechazado";
            default:
                return estado.name();
        }
    }
}
