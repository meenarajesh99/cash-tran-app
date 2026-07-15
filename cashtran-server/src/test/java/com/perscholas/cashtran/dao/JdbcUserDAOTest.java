package com.perscholas.cashtran.dao;

import com.perscholas.cashtran.model.Transfer;
import com.perscholas.cashtran.model.User;
import org.junit.jupiter.api.*;
import org.springframework.util.Assert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JdbcUserDAOTest extends DaoIntegrationTest{
    public static final User USER_1 = new User(1L,"Anne","Pass","ROLE_USER");
    public static final User USER_2 = new User(2L,"Roland","Pass","ROLE_USER");
    public static final User USER_3 = new User(3L,"Andy","Pass","ROLE_USER");

    private JdbcUserDAO sut;

    private User userTest;

    @BeforeAll
    public void setup(){
        sut = new JdbcUserDAO(dataSource);
        userTest = new User(1004L,"Robot","Pass","Admin");
    }

    @Test
    public void findUserByAccountID() {
        String findUser = sut.findUserByAccountID(2L);
        assertNotNull(findUser);
        assertEquals(USER_2.getUsername(),findUser);

    }

    @Test
    public void findIdByUsername() {
        Long findId = sut.findIdByUsername("Anne");
        assertNotNull(findId);
        assertEquals(USER_1.getId(),findId);


    }

    @Test
    public void findAll() {
        List<User> listUsers = sut.findAll(2);

        assertEquals(2,listUsers.size());
        assertUserMatch(USER_1,listUsers.get(0));
        assertUserMatch(USER_3, listUsers.get(1));

    }

    @Test
    public void findByUsername() {
        User findByUsername = sut.findByUsername("Anne");
        assertNotNull(findByUsername);
        assertUserMatch(USER_1,findByUsername);
    }

    @Test
    public void create() {
        boolean createNewUser = sut.create("Robot","Pass");

        assertTrue(createNewUser);

    }

    private void assertUserMatch
            (User expected, User actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getPassword(), actual.getPassword());

    }

}

