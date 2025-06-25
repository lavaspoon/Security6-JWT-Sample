package devlava.jwt.repository;

import devlava.jwt.entity.TbLmsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface TbLmsEventRepository extends JpaRepository<TbLmsEvent, Long> {

    @Query("""
                select e from TbLmsEvent e join fetch e.member m where m.id = :memberId
            """)
    List<TbLmsEvent> findByMemberId(String memberId);

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
}
