package ts.backend_carddropper.TestServices;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.reroll.entity.RerollCooldown;
import ts.backend_carddropper.reroll.entity.UserReroll;
import ts.backend_carddropper.reroll.models.RerollCooldownDto;
import ts.backend_carddropper.reroll.models.RerollResponse;
import ts.backend_carddropper.reroll.repository.RepositoryReroll;
import ts.backend_carddropper.reroll.service.ServiceReroll;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceReroll {

    @Autowired
    private ServiceReroll serviceReroll;

    @Autowired
    private TestDataHelper testDataHelper;

    @MockitoBean
    private RepositoryUser repositoryUser;

    @MockitoBean
    private RepositoryCard repositoryCard;

    @MockitoBean
    private RepositoryUserCard repositoryUserCard;

    @MockitoBean
    private RepositoryReroll repositoryReroll;

    private List<User> users;
    private List<Card> cards;
    private User alice;

    private static final String ALICE_KEYCLOAK_ID = "kc-alice";

    @BeforeEach
    void setUp() {
        users = testDataHelper.createUsers();
        alice = users.getFirst();
        alice.setKeycloakId(ALICE_KEYCLOAK_ID);
        cards = testDataHelper.createCards(alice);

        when(repositoryUser.findByKeycloakId(ALICE_KEYCLOAK_ID)).thenReturn(Optional.of(alice));
        when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repositoryReroll.save(any(UserReroll.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Helper: simule qu'un user possède une carte (quantity=1) */
    private UserCard giveCard(User user, Card card) {
        UserCard uc = new UserCard(user, card, 1);
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.of(uc));
        return uc;
    }

    /** Helper: simule qu'un user possède plusieurs copies d'une carte */
    private UserCard giveCards(User user, Card card, int quantity) {
        UserCard uc = new UserCard(user, card, quantity);
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.of(uc));
        return uc;
    }

    /** Helper: simule qu'un user ne possède PAS une carte */
    private void noCard(User user, Card card) {
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.empty());
    }

    /** Helper: crée un UserReroll sans cooldowns */
    private UserReroll createUserReroll(User user) {
        UserReroll ur = new UserReroll(user);
        ur.setId(1L);
        ur.setCooldowns(new ArrayList<>());
        when(repositoryReroll.findByUserId(user.getId())).thenReturn(Optional.of(ur));
        return ur;
    }

    /** Helper: crée un UserReroll avec un cooldown expiré pour la rareté */
    private UserReroll createUserRerollWithExpiredCooldown(User user, Rarity rarity) {
        UserReroll ur = createUserReroll(user);
        RerollCooldown cd = new RerollCooldown(ur, rarity,
                LocalDateTime.now().minus(ServiceReroll.getCooldownDuration(rarity)).minusHours(1));
        cd.setId(1L);
        ur.getCooldowns().add(cd);
        return ur;
    }

    /** Helper: crée un UserReroll avec un cooldown actif pour la rareté */
    private UserReroll createUserRerollWithActiveCooldown(User user, Rarity rarity) {
        UserReroll ur = createUserReroll(user);
        RerollCooldown cd = new RerollCooldown(ur, rarity, LocalDateTime.now());
        cd.setId(1L);
        ur.getCooldowns().add(cd);
        return ur;
    }


    // ========================================
    //              REROLL TESTS
    // ========================================

    @Nested
    @DisplayName("Reroll d'une carte")
    class RerollTests {

        @Test
        @DisplayName("reroll COMMON réussi : carte sacrifiée retirée, nouvelle carte ajoutée")
        void testReroll_common_success() {
            Card sacrificed = cards.get(0); // common_card_1
            Card poolCard = cards.get(1);   // common_card_2
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            // Pas de cooldown existant → crée un nouveau UserReroll
            when(repositoryReroll.findByUserId(alice.getId())).thenReturn(Optional.empty());

            // Pool contient une autre carte COMMON
            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            assertNotNull(response);
            assertEquals(sacrificed.getName(), response.sacrificedCard().name());
            assertEquals(poolCard.getName(), response.receivedCard().name());

            // Carte sacrifiée retirée (quantity=1 → delete)
            verify(repositoryUserCard).delete(any(UserCard.class));
            // Nouvelle carte ajoutée
            verify(repositoryUserCard).save(argThat(uc -> uc.getCard().getId().equals(poolCard.getId())));
            // Cooldown mis à jour
            verify(repositoryReroll, atLeastOnce()).save(any(UserReroll.class));
        }

        @Test
        @DisplayName("reroll RARE avec cooldown expiré : réussi")
        void testReroll_rare_expiredCooldown() {
            Card sacrificed = cards.get(12); // first RARE
            Card poolCard = cards.get(13);   // another RARE
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            createUserRerollWithExpiredCooldown(alice, Rarity.RARE);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.RARE), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            assertNotNull(response);
            assertEquals(Rarity.RARE, response.receivedCard().rarity());
        }

        @Test
        @DisplayName("reroll avec copies multiples : quantity décrémentée au lieu de supprimer")
        void testReroll_multipleCopiess_decrementsQuantity() {
            Card sacrificed = cards.get(0);
            Card poolCard = cards.get(1);
            giveCards(alice, sacrificed, 3);
            noCard(alice, poolCard);

            createUserReroll(alice);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            assertNotNull(response);
            // quantity > 1 → save au lieu de delete
            verify(repositoryUserCard, never()).delete(any(UserCard.class));
            // 2 saves: 1 for decrement + 1 for adding new card
            verify(repositoryUserCard, atLeast(2)).save(any(UserCard.class));
        }

        @Test
        @DisplayName("reroll LEGENDARY réussi avec cooldown expiré")
        void testReroll_legendary_success() {
            Card sacrificed = cards.get(22); // first LEGENDARY
            Card poolCard = cards.get(23);   // second LEGENDARY
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            createUserRerollWithExpiredCooldown(alice, Rarity.LEGENDARY);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.LEGENDARY), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            assertEquals(Rarity.LEGENDARY, response.receivedCard().rarity());
        }

        @Test
        @DisplayName("reroll exclut la carte sacrifiée du pool (ne peut jamais recevoir la même carte)")
        void testReroll_excludesSacrificedCardFromPool() {
            Card sacrificed = cards.get(0);
            Card poolCard = cards.get(1);
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            createUserReroll(alice);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            // Vérifie que le pool a bien été appelé avec l'exclusion de la carte sacrifiée
            verify(repositoryCard).findPoolCardsByRarityExcluding(
                    eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId())));
            // La carte reçue ne doit pas être la même
            assertNotEquals(sacrificed.getId(), response.receivedCard().id());
        }

        @Test
        @DisplayName("reroll carte unique : carte retirée mais reste active (retourne dans le pool)")
        void testReroll_uniqueCard_remainsActiveAfterSacrifice() {
            Card sacrificed = cards.get(0);
            sacrificed.setUniqueCard(true);
            sacrificed.setActive(true);

            Card poolCard = cards.get(1);
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            createUserReroll(alice);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());

            // Carte retirée de la collection
            verify(repositoryUserCard).delete(any(UserCard.class));
            // Carte unique reste active (pas de setActive(false) → retourne dans le pool)
            assertTrue(sacrificed.isActive());
            verify(repositoryCard, never()).save(any(Card.class));
        }
    }


    // ========================================
    //         TESTS D'ERREURS
    // ========================================

    @Nested
    @DisplayName("Erreurs de reroll")
    class RerollErrorTests {

        @Test
        @DisplayName("échoue quand l'utilisateur ne possède pas la carte")
        void testReroll_cardNotOwned() {
            Card card = cards.get(0);
            noCard(alice, card);

            assertThrows(IllegalArgumentException.class,
                    () -> serviceReroll.reroll(ALICE_KEYCLOAK_ID, card.getId()));
        }

        @Test
        @DisplayName("échoue quand le cooldown est actif")
        void testReroll_cooldownActive() {
            Card card = cards.get(0);
            giveCard(alice, card);

            createUserRerollWithActiveCooldown(alice, Rarity.COMMON);

            assertThrows(IllegalStateException.class,
                    () -> serviceReroll.reroll(ALICE_KEYCLOAK_ID, card.getId()));
        }

        @Test
        @DisplayName("échoue quand le pool est vide (aucune carte disponible)")
        void testReroll_emptyPool() {
            Card card = cards.get(0);
            giveCard(alice, card);

            createUserReroll(alice);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(card.getId()))))
                    .thenReturn(Collections.emptyList());

            assertThrows(IllegalStateException.class,
                    () -> serviceReroll.reroll(ALICE_KEYCLOAK_ID, card.getId()));
        }

        @Test
        @DisplayName("échoue quand l'utilisateur n'existe pas (keycloakId inconnu)")
        void testReroll_userNotFound() {
            when(repositoryUser.findByKeycloakId("unknown")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceReroll.reroll("unknown", 1L));
        }

        @Test
        @DisplayName("cooldown RARE actif ne bloque pas un reroll COMMON")
        void testReroll_cooldownOnDifferentRarity_doesNotBlock() {
            Card sacrificed = cards.get(0); // COMMON
            Card poolCard = cards.get(1);
            giveCard(alice, sacrificed);
            noCard(alice, poolCard);

            // Cooldown actif sur RARE, pas COMMON
            createUserRerollWithActiveCooldown(alice, Rarity.RARE);

            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.COMMON), eq(alice.getId()), eq(List.of(sacrificed.getId()))))
                    .thenReturn(List.of(poolCard));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            // Ne doit pas lever d'exception
            RerollResponse response = serviceReroll.reroll(ALICE_KEYCLOAK_ID, sacrificed.getId());
            assertNotNull(response);
        }
    }


    // ========================================
    //         COOLDOWNS TESTS
    // ========================================

    @Nested
    @DisplayName("Consultation des cooldowns")
    class CooldownTests {

        @Test
        @DisplayName("retourne une liste vide quand aucun reroll n'a été fait")
        void testGetCooldowns_empty() {
            when(repositoryReroll.findByUserId(alice.getId())).thenReturn(Optional.empty());

            List<RerollCooldownDto> result = serviceReroll.getCooldowns(ALICE_KEYCLOAK_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("retourne les cooldowns existants avec date d'expiration calculée")
        void testGetCooldowns_withEntries() {
            UserReroll ur = createUserReroll(alice);
            LocalDateTime now = LocalDateTime.now();
            RerollCooldown cd = new RerollCooldown(ur, Rarity.COMMON, now);
            cd.setId(1L);
            ur.getCooldowns().add(cd);

            List<RerollCooldownDto> result = serviceReroll.getCooldowns(ALICE_KEYCLOAK_ID);

            assertEquals(1, result.size());
            assertEquals(Rarity.COMMON, result.getFirst().rarity());
            assertNotNull(result.getFirst().availableAt());
            // availableAt doit être lastRerollAt + durée du cooldown (1 jour pour COMMON)
            assertEquals(now.plusDays(1).getMinute(), result.getFirst().availableAt().getMinute());
        }

        @Test
        @DisplayName("retourne plusieurs cooldowns pour différentes raretés")
        void testGetCooldowns_multipleRarities() {
            UserReroll ur = createUserReroll(alice);
            LocalDateTime now = LocalDateTime.now();

            RerollCooldown cdCommon = new RerollCooldown(ur, Rarity.COMMON, now);
            cdCommon.setId(1L);
            RerollCooldown cdRare = new RerollCooldown(ur, Rarity.RARE, now.minusDays(2));
            cdRare.setId(2L);

            ur.getCooldowns().add(cdCommon);
            ur.getCooldowns().add(cdRare);

            List<RerollCooldownDto> result = serviceReroll.getCooldowns(ALICE_KEYCLOAK_ID);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("échoue quand l'utilisateur n'existe pas")
        void testGetCooldowns_userNotFound() {
            when(repositoryUser.findByKeycloakId("unknown")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceReroll.getCooldowns("unknown"));
        }
    }
}
