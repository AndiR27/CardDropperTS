package ts.backend_carddropper.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.User;

import java.util.Optional;

@Repository
public interface RepositoryUser extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    // Méthode pour récupérer un utilisateur avec un verrou pessimiste en mode écriture
    // Cela garantit que l'utilisateur est verrouillé pour les autres transactions jusqu'à ce que la transaction
    // courante soit terminée
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
