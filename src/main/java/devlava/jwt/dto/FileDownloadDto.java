package devlava.jwt.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@Builder
public class FileDownloadDto {
    private final Resource resource;
    private final String originalFileName;
}