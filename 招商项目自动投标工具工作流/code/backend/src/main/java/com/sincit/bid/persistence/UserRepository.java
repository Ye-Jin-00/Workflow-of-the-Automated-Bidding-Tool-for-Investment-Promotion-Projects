package com.sincit.bid.persistence;

import com.sincit.bid.domain.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JsonDataStore store;

    public UserRepository(JsonDataStore store) {
        this.store = store;
    }

    public synchronized Optional<User> findByUsername(String username) {
        return store.database().getUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public synchronized Optional<User> findById(String id) {
        return store.database().getUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public synchronized List<User> findAll() {
        return new ArrayList<User>(store.database().getUsers());
    }

    public synchronized User save(User user) {
        List<User> users = store.database().getUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                store.save();
                return user;
            }
        }
        users.add(user);
        store.save();
        return user;
    }

    public synchronized long count() {
        return store.database().getUsers().size();
    }
}
