package devlava.jwt.repository;

import devlava.jwt.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("SELECT t FROM Token t JOIN FETCH t.member m LEFT JOIN FETCH m.role WHERE t.refreshToken = :refreshToken")
    Optional<Token> findByRefreshTokenWithMemberAndRole(@Param("refreshToken") String refreshToken);

    Optional<Token> findByRefreshToken(String refreshToken);

    Optional<Token> findByMemberId(String memberId);

    void deleteByMemberId(String memberId);
}