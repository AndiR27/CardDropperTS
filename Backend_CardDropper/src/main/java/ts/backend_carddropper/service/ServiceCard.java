package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCard {

    //==============================
    //       DEPENDANCES
    //==============================
    private final MapperCard mapperCard;
    private final RepositoryCard repositoryCard;
    private final RepositoryUser repositoryUser;


    //==============================
    //    CRUD CARD METHODS
    //==============================

    /**
     * Trouver une carte par son id
     */
    public Optional<CardDto> findById(Long id) {
        return repositoryCard.findById(id)
                .map(mapperCard::toDto);
    }

    /**
     * Trouver toutes les cartes
     */
    public List<CardDto> findAll() {
        return repositoryCard.findAll()
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Trouver toutes les cartes par rareté
     */
    public List<CardDto> findByRarity(Rarity rarity) {
        return repositoryCard.findByRarity(rarity)
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Trouver toutes les cartes possédées par un utilisateur
     */
    public List<CardDto> findAllByUserId(Long userId) {
        findUserOrThrow(userId);
        return repositoryCard.findAllByUserId(userId)
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Trouver toutes les cartes créées par un utilisateur
     */
    public List<CardDto> findAllByCreatedById(Long userId) {
        findUserOrThrow(userId);
        return repositoryCard.findAllByCreatedById(userId)
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Trouver toutes les cartes ciblant un utilisateur
     */
    public List<CardDto> findAllByTargetUserId(Long userId) {
        findUserOrThrow(userId);
        return repositoryCard.findAllByTargetUserId(userId)
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Créer une carte.
     * Règle : si un targetUser est fourni, il doit être le même que le créateur.
     */
    @Transactional
    public CardDto createCard(CardDto cardDto) {
        if (cardDto.targetUserId() != null && !cardDto.targetUserId().equals(cardDto.createdById())) {
            throw new IllegalArgumentException("targetUser must be the card creator itself");
        }
        Card card = mapperCard.toEntity(cardDto);
        applyUserRelations(cardDto, card);
        Card saved = repositoryCard.save(card);
        log.info("Created card '{}' (rarity={}) with id: {}", saved.getName(), saved.getRarity(), saved.getId());
        return mapperCard.toDto(saved);
    }

    /**
     * Mettre à jour une carte
     */
    @Transactional
    public Optional<CardDto> update(Long id, CardDto cardDto) {
        Optional<Card> cardOpt = repositoryCard.findById(id);
        if (cardOpt.isEmpty()) {
            return Optional.empty();
        }
        Card card = cardOpt.get();
        mapperCard.updateEntity(cardDto, card);
        applyUserRelations(cardDto, card);
        Card updated = repositoryCard.save(card);
        log.info("Updated card id={} ('{}')", updated.getId(), updated.getName());
        return Optional.of(mapperCard.toDto(updated));
    }

    /**
     * Transférer la propriété d'une carte à un autre utilisateur
     */
    @Transactional
    public CardDto transferOwnership(Long cardId, Long newOwnerId) {
        Card card = repositoryCard.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        card.setUser(findUserOrThrow(newOwnerId));
        Card updated = repositoryCard.save(card);
        log.info("Transferred card id={} to user id={}", cardId, newOwnerId);
        return mapperCard.toDto(updated);
    }

    /**
     * Supprimer une carte par son id
     */
    @Transactional
    public void delete(Long id) {
        if (!repositoryCard.existsById(id)) {
            throw new EntityNotFoundException("Card not found with id: " + id);
        }
        repositoryCard.deleteById(id);
        log.info("Deleted card with id: {}", id);
    }


    //==============================
    //       PRIVATE METHODS
    //==============================

    private User findUserOrThrow(Long id) {
        return repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    /**
     * Applique les relations User (owner, creator, target) sur une entité Card
     * à partir des IDs portés par le DTO.
     */
    private void applyUserRelations(CardDto dto, Card card) {
        if (dto.userId() != null)       card.setUser(findUserOrThrow(dto.userId()));
        if (dto.createdById() != null)  card.setCreatedBy(findUserOrThrow(dto.createdById()));
        if (dto.targetUserId() != null) card.setTargetUser(findUserOrThrow(dto.targetUserId()));
    }
}
