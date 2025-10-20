package com.ecommerce.project.security.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserInfoResponse {
    private Long id;
    private String username;
    private List<String> roles;

    public UserInfoResponse(Long id, List<String> roles, String username) {
        this.id = id;
        this.roles = roles;
        this.username = username;
    }
}
