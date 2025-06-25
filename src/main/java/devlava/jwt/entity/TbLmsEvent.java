package devlava.jwt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "TB_LMS_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbLmsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "draw")
    private boolean draw;

    @Column(name = "rank")
    private int rank;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATE_DT", updatable = false)
    private Date createDt;

    // 연관관계 편의 메서드
    public void asMember(Member member) {
        this.member = member;
        member.getEvents().add(this);
    }

}
