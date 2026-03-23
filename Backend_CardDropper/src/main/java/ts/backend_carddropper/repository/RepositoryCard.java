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

    // Pool de cartes droppables :
    //  - actives uniquement
    //  - uniques sans aucun propriétaire (si quelqu'un la possède déjà, personne d'autre ne peut la drop)
    //  - les cartes ciblant l'utilisateur courant sont exclues (il ne peut pas dropper sa propre carte ciblée)
    @Query("SELECT c FROM Card c WHERE c.rarity = :rarity AND c.active = true AND (c.uniqueCard = false OR c.userCards IS EMPTY) AND (c.targetUser IS NULL OR c.targetUser.id != :userId)")
    List<Card> findPoolCardsByRarity(@Param("rarity") Rarity rarity, @Param("userId") Long userId);

    // Même pool en excluant une liste d'IDs (doublons déjà sélectionnés dans le même pack)
    @Query("SELECT c FROM Card c WHERE c.rarity = :rarity AND c.active = true AND (c.uniqueCard = false OR c.userCards IS EMPTY) AND (c.targetUser IS NULL OR c.targetUser.id != :userId) AND c.id NOT IN :excludedIds")
    List<Card> findPoolCardsByRarityExcluding(@Param("rarity") Rarity rarity, @Param("userId") Long userId, @Param("excludedIds") List<Long> excludedIds);

    //Trouver toutes les cartes possédées par un utilisateur
    @Query("SELECT c FROM Card c JOIN c.userCards uc WHERE uc.user.id = :userId")
    List<Card> findAllByOwnersId(@Param("userId") Long userId);

    // Trouver toutes les cartes créées par un utilisateur
    @Query("SELECT c FROM Card c WHERE c.createdBy.id = :userId")
    List<Card> findAllByCreatedById(@Param("userId") Long userId);

    // Trouver toutes les cartes ciblant un utilisateur
    @Query("SELECT c FROM Card c WHERE c.targetUser.id = :userId")
    List<Card> findAllByTargetUserId(@Param("userId") Long userId);

    // Pagination : toutes les cartes
    Page<Card> findAll(Pageable pageable);

    // Pagination : cartes par rareté
    Page<Card> findByRarity(Rarity rarity, Pageable pageable);

    boolean existsByName(String name);

    // Mettre à jour le drop rate de toutes les cartes d'une rareté donnée dans le pool (non-uniques + uniques sans propriétaire)
    @Modifying
    @Query("UPDATE Card c SET c.dropRate = :dropRate WHERE c.rarity = :rarity AND (c.uniqueCard = false OR c.userCards IS EMPTY)")
    int updateDropRateByRarityForPoolCards(@Param("rarity") Rarity rarity, @Param("dropRate") double dropRate);
}
