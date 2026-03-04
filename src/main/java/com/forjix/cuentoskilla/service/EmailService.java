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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void enviarNotificacionCambioEstado(Order order, OrderStatus nuevoEstado) {
        if (mailSender == null || fromEmail == null || fromEmail.isEmpty()) {
            System.out.println("[EmailService] Mail no configurado, omitiendo envío para pedido #" + order.getId());
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(fromEmail);
            mensaje.setTo(order.getUser().getEmail());

            String asunto = "Actualización de tu pedido #" + order.getId() + " - Cuentos de Killa";
            String cuerpo = "Hola,\n\n" +
                    "El estado de tu pedido #" + order.getId() + " ha sido actualizado.\n\n" +
                    "Nuevo Estado: " + nuevoEstado.name() + "\n\n" +
                    "Gracias por comprar con nosotros,\n" +
                    "El equipo de Cuentos de Killa";

            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            mailSender.send(mensaje);
            System.out.println("[EmailService] Correo enviado a " + order.getUser().getEmail());
        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando correo: " + e.getMessage());
        }
    }
}
