package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePack {

    //==============================
    //       DEPENDANCES
    //==============================
    private final RepositoryPackTemplate repositoryPackTemplate;
    private final RepositoryCard repositoryCard;
    private final MapperCard mapperCard;
    private final ServiceUser serviceUser;

    // Réduction du dropRate appliquée à chaque carte non-unique tirée
    private static final double DROP_RATE_REDUCTION = 0.05;
    // Plancher minimum pour qu'une carte reste dans le pool
    private static final double DROP_RATE_FLOOR = 0.01;


    //==============================
    //       PACK METHODS
    //==============================

    /**
     * Génère un pack pour un utilisateur à partir d'un template.
     * Pour chaque slot :
     *   1. Détermine la rareté (fixe ou aléatoire pondérée)
     *   2. Tire une carte du pool via dropRate pondéré
     *   3. Réduit légèrement le dropRate si la carte n'est pas unique
     * Puis assigne toutes les cartes à l'utilisateur via openPack.
     */
    @Transactional
    public List<CardDto> generatePack(Long userId, Long templateId) {
        PackTemplate template = repositoryPackTemplate.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("PackTemplate not found with id: " + templateId));

        List<Card> selectedCards = new ArrayList<>();

        for (PackSlot slot : template.getSlots()) {
            Rarity rarity = determineRarity(slot);

            // Exclure les cartes déjà tirées dans ce pack
            List<Long> alreadyPickedIds = selectedCards.stream().map(Card::getId).toList();
            Card card = pickCardFromPool(rarity, alreadyPickedIds);

            // Réduire légèrement le dropRate pour les cartes non-uniques
            if (!card.isUniqueCard()) {
                double reduced = Math.max(DROP_RATE_FLOOR, card.getDropRate() * (1 - DROP_RATE_REDUCTION));
                card.setDropRate(reduced);
                log.debug("Reduced dropRate of card '{}' to {}", card.getName(), reduced);
            }

            selectedCards.add(card);
        }

        // Persiste les dropRates mis à jour avant d'assigner les cartes
        repositoryCard.saveAll(selectedCards);

        List<Long> cardIds = selectedCards.stream().map(Card::getId).toList();
        log.info("Generated pack '{}' for user id={} : {} card(s)", template.getName(), userId, cardIds.size());
        return serviceUser.openPack(userId, cardIds);
    }


    //==============================
    //       PRIVATE METHODS
    //==============================

    /**
     * Détermine la rareté d'un slot :
     * - Si fixedRarity est défini, retourne directement cette rareté.
     * - Sinon, tire aléatoirement dans rarityWeights (tirage pondéré).
     */
    private Rarity determineRarity(PackSlot slot) {
        if (slot.getFixedRarity() != null) {
            return slot.getFixedRarity();
        }

        Map<Rarity, Double> weights = slot.getRarityWeights();
        if (weights.isEmpty()) {
            throw new IllegalStateException("PackSlot id=" + slot.getId() + " has no fixedRarity and no rarityWeights");
        }

        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll  = ThreadLocalRandom.current().nextDouble() * total;

        double cumulative = 0;
        for (Map.Entry<Rarity, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        // Fallback (float precision edge case)
        return weights.keySet().iterator().next();
    }

    /**
     * Tire une carte aléatoire du pool pour la rareté donnée,
     * en excluant les cartes déjà sélectionnées dans ce pack.
     * La sélection est pondérée par le dropRate de chaque carte.
     */
    private Card pickCardFromPool(Rarity rarity, List<Long> excludedIds) {
        List<Card> pool = excludedIds.isEmpty()
                ? repositoryCard.findByRarityAndUserIsNull(rarity)
                : repositoryCard.findByRarityAndUserIsNullAndIdNotIn(rarity, excludedIds);

        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + rarity + " card available in the pool");
        }

        double total = pool.stream().mapToDouble(Card::getDropRate).sum();
        double roll  = ThreadLocalRandom.current().nextDouble() * total;

        double cumulative = 0;
        for (Card card : pool) {
            cumulative += card.getDropRate();
            if (roll <= cumulative) {
                return card;
            }
        }
        // Fallback (float precision edge case)
        return pool.getLast();
    }
}
