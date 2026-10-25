package com.nvqn.entity;

import com.nvqn.Util.IdGenerator;
import com.nvqn.constant.Role;
import com.nvqn.constant.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@Table(name = "app_users")
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue()
    UUID id;

    @Column(nullable = false,updatable = false)
    String code;

    @Column(nullable = false,unique = true)
    String phone;

    @Column(nullable = false,unique = true)
    String email;


    LocalDateTime dateOfBirth;


    @Column(nullable = false)
    String password;

    @Enumerated(EnumType.STRING)
    Role role;

    @Enumerated(EnumType.STRING)
    UserStatus userStatus;

    @CreationTimestamp
    LocalDateTime createTime;

    @UpdateTimestamp
    LocalDateTime updateTime;

    @PrePersist
    void generateCode() {
        if (code != null) {return;}

        if (role == null) {
            throw new IllegalStateException("Role must not be null");
        }

        String prefix = switch (role) {
            case CLIENT -> "CLI";
            case ADMIN -> "ADM";
        };

        code = prefix + "-" + IdGenerator.randomCode(10);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+ this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return userStatus != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {

        return this.userStatus == UserStatus.ACTIVE;
    }
}
