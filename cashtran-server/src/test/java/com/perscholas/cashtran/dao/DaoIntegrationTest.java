package com.perscholas.cashtran.dao;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.SQLException;
@Disabled
public class DaoIntegrationTest {

    static SingleConnectionDataSource dataSource;

    @BeforeAll
    public static void setupDataSource() {
        dataSource = new SingleConnectionDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/test-data");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres1");
        dataSource.setAutoCommit(false);
    }

    @AfterAll
    public static void closeDataSource() throws SQLException {
        dataSource.destroy();
    }

    @AfterEach
    public void rollback() throws SQLException {
        dataSource.getConnection().rollback();
    }
}

