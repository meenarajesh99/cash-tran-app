package com.perscholas.cashtran.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cashtran_user")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private boolean activated;

//  @Column(nullable = false)
//  private boolean mfaEnabled = false;
//
//    @Column(name = "phone_number", unique = true)
//    private String phoneNumber;

  /*
   * User <-> Authority
   *
   * user_authority:
   *
   * user_id
   * authority_id
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "cashtran_user_authority",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "authority_id"))
  private Set<Authority> authorities = new HashSet<>();

  /*
   * User -> Account
   *
   * Account owns relationship
   * through account.user_id
   */
  @OneToOne(
      mappedBy = "user",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @JsonIgnore
  private Account account;

  public User() {}

  public User(String username, String password, String email, boolean activated) {

    this.username = username;
    this.password = password;
    this.email = email;
    this.activated = activated;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isActivated() {
    return activated;
  }

  public void setActivated(boolean activated) {
    this.activated = activated;
  }

  /*
   * Required by Spring Security UserDetails
   */
  public boolean isEnabled() {
    return activated;
  }

  public Set<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Authority> authorities) {
    this.authorities.clear();
    if (authorities != null) {
      this.authorities.addAll(authorities);
    }
  }

  public void addAuthority(Authority authority) {
    if (authority != null) {
      authorities.add(authority);
    }
  }

  public void removeAuthority(Authority authority) {
    authorities.remove(authority);
  }

  /*
   * Prefer this method:
   *
   * Authority authority =
   * authorityRepository.findByName("ROLE_USER");
   *
   * user.addAuthority(authority);
   *
   * instead of creating new Authority objects.
   */
  public boolean hasAuthority(String authorityName) {
    return authorities.stream()
        .anyMatch(authority -> authority.getAuthorityName().equals(authorityName));
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
    if (account != null && account.getUser() != this) {
      account.setUser(this);
    }
  }

  /*
   * Keep both sides synchronized
   */
  public void removeAccount() {

    if (account != null) {
      account.setUser(null);
      account = null;
    }
  }

//    public String getPhoneNumber() {
//        return phoneNumber;
//    }
//
//    public void setPhoneNumber(String phoneNumber) {
//        this.phoneNumber = phoneNumber;
//    }
//
//    public boolean isMfaEnabled() {
//        return mfaEnabled;
//    }
//
//    public void setMfaEnabled(boolean mfaEnabled) {
//        this.mfaEnabled = mfaEnabled;
//    }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User)) {
      return false;
    }

    User user = (User) o;
    return userId != null && userId.equals(user.userId);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "User{"
        + "userId="
        + userId
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", activated="
        + activated
        + '}';
  }
}
