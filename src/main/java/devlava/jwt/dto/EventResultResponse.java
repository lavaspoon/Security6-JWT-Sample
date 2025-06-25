package devlava.jwt.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventResultResponse {
    private String message;
}
