package com.ecommerce.project.security.services;

import com.ecommerce.project.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {
  private static final Long serialVersionUID = 1L;

  private Long id;
  private String email;
  private String username;

  @JsonIgnore private String password;

  private Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImpl(
      Collection<? extends GrantedAuthority> authorities,
      String email,
      Long id,
      String password,
      String username) {
    this.id = id;
    this.authorities = authorities;
    this.email = email;
    this.password = password;
    this.username = username;
  }

  public static UserDetailsImpl build(User user) {
    List<GrantedAuthority> authorities =
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
            .collect(Collectors.toList());

    return new UserDetailsImpl(
        authorities, user.getEmail(), user.getUserId(), user.getPassword(), user.getUserName());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return UserDetails.super.isEnabled();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserDetailsImpl user = (UserDetailsImpl) o;
    return Objects.equals(id, user.id);
  }
}
