package ts.backend_carddropper.helper;

import org.springframework.stereotype.Component;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TestDataHelper {

    /**
     * Creates 2 test users (alice, bob). Not persisted.
     */
    public List<User> createUsers() {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@test.com");
        alice.setCardsOwned(new ArrayList<>());
        alice.setCardsCreated(new ArrayList<>());
        alice.setCardsTargeting(new ArrayList<>());

        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setEmail("bob@test.com");
        bob.setCardsOwned(new ArrayList<>());
        bob.setCardsCreated(new ArrayList<>());
        bob.setCardsTargeting(new ArrayList<>());

        return List.of(alice, bob);
    }

    /**
     * Creates 24 pool cards across all rarities (no owner). Not persisted.
     * 12 COMMON (dropRate 1.0), 6 RARE (0.5), 4 EPIC (0.2), 2 LENGENDARY (0.1)
     */
    public List<Card> createCards(User creator) {
        List<Card> cards = new ArrayList<>();
        long id = 1L;

        id = addCards(cards, id, creator, Rarity.COMMON, 12, 1.0);
        id = addCards(cards, id, creator, Rarity.RARE, 6, 0.5);
        id = addCards(cards, id, creator, Rarity.EPIC, 4, 0.2);
        addCards(cards, id, creator, Rarity.LEGENDARY, 2, 0.1);

        return cards;
    }

    /**
     * Creates a pack template "Standard Pack" with 3 slots. Not persisted.
     * Slot 1: weighted random (COMMON:70, RARE:25, EPIC:4, LENGENDARY:1)
     * Slot 2: weighted random (COMMON:40, RARE:35, EPIC:20, LENGENDARY:5)
     * Slot 3: fixedRarity = RARE
     */
    public PackTemplate createPackTemplate() {
        PackTemplate template = new PackTemplate();
        template.setId(1L);
        template.setName("Standard Pack");

        PackSlot slot1 = new PackSlot();
        slot1.setId(1L);
        slot1.setPackTemplate(template);
        slot1.setRarityWeights(Map.of(
                Rarity.COMMON, 70.0,
                Rarity.RARE, 25.0,
                Rarity.EPIC, 4.0,
                Rarity.LEGENDARY, 1.0
        ));

        PackSlot slot2 = new PackSlot();
        slot2.setId(2L);
        slot2.setPackTemplate(template);
        slot2.setRarityWeights(Map.of(
                Rarity.COMMON, 40.0,
                Rarity.RARE, 35.0,
                Rarity.EPIC, 20.0,
                Rarity.LEGENDARY, 5.0
        ));

        PackSlot slot3 = new PackSlot();
        slot3.setId(3L);
        slot3.setPackTemplate(template);
        slot3.setFixedRarity(Rarity.RARE);

        template.setSlots(List.of(slot1, slot2, slot3));
        return template;
    }

    private long addCards(List<Card> cards, long startId, User creator,
                          Rarity rarity, int count, double dropRate) {
        for (int i = 1; i <= count; i++) {
            Card card = new Card();
            card.setId(startId);
            card.setName(rarity.name().toLowerCase() + "_card_" + i);
            card.setRarity(rarity);
            card.setDropRate(dropRate);
            card.setUniqueCard(false);
            card.setCreatedBy(creator);
            card.setUser(null); // pool card
            cards.add(card);
            startId++;
        }
        return startId;
    }
}
