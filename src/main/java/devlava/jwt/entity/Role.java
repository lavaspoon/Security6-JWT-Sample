package devlava.jwt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ROLE")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    public enum RoleType {
        ADMIN,
        USER,
        MANAGER
    }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType roleName;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Member member;
}
