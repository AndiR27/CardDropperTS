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
        findUserOrThrow(userId); // Vérifie que l'utilisateur existe
        return repositoryCard.findAllByUserId(userId)
                .stream()
                .map(mapperCard::toDto)
                .toList();
    }

    /**
     * Crée une nouvelle carte et l'ajoute au pool global.
     * L'utilisateur en est le créateur mais n'en est pas propriétaire.
     * Il peut optionnellement se cibler lui-même (targetUser = lui-même uniquement).
     */
    @Transactional
    public CardDto createCard(Long userId, CardDto cardDto) {
        User creator = findUserOrThrow(userId);

        if (cardDto.targetUserId() != null && !cardDto.targetUserId().equals(userId)) {
            throw new IllegalArgumentException("targetUser must be the card creator itself");
        }

        Card card = mapperCard.toEntity(cardDto);
        card.setCreatedBy(creator);
        card.setUser(null); // pas de propriétaire : la carte entre dans le pool

        if (cardDto.targetUserId() != null) {
            card.setTargetUser(creator);
        }

        Card saved = repositoryCard.save(card);
        log.info("User '{}' created card '{}' (rarity={}) — added to pool", creator.getUsername(), saved.getName(), saved.getRarity());
        return mapperCard.toDto(saved);
    }

    /**
     * Échange deux cartes entre deux utilisateurs.
     * Chaque utilisateur doit posséder sa carte respective.
     */
    @Transactional
    public void tradeCard(Long userId, Long myCardId, Long targetUserId, Long theirCardId) {
        User user       = findUserOrThrow(userId);
        User targetUser = findUserOrThrow(targetUserId);

        Card myCard    = findOwnedCardOrThrow(myCardId, userId);
        Card theirCard = findOwnedCardOrThrow(theirCardId, targetUserId);

        myCard.setUser(targetUser);
        theirCard.setUser(user);

        repositoryCard.save(myCard);
        repositoryCard.save(theirCard);

        log.info("Trade: user '{}' gave card id={} and received card id={} from user '{}'",
                user.getUsername(), myCardId, theirCardId, targetUser.getUsername());
    }

    /**
     * Assigne une liste de cartes du pool à l'utilisateur lors de l'ouverture d'un paquet.
     * Les cartes doivent être disponibles dans le pool (pas encore possédées).
     * Les cartes sont mises à jour pour avoir l'utilisateur comme propriétaire.
     */
    @Transactional
    public List<CardDto> openPack(Long userId, List<Long> cardIds) {
        User user = findUserOrThrow(userId);

        List<Card> cards = repositoryCard.findAllById(cardIds);

        if (cards.size() != cardIds.size()) {
            throw new EntityNotFoundException("One or more cards not found");
        }

        for (Card card : cards) {
            if (card.getUser() != null) {
                throw new IllegalStateException("Card id=" + card.getId() + " is already owned and not in the pool");
            }
            card.setUser(user);
        }
        user.addCardsOwned(cards);

        List<Card> saved = repositoryCard.saveAll(cards);
        log.info("User '{}' opened a pack and received {} card(s)", user.getUsername(), saved.size());
        return saved.stream().map(mapperCard::toDto).toList();
    }

    /**
     * Fusionne plusieurs cartes de même rareté pour en créer une de rareté supérieure.
     * Règles :
     *   - COMMON  x3 → RARE
     *   - RARE    x3 → EPIC
     *   - EPIC    x3 → LEGENDARY
     *   - LEGENDARY : fusion impossible
     *
     * Les cartes consommées sont retournées dans le pool (user = null).
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
            if (card.getUser() == null || !card.getUser().getId().equals(userId)) {
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
        List<Card> pool = repositoryCard.findByRarityAndUserIsNull(nextRarity);
        if (pool.isEmpty()) {
            throw new IllegalStateException("No " + nextRarity + " card available in the pool");
        }
        Card result = pool.get(new Random().nextInt(pool.size()));

        // Retourner les cartes consommées dans le pool
        cards.forEach(c -> c.setUser(null));
        repositoryCard.saveAll(cards);

        // Attribuer la carte résultante à l'utilisateur
        result.setUser(user);
        Card saved = repositoryCard.save(result);

        log.info("User '{}' merged {} {} card(s) → received '{}' ({})",
                user.getUsername(), MERGE_REQUIRED_COUNT, rarity, saved.getName(), nextRarity);
        return mapperCard.toDto(saved);
    }


    //==============================
    //    UTILISER UNE CARTE
    //==============================

    /**
     * Utilise une carte sur un autre utilisateur.
     * - Carte unique : retirée de la collection du propriétaire (retourne au pool).
     * - Carte non-unique : le propriétaire la conserve.
     * Publie un UseCardEvent pour le live feed.
     */
    @Transactional
    public void useCard(Long userId, Long cardId, Long targetUserId) {
        User user = findUserOrThrow(userId);
        Card card = findOwnedCardOrThrow(cardId, userId);
        User target = findUserOrThrow(targetUserId);

        // Si la carte a une cible définie, vérifier qu'elle correspond
        if (card.getTargetUser() != null && !card.getTargetUser().getId().equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "Card id=" + cardId + " targets user id=" + card.getTargetUser().getId()
                            + ", not user id=" + targetUserId);
        }

        // Carte unique → consommée (retourne au pool)
        // Carte non-unique → le propriétaire la conserve
        if (card.isUniqueCard()) {
            card.setUser(null);
            repositoryCard.save(card);
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
    private Card findOwnedCardOrThrow(Long cardId, Long userId) {
        Card card = repositoryCard.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        if (card.getUser() == null || !card.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User id=" + userId + " does not own card id=" + cardId);
        }
        return card;
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
     * Retire l'utilisateur de toutes les relations FK dans la table card
     * avant de le supprimer, pour éviter les violations de contraintes.
     */
    private void detachUserFromCards(User user) {
        for (Card card : repositoryCard.findAllByUserId(user.getId())) {
            card.setUser(null);
        }
        for (Card card : repositoryCard.findAllByCreatedById(user.getId())) {
            card.setCreatedBy(null);
        }
        for (Card card : repositoryCard.findAllByTargetUserId(user.getId())) {
            card.setTargetUser(null);
        }
        repositoryCard.flush();
    }
}
