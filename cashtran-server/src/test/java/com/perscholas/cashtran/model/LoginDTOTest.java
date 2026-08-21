package com.perscholas.cashtran.model;

import com.perscholas.cashtran.dto.LoginDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginDTOTest {

    LoginDTO login = new LoginDTO();

    @Test
    public void getUsername_equals_Username() {
        login.setUsername("Username");
        assertEquals(login.getUsername(), "Username");
    }

    @Test
    public void getPassword_equals_Password() {
        login.setPassword("Password");
        assertEquals(login.getPassword(), "Password");
    }

    @Test
    void testToString_Creates_Accurate_Transfer_Object() {
        login.setUsername("Username");
        login.setPassword("Password");
        String accurateDTO = login.toString();
        assertEquals(accurateDTO, "LoginDTO{" + "username='" + "Username" + '\'' + ", password='" + "[PROTECTED]" + '\'' + '}');
    }
}
