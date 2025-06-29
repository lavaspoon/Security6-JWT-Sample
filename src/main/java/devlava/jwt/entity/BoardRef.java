package devlava.jwt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "board_refs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String fileType; // PDF, IMAGE, VIDEO_LINK

    @Column
    private String fileUrl; // 실제 파일 저장 경로 또는 영상 링크 URL

    @Column
    private Long fileSize; // 파일 크기 (바이트)

    @CreationTimestamp
    @Column(name = "CREATE_DT", updatable = false)
    private LocalDateTime createdAt;
}
