package ts.backend_carddropper.reroll.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.reroll.entity.RerollCooldown;
import ts.backend_carddropper.reroll.entity.UserReroll;
import ts.backend_carddropper.reroll.mapping.MapperReroll;
import ts.backend_carddropper.reroll.models.RerollCooldownDto;
import ts.backend_carddropper.reroll.models.RerollResponse;
import ts.backend_carddropper.reroll.repository.RepositoryReroll;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceReroll {

    //==============================
    //       DEPENDANCES
    //==============================
    private final RepositoryReroll repositoryReroll;
    private final RepositoryUser repositoryUser;
    private final RepositoryCard repositoryCard;
    private final RepositoryUserCard repositoryUserCard;
    private final MapperReroll mapperReroll;
    private final MapperCard mapperCard;

    private static final double OWNED_PENALTY = 0.5;


    //==============================
    //       COOLDOWNS PAR RARETÉ
    //==============================

    public static Duration getCooldownDuration(Rarity rarity) {
        return switch (rarity) {
            case COMMON    -> Duration.ofDays(1);
            case RARE      -> Duration.ofDays(3);
            case EPIC      -> Duration.ofDays(10);
            case LEGENDARY -> Duration.ofDays(14);
        };
    }


    //==============================
    //       REROLL
    //==============================

    /**
     * Reroll une carte en une autre de même rareté.
     * La carte sacrifiée est retirée, une nouvelle est piochée dans le pool.
     */
    @Transactional
    public RerollResponse reroll(String keycloakId, Long cardId) {
        User user = findUser(keycloakId);

        // Vérifier que l'utilisateur possède la carte
        UserCard userCard = repositoryUserCard.findByUserIdAndCardId(user.getId(), cardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User does not own card id=" + cardId));

        Card sacrificedCard = userCard.getCard();
        Rarity rarity = sacrificedCard.getRarity();

        // Vérifier le cooldown
        UserReroll userReroll = repositoryReroll.findByUserId(user.getId())
                .orElseGet(() -> repositoryReroll.save(new UserReroll(user)));

        checkCooldown(userReroll, rarity);

        // Construire le pool (même rareté, exclut la carte sacrifiée)
        List<Card> pool = repositoryCard.findPoolCardsByRarityExcluding(
                rarity, user.getId(), List.of(cardId));

        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + rarity + " card available in the pool for reroll");
        }

        // Sélection pondérée
        Set<Long> ownedCardIds = repositoryUserCard.findByUserId(user.getId()).stream()
                .map(uc -> uc.getCard().getId())
                .collect(Collectors.toSet());

        Card receivedCard = pickWeightedCard(pool, ownedCardIds);

        // Retirer la carte sacrifiée
        removeUserCard(userCard);

        // Ajouter la nouvelle carte
        addCardToUser(user, receivedCard);

        // Mettre à jour le cooldown
        updateCooldown(userReroll, rarity);

        log.info("Reroll: user '{}' sacrificed '{}' ({}) → received '{}' ({})",
                user.getUsername(), sacrificedCard.getName(), rarity,
                receivedCard.getName(), rarity);

        return new RerollResponse(
                mapperCard.toDto(sacrificedCard),
                mapperCard.toDto(receivedCard));
    }


    //==============================
    //       CONSULTER LES COOLDOWNS
    //==============================

    /**
     * Retourne les cooldowns actuels de l'utilisateur pour chaque rareté.
     */
    public List<RerollCooldownDto> getCooldowns(String keycloakId) {
        User user = findUser(keycloakId);

        return repositoryReroll.findByUserId(user.getId())
                .map(ur -> ur.getCooldowns().stream()
                        .map(mapperReroll::toDto)
                        .toList())
                .orElse(List.of());
    }


    //==============================
    //       MÉTHODES PRIVÉES
    //==============================

    private User findUser(String keycloakId) {
        return repositoryUser.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException("User not found for keycloakId: " + keycloakId));
    }

    /**
     * Vérifie que le cooldown pour la rareté est expiré.
     */
    private void checkCooldown(UserReroll userReroll, Rarity rarity) {
        userReroll.getCooldowns().stream()
                .filter(cd -> cd.getRarity() == rarity)
                .findFirst()
                .ifPresent(cd -> {
                    LocalDateTime availableAt = cd.getLastRerollAt().plus(getCooldownDuration(rarity));
                    if (LocalDateTime.now().isBefore(availableAt)) {
                        throw new IllegalStateException(
                                "Reroll for " + rarity + " is on cooldown until " + availableAt);
                    }
                });
    }

    /**
     * Met à jour ou crée le cooldown pour la rareté donnée.
     */
    private void updateCooldown(UserReroll userReroll, Rarity rarity) {
        RerollCooldown cooldown = userReroll.getCooldowns().stream()
                .filter(cd -> cd.getRarity() == rarity)
                .findFirst()
                .orElseGet(() -> {
                    RerollCooldown newCd = new RerollCooldown(userReroll, rarity, LocalDateTime.now());
                    userReroll.getCooldowns().add(newCd);
                    return newCd;
                });
        cooldown.setLastRerollAt(LocalDateTime.now());
        repositoryReroll.save(userReroll);
    }

    /**
     * Sélection pondérée identique au pack opening :
     * poids = 1/(1+owners), pénalité si déjà possédée.
     */
    private Card pickWeightedCard(List<Card> pool, Set<Long> ownedCardIds) {
        double total = pool.stream().mapToDouble(c -> cardWeight(c, ownedCardIds)).sum();
        double roll = ThreadLocalRandom.current().nextDouble() * total;

        double cumulative = 0;
        for (Card card : pool) {
            cumulative += cardWeight(card, ownedCardIds);
            if (roll <= cumulative) {
                return card;
            }
        }
        return pool.getLast();
    }

    private double cardWeight(Card card, Set<Long> ownedCardIds) {
        double weight = 1.0 / (1 + card.getUserCards().size());
        if (ownedCardIds.contains(card.getId())) {
            weight *= OWNED_PENALTY;
        }
        return weight;
    }

    /**
     * Ajoute une copie d'une carte à l'utilisateur.
     */
    private void addCardToUser(User user, Card card) {
        repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId())
                .ifPresentOrElse(
                        uc -> {
                            uc.setQuantity(uc.getQuantity() + 1);
                            repositoryUserCard.save(uc);
                        },
                        () -> repositoryUserCard.save(new UserCard(user, card, 1))
                );
    }

    /**
     * Retire une copie de la carte :
     * - quantity > 1 → décrémente
     * - quantity == 1 → supprime le UserCard
     */
    private void removeUserCard(UserCard uc) {
        if (uc.getQuantity() > 1) {
            uc.setQuantity(uc.getQuantity() - 1);
            repositoryUserCard.save(uc);
        } else {
            repositoryUserCard.delete(uc);
        }
    }
}
