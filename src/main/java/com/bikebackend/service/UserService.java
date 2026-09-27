package com.bikebackend.service;

import com.bikebackend.config.RedisCache;
import com.bikebackend.dao.UserDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.User;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public long createUser(User user) throws SQLException {
        if (user.getUsername() == null || user.getUsername().isBlank()) throw new BadRequestException("Username is required");
        if (user.getEmail() == null || user.getEmail().isBlank()) throw new BadRequestException("Email is required");
        if (user.getPassword() == null || user.getPassword().isBlank()) throw new BadRequestException("Password is required");
        long uid = userDAO.createUser(user);
        invalidateUsers();
        return uid;
    }

    public User getUser(long uid) throws SQLException {
        User user = RedisCache.get(RedisCache.userKey(uid), User.class);
        if (user == null) {
            user = userDAO.getUserById(uid);
            if (user != null) RedisCache.put(RedisCache.userKey(uid), user);
        }
        if (user == null) throw new NotFoundException("User not found");
        return user;
    }

    public void updateUser(User user) throws SQLException {
        if (user.getUid() == null) throw new BadRequestException("User ID is required");
        if (!userDAO.updateUser(user)) throw new NotFoundException("User not found");
        invalidateUser(user.getUid());
    }

    public void changePassword(long uid, String password) throws SQLException {
        if (password == null || password.isBlank()) throw new BadRequestException("Password is required");
        if (!userDAO.updatePassword(uid, password)) throw new NotFoundException("User not found");
        invalidateUser(uid);
    }

    public void changeStatus(long uid, String status) throws SQLException {
        if (status == null || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"BLOCKED".equals(status)))
            throw new BadRequestException("Status must be ACTIVE, INACTIVE or BLOCKED");
        if (!userDAO.updateStatus(uid, status)) throw new NotFoundException("User not found");
        invalidateUser(uid);
    }

    public List<User> getUsers(String search, String status, String sort, String order) throws SQLException {
        long version = RedisCache.version(RedisCache.usersVersionKey());
        String key = RedisCache.userListKey(version, search, status, sort, order);
        List<User> users = RedisCache.get(key, RedisCache.USERS);
        if (users == null) {
            users = userDAO.getAllUsers(search, status, sort, order);
            RedisCache.put(key, users);
        }
        return users;
    }

    private void invalidateUser(long uid) { RedisCache.evict(RedisCache.userKey(uid)); invalidateUsers(); }
    private void invalidateUsers() { RedisCache.bumpVersion(RedisCache.usersVersionKey()); }
}
