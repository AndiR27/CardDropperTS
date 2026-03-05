package ts.backend_carddropper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ts.backend_carddropper.entity.LiveFeedEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositoryLiveFeed extends JpaRepository<LiveFeedEvent, Long> {

    List<LiveFeedEvent> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
}
