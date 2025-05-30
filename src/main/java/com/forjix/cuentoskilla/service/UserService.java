package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    public User save(User user) {
        return repo.save(user);
    }
}
