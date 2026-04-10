package ts.backend_carddropper.minigames.dodge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.minigames.dodge.entity.DodgeScore;
import ts.backend_carddropper.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryDodge extends JpaRepository<DodgeScore, Long> {

    Optional<DodgeScore> findByUserAndWeekStart(User user, LocalDate weekStart);

    List<DodgeScore> findTop5ByWeekStartOrderByBestScoreDesc(LocalDate weekStart);
}
