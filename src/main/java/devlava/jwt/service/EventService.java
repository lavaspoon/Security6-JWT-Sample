package devlava.jwt.service;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.dto.EventHistoryResponse;
import devlava.jwt.dto.EventResultResponse;
import devlava.jwt.entity.Member;
import devlava.jwt.entity.TbLmsEvent;
import devlava.jwt.repository.MemberRepository;
import devlava.jwt.repository.TbLmsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 해당 이벤트는 6월 25일 - 6월 26일 진행 이벤트 입니다.
 * 각 일자에 1등 2명, 2등 3명 제한을 두고 있습니다.
 */
@Service
@Transactional
public class EventService {

    @Autowired
    TbLmsEventRepository repository;

    @Autowired
    MemberRepository memberRepository;

    private static final LocalDate EVENT_START_DATE = LocalDate.of(2025, 6, 25);
    private static final LocalDate EVENT_END_DATE = LocalDate.of(2025, 6, 26);
    private static final int FIRST_PRIZE_LIMIT = 2; // 1등 당첨자 제한 수
    private static final int SECOND_PRIZE_LIMIT = 3; // 2등 당첨자 제한 수

    /**
     * 특정 날짜의 등수별 당첨자 수를 조회합니다.
     */
    private record PrizeCount(long rank1Count, long rank2Count) {
    }

    /**
     * 오늘 날짜의 등수별 당첨자 수를 조회합니다.
     */
    private PrizeCount getTodayPrizeCounts(Date startOfDay, Date endOfDay) {
        long rank1Count = repository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 1);
        long rank2Count = repository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 2);
        return new PrizeCount(rank1Count, rank2Count);
    }

    /**
     * 이벤트 참여 가능 여부를 검증한 결과
     */
    private record ValidationResult(boolean isValid, String errorMessage) {
        static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        static ValidationResult fail(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }

    /**
     * 이벤트 참여 가능 여부를 검증합니다.
     */
    private ValidationResult validateEventParticipation(String memberId, boolean isDraw) {
        LocalDate today = LocalDate.now();

        // 1. 이벤트 기간 검증
        if (today.isBefore(EVENT_START_DATE) || today.isAfter(EVENT_END_DATE)) {
            return ValidationResult.fail("이벤트 기간이 아닙니다.");
        }

        // 2. 전체 이벤트 기간 중 당첨 이력 검증
        Optional<TbLmsEvent> drawResultByMemberId = repository.findDrawResultByMemberId(memberId);
        if (drawResultByMemberId.isPresent()) {
            TbLmsEvent event = drawResultByMemberId.get();
            return ValidationResult.fail("이벤트 당첨 내역이 있습니다. (" + event.getRank() + "등)");
        }

        // 3. 오늘 참여 이력 검증
        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<TbLmsEvent> todayEvents = repository.findByMemberIdAndCreateDtBetween(memberId, startOfDay, endOfDay);
        if (!todayEvents.isEmpty()) {
            return ValidationResult.fail("오늘 이미 참여하셨습니다.");
        }

        // 4. 당첨자 수 검증 (당첨 시에만)
        if (isDraw) {
            PrizeCount prizeCount = getTodayPrizeCounts(startOfDay, endOfDay);
            if (prizeCount.rank1Count() >= FIRST_PRIZE_LIMIT && prizeCount.rank2Count() >= SECOND_PRIZE_LIMIT) {
                return ValidationResult.fail("오늘 당첨 인원이 모두 찼습니다.");
            }
        }

        return ValidationResult.success();
    }

    public EventAvailableResponse isAvailable(String memberId) {
        ValidationResult validationResult = validateEventParticipation(memberId, true);

        return EventAvailableResponse.builder()
                .available(validationResult.isValid())
                .reason(validationResult.isValid() ? "참여 가능합니다." : validationResult.errorMessage())
                .build();
    }

    public EventResultResponse save(boolean isDraw, String memberId) {
        // 참여 가능 여부 검증
        ValidationResult validationResult = validateEventParticipation(memberId, isDraw);
        if (!validationResult.isValid()) {
            return EventResultResponse.builder()
                    .message(validationResult.errorMessage())
                    .build();
        }

        LocalDate today = LocalDate.now();
        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Member 엔티티 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 당첨시 rank 부여하여 저장, 미당첨시 rank 0 부여하여 저장
        if (isDraw) {
            // 1등, 2등 인원 확인
            PrizeCount prizeCount = getTodayPrizeCounts(startOfDay, endOfDay);
            int rank;
            String resultMessage;

            // 1등이 2명 미만이면 1등
            if (prizeCount.rank1Count() < FIRST_PRIZE_LIMIT) {
                rank = 1;
                resultMessage = "축하합니다! 1등에 당첨되셨습니다!";
            }
            // 1등이 찼고, 2등이 3명 미만이면 2등
            else if (prizeCount.rank2Count() < SECOND_PRIZE_LIMIT) {
                rank = 2;
                resultMessage = "축하합니다! 2등에 당첨되셨습니다!";
            }
            // 1등, 2등 모두 찼으면 당첨 불가
            else {
                return EventResultResponse.builder()
                        .message("죄송합니다. 오늘 당첨 인원이 모두 찼습니다.")
                        .build();
            }

            TbLmsEvent event = TbLmsEvent.builder()
                    .member(member)
                    .draw(true)
                    .rank(rank)
                    .createDt(new Date())
                    .build();

            repository.save(event);

            return EventResultResponse.builder()
                    .message(resultMessage)
                    .build();
        } else {
            // 미당첨자는 rank 0으로 저장
            TbLmsEvent event = TbLmsEvent.builder()
                    .member(member)
                    .draw(false)
                    .rank(0)
                    .createDt(new Date())
                    .build();

            repository.save(event);

            return EventResultResponse.builder()
                    .message("아쉽게도 당첨되지 않았습니다. 다음 기회에 도전해주세요!")
                    .build();
        }
    }

    /**
     * 이벤트 기간의 참여 내역을 조회합니다.
     * 
     * @return 이벤트 참여 내역 목록
     */
    public List<EventHistoryResponse> getEventHistory() {
        // 이벤트 시작일의 00:00:00
        Date startDt = Date.from(EVENT_START_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // 이벤트 종료일의 23:59:59
        Date endDt = Date
                .from(EVENT_END_DATE.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant());

        List<TbLmsEvent> events = repository.findEventHistoryByDateRange(startDt, endDt);

        return events.stream()
                .map(event -> EventHistoryResponse.builder()
                        .memberId(event.getMember().getId())
                        .memberName(event.getMember().getUsername())
                        .draw(event.isDraw())
                        .rank(event.getRank())
                        .createDt(event.getCreateDt())
                        .build())
                .collect(Collectors.toList());
    }
}