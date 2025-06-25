package devlava.jwt.dto;

import lombok.*;

@Data
@Builder
public class EventAvailableResponse {
    private boolean available;
    private String reason;
}
