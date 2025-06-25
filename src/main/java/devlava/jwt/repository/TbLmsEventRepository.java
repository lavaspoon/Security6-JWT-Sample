package devlava.jwt.repository;

import devlava.jwt.entity.TbLmsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TbLmsEventRepository extends JpaRepository<TbLmsEvent, Long> {

    /* 해당 계정의 이벤트 당첨 내역 확인 */
    @Query("""
                select e from TbLmsEvent e
                    join fetch e.member m
                where m.id = :memberId
                    and e.draw = true
            """)
    Optional<TbLmsEvent> findDrawResultByMemberId(String memberId);

    @Query("""
                select e from TbLmsEvent e
                join fetch e.member m
                where m.id = :memberId
                and e.createDt >= :startDate
                and e.createDt < :endDate
            """)
    List<TbLmsEvent> findByMemberIdAndCreateDtBetween(
            @Param("memberId") String memberId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    // 당일 등수별 인원 수 조회
    @Query("""
                select count(e) from TbLmsEvent e
                where e.createDt >= :startDate
                     and e.createDt < :endDate
                and e.rank = :rank
            """)
    long countByCreateDtBetweenAndRank(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("rank") int rank);

    // 특정 기간의 이벤트 참여 내역 조회 (생성일시 기준 내림차순)
    @Query("SELECT e FROM TbLmsEvent e JOIN FETCH e.member WHERE e.createDt BETWEEN :startDt AND :endDt ORDER BY e.createDt DESC")
    List<TbLmsEvent> findEventHistoryByDateRange(@Param("startDt") Date startDt, @Param("endDt") Date endDt);
}
