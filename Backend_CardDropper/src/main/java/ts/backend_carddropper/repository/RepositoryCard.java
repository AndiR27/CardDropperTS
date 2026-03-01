package ts.backend_carddropper.repository;

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

    // Trouver toutes les cartes du pool (sans owner) par rareté — utilisé pour la fusion
    List<Card> findByRarityAndUserIsNull(Rarity rarity);

    // Trouver les cartes du pool par rareté en excluant celles déjà sélectionnées — utilisé pour la génération de pack
    List<Card> findByRarityAndUserIsNullAndIdNotIn(Rarity rarity, List<Long> excludedIds);

    // Trouver toutes les cartes possédées par un utilisateur
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId")
    List<Card> findAllByUserId(@Param("userId") Long userId);

    // Trouver toutes les cartes créées par un utilisateur
    @Query("SELECT c FROM Card c WHERE c.createdBy.id = :userId")
    List<Card> findAllByCreatedById(@Param("userId") Long userId);

    // Trouver toutes les cartes ciblant un utilisateur
    @Query("SELECT c FROM Card c WHERE c.targetUser.id = :userId")
    List<Card> findAllByTargetUserId(@Param("userId") Long userId);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Card c SET c.dropRate = :dropRate WHERE c.rarity = :rarity AND c.user IS NULL")
    int updateDropRateByRarityForPoolCards(@Param("rarity") Rarity rarity, @Param("dropRate") double dropRate);
}
