package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.event.UseCardEvent;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;

import java.util.List;
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
    private final ApplicationEventPublisher eventPublisher;

    // Nombre de cartes requises pour une fusion
    private static final int MERGE_REQUIRED_COUNT = 3;


    //==============================
    //    CRUD UTILISATEUR
    //==============================

    /**
     * Trouver un utilisateur par son id
     */
    public Optional<UserDto> findById(Long id) {
        return repositoryUser.findById(id)
                .map(mapperUser::toDto);
    }

    /**
     * Trouver tous les utilisateurs
     */
    public List<UserDto> findAll() {
        return repositoryUser.findAll()
                .stream()
                .map(mapperUser::toDto)
                .toList();
    }

    /**
     * Trouver un utilisateur par son username
     */
    public Optional<UserDto> findByUsername(String username) {
        return repositoryUser.findByUsername(username)
                .map(mapperUser::toDto);
    }

    /**
     * Trouver un utilisateur par son email
     */
    public Optional<UserDto> findByEmail(String email) {
        return repositoryUser.findByEmail(email)
                .map(mapperUser::toDto);
    }

    /**
     * Créer un utilisateur
     */
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

    /**
     * Mettre à jour un utilisateur
     */
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
     * Supprimer un utilisateur.
     * Avant la suppression : détache les cartes liées (owner, creator, target)
     * pour éviter les violations de contraintes FK.
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
     */
    public List<CardDto> getCardsOwned(Long userId) {
        User user = findUserOrThrow(userId);
        return user.getCardsOwned()
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Échange deux cartes entre deux utilisateurs.
     * Chaque utilisateur doit posséder sa carte respective.
     */
    @Transactional
    public void tradeCard(Long userId, Long myCardId, Long targetUserId, Long theirCardId) {
        User user       = findUserOrThrow(userId);
        User targetUser = findUserOrThrow(targetUserId);

        Card myCard    = findOwnedCardOrThrow(myCardId, user);
        Card theirCard = findOwnedCardOrThrow(theirCardId, targetUser);

        // Swap ownership via join table
        user.getCardsOwned().remove(myCard);
        targetUser.getCardsOwned().add(myCard);

        targetUser.getCardsOwned().remove(theirCard);
        user.getCardsOwned().add(theirCard);

        repositoryUser.save(user);
        repositoryUser.save(targetUser);

        log.info("Trade: user '{}' gave card id={} and received card id={} from user '{}'",
                user.getUsername(), myCardId, theirCardId, targetUser.getUsername());
    }

    /**
     * Assigne une liste de cartes du pool à l'utilisateur lors de l'ouverture d'un paquet.
     * Les cartes uniques doivent ne pas encore être possédées.
     * Les cartes sont ajoutées à la collection de l'utilisateur.
     */
    @Transactional
    public List<CardDto> openPack(Long userId, List<Long> cardIds) {
        User user = findUserOrThrow(userId);

        List<Card> cards = repositoryCard.findAllById(cardIds);

        if (cards.size() != cardIds.size()) {
            throw new EntityNotFoundException("One or more cards not found");
        }

        for (Card card : cards) {
            if (card.isUniqueCard() && !card.getOwners().isEmpty()) {
                throw new IllegalStateException("Card id=" + card.getId() + " is unique and already owned");
            }
            user.getCardsOwned().add(card);
        }

        repositoryUser.save(user);
        log.info("User '{}' opened a pack and received {} card(s)", user.getUsername(), cards.size());
        return cards.stream().map(mapperCard::toDto).toList();
    }

    /**
     * Fusionne plusieurs cartes de même rareté pour en créer une de rareté supérieure.
     * Règles :
     *   - COMMON  x3 → RARE
     *   - RARE    x3 → EPIC
     *   - EPIC    x3 → LEGENDARY
     *   - LEGENDARY : fusion impossible
     *
     * Les cartes consommées sont retirées de la collection de l'utilisateur.
     * La carte résultante est tirée aléatoirement dans le pool de la rareté supérieure.
     */
    @Transactional
    public CardDto mergeCards(Long userId, List<Long> cardIds) {
        User user = findUserOrThrow(userId);

        if (cardIds.size() != MERGE_REQUIRED_COUNT) {
            throw new IllegalArgumentException("Exactly " + MERGE_REQUIRED_COUNT + " cards are required to merge");
        }

        List<Card> cards = repositoryCard.findAllById(cardIds);

        if (cards.size() != MERGE_REQUIRED_COUNT) {
            throw new EntityNotFoundException("One or more cards not found");
        }

        // Validation : l'utilisateur possède toutes les cartes
        for (Card card : cards) {
            if (!user.getCardsOwned().contains(card)) {
                throw new IllegalArgumentException("User does not own card id=" + card.getId());
            }
        }

        // Validation : toutes les cartes ont la même rareté
        Rarity rarity = cards.getFirst().getRarity();
        boolean allSameRarity = cards.stream().allMatch(c -> c.getRarity() == rarity);
        if (!allSameRarity) {
            throw new IllegalArgumentException("All cards must have the same rarity to merge");
        }

        Rarity nextRarity = getNextRarity(rarity);

        // Piocher une carte aléatoire du pool à la rareté supérieure
        List<Card> pool = repositoryCard.findPoolCardsByRarity(nextRarity);
        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + nextRarity + " card available in the pool");
        }
        Card result = pool.get(new Random().nextInt(pool.size()));

        // Retirer les cartes consommées de la collection de l'utilisateur
        cards.forEach(c -> user.getCardsOwned().remove(c));

        // Attribuer la carte résultante à l'utilisateur et mettre à jour les relations
        //ser.getCardsOwned().add(result);
        result.addOwner(user);

        repositoryUser.save(user);

        log.info("User '{}' merged {} {} card(s) → received '{}' ({})",
                user.getUsername(), MERGE_REQUIRED_COUNT, rarity, result.getName(), nextRarity);
        return mapperCard.toDto(result);
    }


    //==============================
    //    UTILISER UNE CARTE
    //==============================

    /**
     * Utilise une carte sur un autre utilisateur.
     * - Carte unique : retirée de la collection du propriétaire.
     * - Carte non-unique : le propriétaire la conserve.
     * Publie un UseCardEvent pour le live feed.
     */
    @Transactional
    public void useCard(Long userId, Long cardId, Long targetUserId) {
        User user = findUserOrThrow(userId);
        Card card = findOwnedCardOrThrow(cardId, user);
        User target = findUserOrThrow(targetUserId);

        // Si la carte a une cible définie, vérifier qu'elle correspond
        if (card.getTargetUser() != null && !card.getTargetUser().getId().equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "Card id=" + cardId + " targets user id=" + card.getTargetUser().getId()
                            + ", not user id=" + targetUserId);
        }

        // Carte unique → consommée (retirée de la collection)
        // Carte non-unique → le propriétaire la conserve
        if (card.isUniqueCard()) {
            user.getCardsOwned().remove(card);
            repositoryUser.save(user);
        }

        log.info("User '{}' used card '{}' ({}) on user '{}' (unique={})",
                user.getUsername(), card.getName(), card.getRarity(), target.getUsername(), card.isUniqueCard());

        // Publier l'événement pour le live feed
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

    /**
     * Retourne l'utilisateur ou lève une EntityNotFoundException.
     */
    private User findUserOrThrow(Long id) {
        return repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    /**
     * Vérifie qu'une carte existe et est bien possédée par l'utilisateur donné.
     */
    private Card findOwnedCardOrThrow(Long cardId, User user) {
        return user.getCardsOwned().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User id=" + user.getId() + " does not own card id=" + cardId));
    }

    /**
     * Retourne la rareté supérieure à celle passée en paramètre.
     */
    private Rarity getNextRarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON     -> Rarity.RARE;
            case RARE       -> Rarity.EPIC;
            case EPIC       -> Rarity.LEGENDARY;
            case LEGENDARY -> throw new IllegalArgumentException("LEGENDARY cards cannot be merged further");
        };
    }

    /**
     * Retire l'utilisateur de toutes les relations dans les tables de jointure
     * avant de le supprimer, pour éviter les violations de contraintes.
     */
    private void detachUserFromCards(User user) {
        // Clear ManyToMany ownership
        user.getCardsOwned().clear();
        // Detach creator and target FK references
        for (Card card : repositoryCard.findAllByCreatedById(user.getId())) {
            card.setCreatedBy(null);
        }
        for (Card card : repositoryCard.findAllByTargetUserId(user.getId())) {
            card.setTargetUser(null);
        }
        repositoryCard.flush();
    }
}
