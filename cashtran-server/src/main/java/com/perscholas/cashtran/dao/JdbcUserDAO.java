package com.perscholas.cashtran.dao;

import com.perscholas.cashtran.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcUserDAO implements UserDao {

    private JdbcTemplate jdbcTemplate;

    // defines starting balance
    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");

    public JdbcUserDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    //uses account ID to select user ID
    @Override
    public long findIdByAccountID(long accountId){
        String sql = "SELECT user_id FROM account WHERE account_id = ?";
        long userId = jdbcTemplate.queryForObject(sql, Long.class, accountId);
        return userId;

    }

    //uses account ID to select username
    @Override
    public String findUserByAccountID(long accountId){
        String sql = "SELECT username FROM cashtran_user JOIN account ON account.user_id = cashtran_user.user_id WHERE account_id = ?";
        String username = jdbcTemplate.queryForObject(sql, String.class, accountId);
        return username;

    }

    // uses username to get user ID
    @Override
    public long findIdByUsername(String username) {
        String sql = "SELECT user_id FROM cashtran_user WHERE username ILIKE ?";
        List<Long> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("user_id"),
                username
        );
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    //lists all users excluding current logged in user
    @Override
    public List<User> findAll(long userId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash FROM cashtran_user WHERE NOT user_id = ?;";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
        while(results.next()) {
            User user = mapRowToUser(results);
            users.add(user);
        }
        return users;
    }

    //pulls user id/name/hash based on username search
    @Override
    public User findByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT user_id, username, password_hash FROM cashtran_user WHERE username ILIKE ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()){
            return mapRowToUser(rowSet);
        }
        throw new UsernameNotFoundException("User " + username + " was not found.");
    }

    //user creation
    @Override
    public boolean create(String username, String password) {

        // create user
        String sql = "INSERT INTO cashtran_user (username, password_hash) VALUES (?, ?) RETURNING user_id";
        String password_hash = new BCryptPasswordEncoder().encode(password);
        Integer newUserId;
        try {
            newUserId = jdbcTemplate.queryForObject(sql, Integer.class, username, password_hash);
        } catch (DataAccessException e) {
            return false;
        }

        // create account
        sql = "INSERT INTO account (user_id, balance) values(?, ?)";
        try {
            jdbcTemplate.update(sql, newUserId, STARTING_BALANCE);
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setActivated(true);
        user.setAuthorities("USER");
        return user;
    }
}

