package devlava.jwt.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class EventHistoryResponse {
    private String memberId;
    private String memberName;
    private boolean draw;
    private int rank;
    private Date createDt;
}