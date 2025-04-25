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

    public Optional<User> findByUid(String uid) {
        return repo.findByUid(uid);
    }

    public User save(User user) {
        return repo.save(user);
    }
}
