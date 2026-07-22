package com.perscholas.cashtran.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "authority")
public class Authority {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "authority_id")
  private Long authorityId;

  @Column(name = "authority_name", nullable = false, unique = true)
  private String authorityName;

  public Authority() {}

  public Authority(String authorityName) {
    this.authorityName = authorityName;
  }

  public Long getAuthorityId() {
    return authorityId;
  }

  public void setAuthorityId(Long authorityId) {
    this.authorityId = authorityId;
  }

  public String getAuthorityName() {
    return authorityName;
  }

  public void setAuthorityName(String name) {
    this.authorityName = authorityName;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) return true;

    if (!(o instanceof Authority)) return false;

    Authority that = (Authority) o;

    return Objects.equals(authorityName, that.authorityName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorityName);
  }
}
