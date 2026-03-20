package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.event.UseCardEvent;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceUser {

    //==============================
    //       DEPENDANCES
    //==============================
    private final MapperUser mapperUser;
    private final MapperCard mapperCard;
    private final RepositoryUser repositoryUser;
    private final RepositoryCard repositoryCard;
    private final RepositoryUserCard repositoryUserCard;
    private final ApplicationEventPublisher eventPublisher;

    private static int mergeRequiredCount(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 3;
            case RARE   -> 4;
            case EPIC   -> 5;
            case LEGENDARY -> throw new IllegalArgumentException("LEGENDARY cards cannot be merged further");
        };
    }


    //==============================
    //    CRUD UTILISATEUR
    //==============================

    public Optional<UserDto> findById(Long id) {
        return repositoryUser.findById(id)
                .map(mapperUser::toDto);
    }

    public List<UserDto> findAll() {
        return repositoryUser.findAll()
                .stream()
                .map(mapperUser::toDto)
                .toList();
    }

    public Optional<UserDto> findByUsername(String username) {
        return repositoryUser.findByUsername(username)
                .map(mapperUser::toDto);
    }

    public Optional<UserDto> findByEmail(String email) {
        return repositoryUser.findByEmail(email)
                .map(mapperUser::toDto);
    }

    @Transactional
    public UserDto create(UserDto userDto) {
        if (repositoryUser.existsByUsername(userDto.username())) {
            throw new IllegalArgumentException("Username already taken: " + userDto.username());
        }
        if (repositoryUser.existsByEmail(userDto.email())) {
            throw new IllegalArgumentException("Email already in use: " + userDto.email());
        }
        User saved = repositoryUser.save(mapperUser.toEntity(userDto));
        log.info("Created user '{}' with id: {}", saved.getUsername(), saved.getId());
        return mapperUser.toDto(saved);
    }

    @Transactional
    public Optional<UserDto> update(Long id, UserDto userDto) {
        Optional<User> userOpt = repositoryUser.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        mapperUser.updateEntity(userDto, user);
        User updated = repositoryUser.save(user);
        log.info("Updated user id={} ('{}')", updated.getId(), updated.getUsername());
        return Optional.of(mapperUser.toDto(updated));
    }

    /**
     * Supprime un utilisateur après avoir détaché ses cartes.
     */
    @Transactional
    public void delete(Long id) {
        User user = repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        detachUserFromCards(user);
        repositoryUser.delete(user);
        log.info("Deleted user id={} ('{}')", id, user.getUsername());
    }


    //==============================
    //    GESTION DES CARTES
    //==============================

    /**
     * Retourne la liste des cartes possédées par un utilisateur.
     * Chaque copie est représentée comme une entrée distincte (compatible avec l'API existante).
     */
    public List<CardDto> getCardsOwned(Long userId) {
        findUserOrThrow(userId);
        return repositoryUserCard.findByUserId(userId).stream()
                .flatMap(uc -> Collections.nCopies(uc.getQuantity(), uc.getCard()).stream())
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Échange deux cartes entre deux utilisateurs.
     */
    @Transactional
    public void tradeCard(Long userId, Long myCardId, Long targetUserId, Long theirCardId) {
        User user       = findUserOrThrow(userId);
        User targetUser = findUserOrThrow(targetUserId);

        UserCard myUc    = findOwnedUserCardOrThrow(userId, myCardId);
        UserCard theirUc = findOwnedUserCardOrThrow(targetUserId, theirCardId);

        Card myCard    = myUc.getCard();
        Card theirCard = theirUc.getCard();

        removeUserCard(myUc);
        addCardToUser(targetUser, myCard);

        removeUserCard(theirUc);
        addCardToUser(user, theirCard);

        log.info("Trade: user '{}' gave card id={} and received card id={} from user '{}'",
                user.getUsername(), myCardId, theirCardId, targetUser.getUsername());
    }

    /**
     * Assigne une liste de cartes du pool à l'utilisateur lors de l'ouverture d'un paquet.
     * Les cartes uniques ne peuvent pas être dans le pool (vérification de sécurité).
     */
    @Transactional
    public List<CardDto> openPack(Long userId, List<Long> cardIds) {
        User user = findUserOrThrow(userId);

        List<Card> cards = repositoryCard.findAllById(cardIds);

        if (cards.size() != cardIds.size()) {
            throw new EntityNotFoundException("One or more cards not found");
        }

        for (Card card : cards) {
            addCardToUser(user, card);
        }

        log.info("User '{}' opened a pack and received {} card(s)", user.getUsername(), cards.size());
        return cards.stream().map(mapperCard::toDto).toList();
    }

    /**
     * Fusionne MERGE_REQUIRED_COUNT cartes de même rareté pour en créer une de rareté supérieure.
     * Supporte les IDs dupliqués (fusion de plusieurs copies de la même carte).
     *
     * Règles :
     *   - COMMON  x3 → RARE
     *   - RARE    x3 → EPIC
     *   - EPIC    x3 → LEGENDARY
     *   - LEGENDARY : fusion impossible
     */
    @Transactional
    public CardDto mergeCards(Long userId, List<Long> cardIds) {
        User user = findUserOrThrow(userId);

        // Compte les occurrences de chaque ID (supporte la fusion de copies identiques)
        Map<Long, Integer> idCounts = new LinkedHashMap<>();
        for (Long id : cardIds) {
            idCounts.merge(id, 1, Integer::sum);
        }

        List<Card> distinctCards = repositoryCard.findAllById(idCounts.keySet().stream().toList());
        if (distinctCards.size() != idCounts.size()) {
            throw new EntityNotFoundException("One or more cards not found");
        }

        // Validation : toutes les cartes ont la même rareté
        Rarity rarity = distinctCards.getFirst().getRarity();
        boolean allSameRarity = distinctCards.stream().allMatch(c -> c.getRarity() == rarity);
        if (!allSameRarity) {
            throw new IllegalArgumentException("All cards must have the same rarity to merge");
        }

        int required = mergeRequiredCount(rarity);
        if (cardIds.size() != required) {
            throw new IllegalArgumentException("Exactly " + required + " " + rarity + " cards are required to merge");
        }

        // Validation : l'utilisateur possède toutes les cartes en quantité suffisante
        for (Card card : distinctCards) {
            int needed = idCounts.get(card.getId());
            UserCard uc = repositoryUserCard.findByUserIdAndCardId(userId, card.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User does not own card id=" + card.getId()));
            if (uc.getQuantity() < needed) {
                throw new IllegalArgumentException(
                        "User does not have enough copies of card id=" + card.getId()
                        + " (has " + uc.getQuantity() + ", needs " + needed + ")");
            }
        }

        Rarity nextRarity = getNextRarity(rarity);

        // Piocher une carte aléatoire du pool à la rareté supérieure
        List<Card> pool = repositoryCard.findPoolCardsByRarity(nextRarity);
        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + nextRarity + " card available in the pool");
        }
        Card result = pool.get(new Random().nextInt(pool.size()));

        // Déduire les cartes consommées
        for (Card card : distinctCards) {
            int needed = idCounts.get(card.getId());
            UserCard uc = repositoryUserCard.findByUserIdAndCardId(userId, card.getId()).orElseThrow();
            int newQty = uc.getQuantity() - needed;
            if (newQty == 0) {
                repositoryUserCard.delete(uc);
            } else {
                uc.setQuantity(newQty);
                repositoryUserCard.save(uc);
            }
        }

        // Attribuer la carte résultante
        addCardToUser(user, result);

        log.info("User '{}' merged {} {} card(s) → received '{}' ({})",
                user.getUsername(), required, rarity, result.getName(), nextRarity);
        return mapperCard.toDto(result);
    }


    //==============================
    //    UTILISER UNE CARTE
    //==============================

    /**
     * Utilise une carte sur un autre utilisateur.
     * La carte est toujours retirée de la collection du propriétaire après utilisation.
     * Publie un UseCardEvent pour le live feed.
     */
    @Transactional
    public void useCard(Long userId, Long cardId, Long targetUserId) {
        User user = findUserOrThrow(userId);

        UserCard uc = findOwnedUserCardOrThrow(userId, cardId);
        Card card = uc.getCard();

        // Si la carte a une cible définie, vérifier qu'elle correspond
        if (card.getTargetUser() != null && !card.getTargetUser().getId().equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "Card id=" + cardId + " targets user id=" + card.getTargetUser().getId()
                            + ", not user id=" + targetUserId);
        }

        User target = findUserOrThrow(targetUserId);

        removeUserCard(uc);

        log.info("User '{}' used card '{}' ({}) on user '{}' (unique={})",
                user.getUsername(), card.getName(), card.getRarity(), target.getUsername(), card.isUniqueCard());

        eventPublisher.publishEvent(new UseCardEvent(
                this,
                user.getUsername(),
                card.getName(),
                card.getRarity().name(),
                target.getUsername()));
    }


    //==============================
    //       MÉTHODES PRIVÉES
    //==============================

    private User findUserOrThrow(Long id) {
        return repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    /**
     * Cherche le UserCard pour (userId, cardId) ou lève une exception.
     */
    private UserCard findOwnedUserCardOrThrow(Long userId, Long cardId) {
        return repositoryUserCard.findByUserIdAndCardId(userId, cardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User id=" + userId + " does not own card id=" + cardId));
    }

    /**
     * Ajoute une copie d'une carte à l'utilisateur :
     * - Si déjà présente : incrémente la quantité.
     * - Sinon : crée un nouveau UserCard avec quantity=1.
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
     * Retire une copie de la carte représentée par le UserCard :
     * - Si quantity > 1 : décrémente.
     * - Si quantity == 1 : supprime le UserCard.
     */
    private void removeUserCard(UserCard uc) {
        if (uc.getQuantity() > 1) {
            uc.setQuantity(uc.getQuantity() - 1);
            repositoryUserCard.save(uc);
        } else {
            repositoryUserCard.delete(uc);
        }
    }

    private Rarity getNextRarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON    -> Rarity.RARE;
            case RARE      -> Rarity.EPIC;
            case EPIC      -> Rarity.LEGENDARY;
            case LEGENDARY -> throw new IllegalArgumentException("LEGENDARY cards cannot be merged further");
        };
    }

    /**
     * Détache l'utilisateur de toutes les tables de jointure avant suppression.
     */
    private void detachUserFromCards(User user) {
        repositoryUserCard.deleteByUserId(user.getId());
        for (Card card : repositoryCard.findAllByCreatedById(user.getId())) {
            card.setCreatedBy(null);
        }
        for (Card card : repositoryCard.findAllByTargetUserId(user.getId())) {
            card.setTargetUser(null);
        }
        repositoryCard.flush();
    }
}
