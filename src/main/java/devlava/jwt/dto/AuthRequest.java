package devlava.jwt.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    private String id;
    private String username;
    private String name;
    private String roleName; // Optional role name field
}