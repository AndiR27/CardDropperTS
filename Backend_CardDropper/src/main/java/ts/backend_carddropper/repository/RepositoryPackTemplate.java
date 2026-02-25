package ts.backend_carddropper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.PackTemplate;

import java.util.Optional;

@Repository
public interface RepositoryPackTemplate extends JpaRepository<PackTemplate, Long> {

    Optional<PackTemplate> findByName(String name);
}
