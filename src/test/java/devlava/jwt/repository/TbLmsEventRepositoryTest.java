package devlava.jwt.repository;

import devlava.jwt.entity.Member;
import devlava.jwt.entity.TbLmsEvent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TbLmsEventRepositoryTest {

    @Autowired
    private TbLmsEventRepository eventRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember1;
    private Member testMember2;
    private Member testMember3;
    private Member testMember4;
    private Member testMember5;

    @BeforeEach
    void setUp() {
        testMember1 = new Member();
        testMember1.setId("1111");
        testMember1.setUsername("1");
        testMember1.setName("lava");
        memberRepository.save(testMember1);

        testMember2 = new Member();
        testMember2.setId("2222");
        testMember2.setUsername("2");
        testMember2.setName("lava");
        memberRepository.save(testMember2);

        testMember3 = new Member();
        testMember3.setId("3333");
        testMember3.setUsername("3");
        testMember3.setName("lava");
        memberRepository.save(testMember3);

        testMember4 = new Member();
        testMember4.setId("4444");
        testMember4.setUsername("4");
        testMember4.setName("lava");
        memberRepository.save(testMember4);

        testMember5 = new Member();
        testMember5.setId("5555");
        testMember5.setUsername("5");
        testMember5.setName("lava");
        memberRepository.save(testMember5);
    }

    @Test
    @DisplayName("당첨내역이 없는 경우")
    void findDrawResultByMemberId_1() {
        // given

        // when
        Optional<TbLmsEvent> result = eventRepository.findDrawResultByMemberId(testMember1.getId());

        // then
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("당첨내역이 있는 경우")
    void findDrawResultByMemberId_2() {
        // given
        TbLmsEvent event1 = new TbLmsEvent();
        event1.setDraw(false);
        event1.setMember(testMember1);
        event1.setCreateDt(new Date());
        eventRepository.save(event1);

        TbLmsEvent event2 = new TbLmsEvent();
        event2.setDraw(true);
        event2.setMember(testMember2);
        event2.setRank(1);
        event2.setCreateDt(new Date());
        eventRepository.save(event2);

        TbLmsEvent event3 = new TbLmsEvent();
        event3.setDraw(true);
        event3.setMember(testMember3);
        event3.setRank(1);
        event3.setCreateDt(new Date());
        eventRepository.save(event3);
        // when
        Optional<TbLmsEvent> result = eventRepository.findDrawResultByMemberId(testMember2.getId());
        // then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("당일 등수별 인원 조회")
    public void countByCreateDtBetweenAndRank_1() throws Exception {
        // given
        LocalDate today = LocalDate.now();
        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date yesterday = Date.from(
                LocalDate.now()
                        .minusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        TbLmsEvent event1 = new TbLmsEvent();
        event1.setDraw(false);
        event1.setMember(testMember1);
        event1.setCreateDt(yesterday);
        eventRepository.save(event1);

        TbLmsEvent event2 = new TbLmsEvent();
        event2.setDraw(true);
        event2.setMember(testMember2);
        event2.setRank(1);
        event2.setCreateDt(yesterday);
        eventRepository.save(event2);

        TbLmsEvent event3 = new TbLmsEvent();
        event3.setDraw(true);
        event3.setMember(testMember3);
        event3.setRank(1);
        event3.setCreateDt(new Date());
        eventRepository.save(event3);

        TbLmsEvent event4 = new TbLmsEvent();
        event4.setDraw(true);
        event4.setMember(testMember4);
        event4.setRank(1);
        event4.setCreateDt(new Date());
        eventRepository.save(event4);

        TbLmsEvent event5 = new TbLmsEvent();
        event5.setDraw(true);
        event5.setMember(testMember5);
        event5.setRank(2);
        event5.setCreateDt(new Date());
        eventRepository.save(event5);

        TbLmsEvent event6 = new TbLmsEvent();
        event6.setDraw(true);
        event6.setMember(testMember1);
        event6.setRank(2);
        event6.setCreateDt(new Date());
        eventRepository.save(event6);
        //when
        long rank1 = eventRepository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 1);
        long rank2 = eventRepository.countByCreateDtBetweenAndRank(startOfDay, endOfDay, 2);
        //then
        assertThat(rank1).isEqualTo(2L);
        assertThat(rank2).isEqualTo(2L);
    }
}