package ts.backend_carddropper.TestServices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that verifies deleting a Card cascades correctly
 * and removes associated UserCard entries without FK violations.
 * Uses real H2 database — no mocks.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestCardDeleteCascade {

    @Autowired
    private RepositoryCard repositoryCard;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private RepositoryUserCard repositoryUserCard;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setKeycloakId("kc-" + username);
        user.setCardsCreated(new ArrayList<>());
        user.setCardsTargeting(new ArrayList<>());
        return repositoryUser.saveAndFlush(user);
    }

    private Card createCardWithOwners(String name, User creator, User... owners) {
        Card card = new Card();
        card.setName(name);
        card.setRarity(Rarity.COMMON);
        card.setDropRate(1.0);
        card.setActive(true);
        card.setCreatedBy(creator);
        Card saved = repositoryCard.saveAndFlush(card);

        for (User owner : owners) {
            UserCard uc = new UserCard(owner, saved, 1);
            repositoryUserCard.saveAndFlush(uc);
        }

        entityManager.clear();
        return saved;
    }

    @Test
    @DisplayName("Deleting a card owned by multiple users cascades UserCard removal")
    void deleteCard_cascadesUserCards() {
        User alice = createUser("alice_cascade");
        User bob = createUser("bob_cascade");
        Card card = createCardWithOwners("CascadeCard", alice, alice, bob);

        assertTrue(repositoryUserCard.existsByCardId(card.getId()));

        repositoryCard.deleteById(card.getId());
        repositoryCard.flush();

        assertFalse(repositoryCard.existsById(card.getId()));
        assertFalse(repositoryUserCard.existsByCardId(card.getId()));
    }

    @Test
    @DisplayName("Deleting a card with no owners works without error")
    void deleteCard_noOwners() {
        User alice = createUser("alice_noowner");
        Card card = createCardWithOwners("LonelyCard", alice);

        repositoryCard.deleteById(card.getId());
        repositoryCard.flush();

        assertFalse(repositoryCard.existsById(card.getId()));
    }

    @Test
    @DisplayName("Deleting a card does not affect other cards or their ownerships")
    void deleteCard_doesNotAffectOtherCards() {
        User alice = createUser("alice_other");
        Card card1 = createCardWithOwners("Card1", alice, alice);
        Card card2 = createCardWithOwners("Card2", alice, alice);

        repositoryCard.deleteById(card1.getId());
        repositoryCard.flush();

        assertFalse(repositoryCard.existsById(card1.getId()));
        assertTrue(repositoryCard.existsById(card2.getId()));
        assertFalse(repositoryUserCard.existsByCardId(card1.getId()));
        assertTrue(repositoryUserCard.existsByCardId(card2.getId()));
    }
}
