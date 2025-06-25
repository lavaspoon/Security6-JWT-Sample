package devlava.jwt.service;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.entity.TbLmsEvent;
import devlava.jwt.repository.TbLmsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    @Autowired
    TbLmsEventRepository repository;

    public EventAvailableResponse isAvailable(String memberId) {
        List<TbLmsEvent> result = repository.findByMemberId(memberId);


        // 1. 이벤트 당첨 내역 여부 체크
        if (result.size() > 0) {

            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("이벤트 당첨 내역이 있습니다.")
                    .build();
        }

        // 2. 당일 참여했는지 여부 체크
        if (result.size() > 0) {

            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("당일 참여 내역이 있습니다.")
                    .build();
        }

        return EventAvailableResponse.builder()
                .available(true)
                .reason("참여 가능합니다.")
                .build();
    }
}
