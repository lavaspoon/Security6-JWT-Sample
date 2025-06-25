package devlava.jwt.service;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.entity.TbLmsEvent;
import devlava.jwt.repository.TbLmsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    TbLmsEventRepository repository;

    private static final LocalDate EVENT_START_DATE = LocalDate.of(2025, 6, 25);
    private static final LocalDate EVENT_END_DATE = LocalDate.of(2025, 6, 26);

    public EventAvailableResponse isAvailable(String memberId) {
        LocalDate today = LocalDate.now();

        // 1. 이벤트 기간 외에는 참여 불가x
        if (today.isBefore(EVENT_START_DATE) || today.isAfter(EVENT_END_DATE)) {
            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("이벤트 기간이 아닙니다.")
                    .build();
        }

        // 2. 전체 이벤트 기간 중 당첨된 적 있는지 확인
        Optional<TbLmsEvent> drawResultByMemberId = repository.findDrawResultByMemberId(memberId);
        if (drawResultByMemberId.isPresent()) {
            TbLmsEvent event = drawResultByMemberId.get();

            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("이벤트 당첨 내역이 있습니다. (" + event.getRank() + "등)")
                    .build();
        }

        // 3. 오늘 날짜 기준 참여 여부 확인
        System.out.println("startofDay: " + today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        System.out.println("plusDays: " + today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        System.out.println("================");
        List<TbLmsEvent> todayEvents = repository.findByMemberIdAndCreateDtBetween(memberId, startOfDay, endOfDay);
        System.out.println("================");
        if (!todayEvents.isEmpty()) {
            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("오늘 이미 참여하셨습니다.")
                    .build();
        }

        // 4. 오늘 등수별 당첨 인원 확인
        long rank1Count = repository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 1);
        long rank2Count = repository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 2);

        if (rank1Count >= 2 && rank2Count >= 3) {
            return EventAvailableResponse.builder()
                    .available(false)
                    .reason("오늘 당첨 인원이 모두 찼습니다.")
                    .build();
        }

        return EventAvailableResponse.builder()
                .available(true)
                .reason("참여 가능합니다.")
                .build();
    }
}