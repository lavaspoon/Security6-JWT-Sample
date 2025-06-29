package devlava.jwt.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BoardDto {
    private Long boardId;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<String> links = new ArrayList<>();

    @Builder.Default
    private List<BoardRefDto> attachments = new ArrayList<>();

    @Builder.Default
    private List<Long> remainingFileIds = new ArrayList<>();

    @Data
    @Builder
    public static class BoardRefDto {
        private Long id;
        private String originalFileName;
        private String storedFileName;
        private String fileType;
        private String fileUrl;
        private Long fileSize;
        private LocalDateTime createdAt;
    }
}
