package devlava.jwt.repository;

import devlava.jwt.entity.Member;
import devlava.jwt.entity.TbLmsEvent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TbLmsEventRepositoryTest {

    @Autowired
    private TbLmsEventRepository eventRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId("2125089");
        testMember.setUsername("euhno");
        testMember.setName("lava");
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("회원의 이벤트 참여 내역이 없는 경우 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoEventParticipation() {
        // when
        List<TbLmsEvent> events = eventRepository.findByMemberId(testMember.getId());

        // then
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("회원의 이벤트 참여 내역이 있는 경우 참여 내역을 반환한다")
    void shouldReturnEventListWhenMemberParticipated() {
        // given
        TbLmsEvent event = new TbLmsEvent();
        event.setMember(testMember);
        event.setDraw(true);
        event.setRank(1);
        event.setCreateDt(new Date()); // 현재 시간 설정
        eventRepository.save(event);

        // when
        List<TbLmsEvent> events = eventRepository.findByMemberId(testMember.getId());

        // then
        assertThat(events)
                .isNotEmpty()
                .hasSize(1);
        assertThat(events.get(0))
                .satisfies(savedEvent -> {
                    assertThat(savedEvent.getMember()).isEqualTo(testMember);
                    assertThat(savedEvent.isDraw()).isTrue();
                    assertThat(savedEvent.getRank()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("여러 날짜의 이벤트 중 당일 참여 이벤트만 조회된다")
    void shouldOnlyReturnTodayEventParticipation() {
        // given
        Calendar cal = Calendar.getInstance();

        // 현재 시간 기준으로 오늘 자정 설정
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfToday = cal.getTime();

        // 내일 자정 설정
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date startOfTomorrow = cal.getTime();

        // 어제 이벤트 (어제 오후 11시 59분)
        cal.add(Calendar.DAY_OF_MONTH, -2); // 어제로 이동
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        TbLmsEvent yesterdayEvent = new TbLmsEvent();
        yesterdayEvent.setMember(testMember);
        yesterdayEvent.setDraw(true);
        yesterdayEvent.setRank(1);
        yesterdayEvent.setCreateDt(cal.getTime());
        eventRepository.save(yesterdayEvent);

        // 오늘 이벤트 (오늘 오후 12시)
        cal.add(Calendar.DAY_OF_MONTH, 1); // 오늘로 이동
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        TbLmsEvent todayEvent = new TbLmsEvent();
        todayEvent.setMember(testMember);
        todayEvent.setDraw(true);
        todayEvent.setRank(2);
        todayEvent.setCreateDt(cal.getTime());
        eventRepository.save(todayEvent);

        // 내일 이벤트 (내일 오전 00시 01분)
        cal.add(Calendar.DAY_OF_MONTH, 1); // 내일로 이동
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 1);
        cal.set(Calendar.SECOND, 0);

        TbLmsEvent tomorrowEvent = new TbLmsEvent();
        tomorrowEvent.setMember(testMember);
        tomorrowEvent.setDraw(true);
        tomorrowEvent.setRank(3);
        tomorrowEvent.setCreateDt(cal.getTime());
        eventRepository.save(tomorrowEvent);

        // when
        List<TbLmsEvent> todayEvents = eventRepository.findByMemberIdAndCreateDtBetween(
                testMember.getId(), startOfToday, startOfTomorrow);

        // then
        System.out.println("todayEvents.size() = " + todayEvents.size());
        System.out.println("Today start: " + startOfToday);
        System.out.println("Today end: " + startOfTomorrow);
        todayEvents.forEach(event -> {
            System.out.println("Event date: " + event.getCreateDt() + ", rank: " + event.getRank());
        });

        assertThat(todayEvents)
                .isNotEmpty()
                .hasSize(1);
        assertThat(todayEvents.get(0))
                .satisfies(savedEvent -> {
                    assertThat(savedEvent.getMember()).isEqualTo(testMember);
                    assertThat(savedEvent.getRank()).isEqualTo(2); // 오늘 이벤트의 rank는 2
                    assertThat(savedEvent.getCreateDt()).isBetween(startOfToday, startOfTomorrow);
                });
    }

    private Date getDateWithOffset(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}