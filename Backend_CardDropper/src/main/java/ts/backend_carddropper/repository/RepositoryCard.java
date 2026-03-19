package ts.backend_carddropper.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.enums.Rarity;

import java.util.List;

@Repository
public interface RepositoryCard extends JpaRepository<Card, Long> {

    // Trouver toutes les cartes par rareté
    List<Card> findByRarity(Rarity rarity);

    // Cartes disponibles dans le pool : non-uniques (toujours disponibles) + uniques sans propriétaire
    @Query("SELECT c FROM Card c WHERE c.rarity = :rarity AND (c.uniqueCard = false OR c.owners IS EMPTY)")
    List<Card> findPoolCardsByRarity(@Param("rarity") Rarity rarity);

    // Cartes du pool en excluant celles déjà sélectionnées — utilisé pour la génération de pack
    @Query("SELECT c FROM Card c WHERE c.rarity = :rarity AND (c.uniqueCard = false OR c.owners IS EMPTY) AND c.id NOT IN :excludedIds")
    List<Card> findPoolCardsByRarityExcluding(@Param("rarity") Rarity rarity, @Param("excludedIds") List<Long> excludedIds);

    //Trouver toutes les cartes possédées par un utilisateur
    @Query("SELECT c FROM Card c JOIN c.owners o WHERE o.id = :userId")
    List<Card> findAllByOwnersId(@Param("userId") Long userId);

    // Trouver toutes les cartes créées par un utilisateur
    @Query("SELECT c FROM Card c WHERE c.createdBy.id = :userId")
    List<Card> findAllByCreatedById(@Param("userId") Long userId);

    // Trouver toutes les cartes ciblant un utilisateur
    @Query("SELECT c FROM Card c WHERE c.targetUser.id = :userId")
    List<Card> findAllByTargetUserId(@Param("userId") Long userId);

    // Paginated — all cards
    Page<Card> findAll(Pageable pageable);

    // Paginated — by rarity
    Page<Card> findByRarity(Rarity rarity, Pageable pageable);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Card c SET c.dropRate = :dropRate WHERE c.rarity = :rarity AND (c.uniqueCard = false OR c.owners IS EMPTY)")
    int updateDropRateByRarityForPoolCards(@Param("rarity") Rarity rarity, @Param("dropRate") double dropRate);
}
