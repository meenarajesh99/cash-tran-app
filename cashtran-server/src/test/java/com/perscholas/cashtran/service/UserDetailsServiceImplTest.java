package com.perscholas.cashtran.service;

import com.perscholas.cashtran.exception.UserNotActivatedException;
import com.perscholas.cashtran.model.Authority;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

  @Mock UserRepository userRepository;

  @InjectMocks UserDetailsServiceImpl userDetailsService;

  @Test
  void loadUserByUsernameReturnsUserDetailsWhenActivated() {
    User user = new User("alice", "encoded", "alice@example.com", true);
    Authority role = new Authority("ROLE_USER");
    user.addAuthority(role);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    UserDetails ud = userDetailsService.loadUserByUsername("alice");

    assertEquals("alice", ud.getUsername());
    assertEquals("encoded", ud.getPassword());
    assertTrue(ud.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void loadUserByUsernameThrowsWhenNotFound() {
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("missing"));
  }

  @Test
  void loadUserByUsernameThrowsWhenNotActivated() {
    User user = new User("bob", "pwd", "bob@example.com", false);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

    assertThrows(UserNotActivatedException.class, () -> userDetailsService.loadUserByUsername("bob"));
  }
}

