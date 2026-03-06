package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.PackTemplateSlot;
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


    //==============================
    //       PACK METHODS
    //==============================

    @Transactional
    public List<CardDto> generatePack(Long userId, Long templateId) {
        PackTemplate template = repositoryPackTemplate.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("PackTemplate not found with id: " + templateId));

        List<Card> selectedCards = new ArrayList<>();

        for (PackTemplateSlot templateSlot : template.getSlots()) {
            for (int i = 0; i < templateSlot.getCount(); i++) {
                Rarity rarity = determineRarity(templateSlot.getPackSlot());

                List<Long> alreadyPickedIds = selectedCards.stream().map(Card::getId).toList();
                Card card = pickCardFromPool(rarity, alreadyPickedIds);

                selectedCards.add(card);
            }
        }

        List<Long> cardIds = selectedCards.stream().map(Card::getId).toList();
        log.info("Generated pack '{}' for user id={} : {} card(s)", template.getName(), userId, cardIds.size());
        return serviceUser.openPack(userId, cardIds);
    }


    //==============================
    //       PRIVATE METHODS
    //==============================

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
        return weights.keySet().iterator().next();
    }

    private Card pickCardFromPool(Rarity rarity, List<Long> excludedIds) {
        List<Card> pool = excludedIds.isEmpty()
                ? repositoryCard.findPoolCardsByRarity(rarity)
                : repositoryCard.findPoolCardsByRarityExcluding(rarity, excludedIds);

        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + rarity + " card available in the pool");
        }

        double total = pool.stream().mapToDouble(this::cardWeight).sum();
        double roll  = ThreadLocalRandom.current().nextDouble() * total;

        double cumulative = 0;
        for (Card card : pool) {
            cumulative += cardWeight(card);
            if (roll <= cumulative) {
                return card;
            }
        }
        return pool.getLast();
    }

    private double cardWeight(Card card) {
        return 1.0 / (1 + card.getOwners().size());
    }
}
