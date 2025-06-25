package devlava.jwt.dto;

import devlava.jwt.entity.Role.RoleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private RoleType role;

    public AuthResponse(String accessToken, String refreshToken, RoleType role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
    }
}