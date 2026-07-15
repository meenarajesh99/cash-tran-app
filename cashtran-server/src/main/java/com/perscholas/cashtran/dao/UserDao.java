
package com.perscholas.cashtran.dao;

import com.perscholas.cashtran.model.User;

import java.util.List;

public interface UserDao {

    public long findIdByAccountID(long accountId);

    String findUserByAccountID(long accountId);

    List<User> findAll(long id);

    User findByUsername(String username);

    long findIdByUsername(String username);

    boolean create(String username, String password);
}

