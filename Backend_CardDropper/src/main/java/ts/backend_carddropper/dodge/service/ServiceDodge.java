package ts.backend_carddropper.dodge.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.dodge.entity.DodgeScore;
import ts.backend_carddropper.dodge.mapping.MapperDodge;
import ts.backend_carddropper.dodge.models.DodgeScoreDto;
import ts.backend_carddropper.dodge.repository.RepositoryDodge;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.repository.RepositoryUser;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceDodge {

    private final RepositoryDodge repositoryDodge;
    private final RepositoryUser repositoryUser;
    private final MapperDodge mapperDodge;
    private final DodgeTokenService tokenService;

    // score = 1 point per 100ms → max 10 pts/sec
    // allow 15% margin for timing drift
    private static final double SCORE_PER_MS = 10.0 / 1000.0;
    private static final double TIMING_MARGIN = 1.15;

    public String startGame(String keycloakId) {
        return tokenService.createToken(keycloakId);
    }

    @Transactional
    public DodgeScoreDto submitScore(String keycloakId, String token, int score) {
        // verify token signature + ownership
        DodgeTokenService.TokenData data = tokenService.verify(token, keycloakId);

        // check timing: score must be plausible for elapsed time
        long elapsedMs = System.currentTimeMillis() - data.startedAtMs();
        int maxScore = (int) (elapsedMs * SCORE_PER_MS * TIMING_MARGIN);

        if (score > maxScore) {
            log.warn("Dodge: rejected score {} from {} (max plausible: {}, elapsed: {}ms)",
                    score, keycloakId, maxScore, elapsedMs);
            throw new IllegalArgumentException("Score exceeds maximum plausible for game duration");
        }

        // token must not be from the future or too old (max 1h)
        if (elapsedMs < 0 || elapsedMs > 3_600_000) {
            throw new IllegalArgumentException("Invalid game session timing");
        }

        User user = findUser(keycloakId);
        LocalDate weekStart = currentWeekStart();

        DodgeScore dodgeScore = repositoryDodge.findByUserAndWeekStart(user, weekStart)
                .orElseGet(() -> new DodgeScore(user, 0, weekStart));

        if (score > dodgeScore.getBestScore()) {
            dodgeScore.setBestScore(score);
            dodgeScore = repositoryDodge.save(dodgeScore);
            log.info("Dodge: {} set new weekly best {} (week {})", user.getUsername(), score, weekStart);
        }

        return mapperDodge.toDto(dodgeScore);
    }

    public DodgeScoreDto getMyScore(String keycloakId) {
        User user = findUser(keycloakId);
        LocalDate weekStart = currentWeekStart();

        return repositoryDodge.findByUserAndWeekStart(user, weekStart)
                .map(mapperDodge::toDto)
                .orElse(new DodgeScoreDto(null, 0, user.getUsername(), weekStart));
    }

    public List<DodgeScoreDto> getTopScores() {
        LocalDate weekStart = currentWeekStart();
        return repositoryDodge.findTop5ByWeekStartOrderByBestScoreDesc(weekStart)
                .stream()
                .map(mapperDodge::toDto)
                .toList();
    }

    private User findUser(String keycloakId) {
        return repositoryUser.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException("User not found for keycloakId: " + keycloakId));
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }
}
