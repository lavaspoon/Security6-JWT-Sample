package devlava.jwt.repository;

import devlava.jwt.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByUsername(String username);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.role WHERE m.id = :id")
    Optional<Member> findByIdWithRole(@Param("id") String id);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.role WHERE m.username = :username")
    Optional<Member> findByUsernameWithRole(@Param("username") String username);
}