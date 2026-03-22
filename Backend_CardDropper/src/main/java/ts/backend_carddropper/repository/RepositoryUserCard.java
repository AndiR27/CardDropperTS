package ts.backend_carddropper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.UserCard;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryUserCard extends JpaRepository<UserCard, Long> {

    Optional<UserCard> findByUserIdAndCardId(Long userId, Long cardId);

    boolean existsByUserIdAndCardId(Long userId, Long cardId);

    List<UserCard> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByCardId(Long cardId);

    boolean existsByCardId(Long cardId);
}
