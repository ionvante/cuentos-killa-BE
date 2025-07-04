package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.DTOs.TimeCount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager em;

    public List<TimeCount> countCuentosByInterval(LocalDateTime start, LocalDateTime end, String unit) {
        String jpql = "SELECT NEW com.forjix.cuentoskilla.model.DTOs.TimeCount(FUNCTION('date_trunc', :unit, c.fechaIngreso), COUNT(c)) " +
                "FROM Cuento c WHERE c.fechaIngreso BETWEEN :start AND :end " +
                "GROUP BY FUNCTION('date_trunc', :unit, c.fechaIngreso) " +
                "ORDER BY FUNCTION('date_trunc', :unit, c.fechaIngreso)";
        TypedQuery<TimeCount> query = em.createQuery(jpql, TimeCount.class);
        query.setParameter("unit", unit);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }

    public List<TimeCount> countOrdersByStatusAndInterval(OrderStatus status, LocalDateTime start, LocalDateTime end, String unit) {
        String jpql = "SELECT NEW com.forjix.cuentoskilla.model.DTOs.TimeCount(FUNCTION('date_trunc', :unit, o.created_at), COUNT(o)) " +
                "FROM Order o WHERE o.created_at BETWEEN :start AND :end AND o.estado = :status " +
                "GROUP BY FUNCTION('date_trunc', :unit, o.created_at) " +
                "ORDER BY FUNCTION('date_trunc', :unit, o.created_at)";
        TypedQuery<TimeCount> query = em.createQuery(jpql, TimeCount.class);
        query.setParameter("unit", unit);
        query.setParameter("start", start);
        query.setParameter("end", end);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<TimeCount> countUsersByInterval(LocalDateTime start, LocalDateTime end, String unit) {
        String jpql = "SELECT NEW com.forjix.cuentoskilla.model.DTOs.TimeCount(FUNCTION('date_trunc', :unit, u.createdAt), COUNT(u)) " +
                "FROM User u WHERE u.createdAt BETWEEN :start AND :end " +
                "GROUP BY FUNCTION('date_trunc', :unit, u.createdAt) " +
                "ORDER BY FUNCTION('date_trunc', :unit, u.createdAt)";
        TypedQuery<TimeCount> query = em.createQuery(jpql, TimeCount.class);
        query.setParameter("unit", unit);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getResultList();
    }
}
