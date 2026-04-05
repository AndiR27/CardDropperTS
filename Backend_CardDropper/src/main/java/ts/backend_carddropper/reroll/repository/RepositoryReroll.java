package ts.backend_carddropper.reroll.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.reroll.entity.UserReroll;

import java.util.Optional;

@Repository
public interface RepositoryReroll extends JpaRepository<UserReroll, Long> {

    Optional<UserReroll> findByUserId(Long userId);
}
