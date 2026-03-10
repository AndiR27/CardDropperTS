package ts.backend_carddropper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.UserPackInventory;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryUserPackInventory extends JpaRepository<UserPackInventory, Long> {

    Optional<UserPackInventory> findByUserIdAndPackTemplateId(Long userId, Long packTemplateId);

    List<UserPackInventory> findByUserIdAndQuantityGreaterThan(Long userId, int minQuantity);
}
