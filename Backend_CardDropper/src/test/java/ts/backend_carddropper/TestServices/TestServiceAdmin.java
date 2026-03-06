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
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.models.PackTemplateDto;
import ts.backend_carddropper.models.PackTemplateSlotDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackSlot;
import ts.backend_carddropper.repository.RepositoryPackTemplate;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.service.ServiceAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceAdmin {

    @Autowired
    private ServiceAdmin serviceAdmin;

    @Autowired
    private TestDataHelper testDataHelper;

    @MockitoBean
    private RepositoryPackTemplate repositoryPackTemplate;

    @MockitoBean
    private RepositoryCard repositoryCard;

    @MockitoBean
    private RepositoryUser repositoryUser;

    @MockitoBean
    private RepositoryPackSlot repositoryPackSlot;

    private PackTemplate template;
    private List<User> users;
    private List<PackSlot> packSlots;

    @BeforeEach
    void setUp() {
        template = testDataHelper.createPackTemplate();
        users = testDataHelper.createUsers();
        packSlots = testDataHelper.createPackSlots();
    }


    // ========================================
    //        PACK TEMPLATE — FIND
    // ========================================

    @Nested
    @DisplayName("PackTemplate find operations")
    class FindTemplateTests {

        @Test
        @DisplayName("findAllPackTemplates returns all templates")
        void testFindAll() {
            when(repositoryPackTemplate.findAll()).thenReturn(List.of(template));

            List<PackTemplateDto> result = serviceAdmin.findAllPackTemplates();

            assertEquals(1, result.size());
            assertEquals("Standard Pack", result.getFirst().name());
        }

        @Test
        @DisplayName("findAllPackTemplates returns empty list when none exist")
        void testFindAll_empty() {
            when(repositoryPackTemplate.findAll()).thenReturn(List.of());

            List<PackTemplateDto> result = serviceAdmin.findAllPackTemplates();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findPackTemplateById returns template when exists")
        void testFindById_exists() {
            when(repositoryPackTemplate.findById(1L)).thenReturn(Optional.of(template));

            Optional<PackTemplateDto> result = serviceAdmin.findPackTemplateById(1L);

            assertTrue(result.isPresent());
            assertEquals("Standard Pack", result.get().name());
            assertEquals(3, result.get().slots().size());
        }

        @Test
        @DisplayName("findPackTemplateById returns empty when not found")
        void testFindById_notFound() {
            when(repositoryPackTemplate.findById(999L)).thenReturn(Optional.empty());

            Optional<PackTemplateDto> result = serviceAdmin.findPackTemplateById(999L);

            assertTrue(result.isEmpty());
        }
    }


    // ========================================
    //        PACK TEMPLATE — CREATE
    // ========================================

    @Nested
    @DisplayName("PackTemplate create operations")
    class CreateTemplateTests {

        @Test
        @DisplayName("createPackTemplate creates template with slots")
        void testCreate_withSlots() {
            when(repositoryPackTemplate.save(any(PackTemplate.class))).thenAnswer(inv -> {
                PackTemplate t = inv.getArgument(0);
                t.setId(10L);
                return t;
            });
            // Mock PackSlot lookups
            when(repositoryPackSlot.findById(packSlots.get(2).getId())).thenReturn(Optional.of(packSlots.get(2)));
            when(repositoryPackSlot.findById(packSlots.get(0).getId())).thenReturn(Optional.of(packSlots.get(0)));

            PackTemplateSlotDto slot1 = new PackTemplateSlotDto(null, packSlots.get(2).getId(), null, 1);
            PackTemplateSlotDto slot2 = new PackTemplateSlotDto(null, packSlots.get(0).getId(), null, 2);
            PackTemplateDto dto = new PackTemplateDto(null, "New Pack", List.of(slot1, slot2));

            PackTemplateDto result = serviceAdmin.createPackTemplate(dto);

            assertEquals("New Pack", result.name());
            assertEquals(2, result.slots().size());
            verify(repositoryPackTemplate).save(any(PackTemplate.class));
        }

        @Test
        @DisplayName("createPackTemplate creates template without slots")
        void testCreate_noSlots() {
            when(repositoryPackTemplate.save(any(PackTemplate.class))).thenAnswer(inv -> {
                PackTemplate t = inv.getArgument(0);
                t.setId(11L);
                return t;
            });

            PackTemplateDto dto = new PackTemplateDto(null, "Empty Pack", null);

            PackTemplateDto result = serviceAdmin.createPackTemplate(dto);

            assertEquals("Empty Pack", result.name());
            assertTrue(result.slots().isEmpty());
        }
    }


    // ========================================
    //        PACK TEMPLATE — UPDATE
    // ========================================

    @Nested
    @DisplayName("PackTemplate update operations")
    class UpdateTemplateTests {

        @Test
        @DisplayName("updatePackTemplate replaces slots")
        void testUpdate_replacesSlots() {
            PackTemplate mutableTemplate = new PackTemplate();
            mutableTemplate.setId(1L);
            mutableTemplate.setName("Standard Pack");
            mutableTemplate.setSlots(new ArrayList<>(template.getSlots()));

            when(repositoryPackTemplate.findById(1L)).thenReturn(Optional.of(mutableTemplate));
            when(repositoryPackTemplate.save(any(PackTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryPackSlot.findById(packSlots.get(2).getId())).thenReturn(Optional.of(packSlots.get(2)));

            PackTemplateSlotDto newSlot = new PackTemplateSlotDto(null, packSlots.get(2).getId(), null, 1);
            PackTemplateDto dto = new PackTemplateDto(null, "Updated Pack", List.of(newSlot));

            Optional<PackTemplateDto> result = serviceAdmin.updatePackTemplate(1L, dto);

            assertTrue(result.isPresent());
            assertEquals("Updated Pack", result.get().name());
            assertEquals(1, result.get().slots().size());
        }

        @Test
        @DisplayName("updatePackTemplate returns empty when not found")
        void testUpdate_notFound() {
            when(repositoryPackTemplate.findById(999L)).thenReturn(Optional.empty());

            PackTemplateDto dto = new PackTemplateDto(null, "Ghost", null);
            Optional<PackTemplateDto> result = serviceAdmin.updatePackTemplate(999L, dto);

            assertTrue(result.isEmpty());
            verify(repositoryPackTemplate, never()).save(any());
        }
    }


    // ========================================
    //        PACK TEMPLATE — DELETE
    // ========================================

    @Nested
    @DisplayName("PackTemplate delete operations")
    class DeleteTemplateTests {

        @Test
        @DisplayName("deletePackTemplate deletes existing template")
        void testDelete_success() {
            when(repositoryPackTemplate.findById(1L)).thenReturn(Optional.of(template));

            serviceAdmin.deletePackTemplate(1L);

            verify(repositoryPackTemplate).delete(template);
        }

        @Test
        @DisplayName("deletePackTemplate throws when not found")
        void testDelete_notFound() {
            when(repositoryPackTemplate.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> serviceAdmin.deletePackTemplate(999L));
            verify(repositoryPackTemplate, never()).delete(any());
        }
    }


    // ========================================
    //        DROP RATES
    // ========================================

    @Nested
    @DisplayName("Drop rate operations")
    class DropRateTests {

        @Test
        @DisplayName("updateDropRateByRarity updates pool cards and returns count")
        void testUpdateDropRate() {
            when(repositoryCard.updateDropRateByRarityForPoolCards(Rarity.LEGENDARY, 0.02)).thenReturn(5);

            int count = serviceAdmin.updateDropRateByRarity(Rarity.LEGENDARY, 0.02);

            assertEquals(5, count);
            verify(repositoryCard).updateDropRateByRarityForPoolCards(Rarity.LEGENDARY, 0.02);
        }

        @Test
        @DisplayName("updateDropRateByRarity returns 0 when no matching cards")
        void testUpdateDropRate_noneMatched() {
            when(repositoryCard.updateDropRateByRarityForPoolCards(Rarity.EPIC, 0.5)).thenReturn(0);

            int count = serviceAdmin.updateDropRateByRarity(Rarity.EPIC, 0.5);

            assertEquals(0, count);
        }
    }


    // ========================================
    //        USER ADMIN VIEW
    // ========================================

    @Nested
    @DisplayName("Admin user view operations")
    class UserAdminTests {

        @Test
        @DisplayName("findAllUsersAdmin returns all users")
        void testFindAllUsers() {
            when(repositoryUser.findAll()).thenReturn(users);

            List<UserDto> result = serviceAdmin.findAllUsersAdmin();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("findUserByIdAdmin returns user when exists")
        void testFindUserById_exists() {
            User alice = users.getFirst();
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Optional<UserDto> result = serviceAdmin.findUserByIdAdmin(alice.getId());

            assertTrue(result.isPresent());
            assertEquals("alice", result.get().username());
        }

        @Test
        @DisplayName("findUserByIdAdmin returns empty when not found")
        void testFindUserById_notFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            Optional<UserDto> result = serviceAdmin.findUserByIdAdmin(999L);

            assertTrue(result.isEmpty());
        }
    }
}
