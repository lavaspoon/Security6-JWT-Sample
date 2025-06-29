package devlava.jwt.repository;

import devlava.jwt.entity.BoardRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRefRepository extends JpaRepository<BoardRef, Long> {
}