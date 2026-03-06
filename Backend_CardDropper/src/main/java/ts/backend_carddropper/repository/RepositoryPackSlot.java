package ts.backend_carddropper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.PackSlot;

import java.util.Optional;

@Repository
public interface RepositoryPackSlot extends JpaRepository<PackSlot, Long> {

    Optional<PackSlot> findByName(String name);
}
