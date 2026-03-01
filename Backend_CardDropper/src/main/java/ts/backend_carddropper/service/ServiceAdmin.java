package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperPackSlot;
import ts.backend_carddropper.mapping.MapperPackTemplate;
import ts.backend_carddropper.models.PackSlotDto;
import ts.backend_carddropper.models.PackTemplateDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdmin {

    private final RepositoryPackTemplate repositoryPackTemplate;
    private final RepositoryCard repositoryCard;
    private final MapperPackTemplate mapperPackTemplate;
    private final MapperPackSlot mapperPackSlot;
    private final ServiceUser serviceUser;


    //==============================
    //    PACK TEMPLATE CRUD
    //==============================

    /**
     * Trouver tous les pack templates
     */
    public List<PackTemplateDto> findAllPackTemplates() {
        return repositoryPackTemplate.findAll().stream()
                .map(mapperPackTemplate::toDto)
                .toList();
    }

    /**
     * Trouver un pack template par son id
     */
    public Optional<PackTemplateDto> findPackTemplateById(Long id) {
        return repositoryPackTemplate.findById(id)
                .map(mapperPackTemplate::toDto);
    }

    /**
     * Créer un pack template avec ses slots
     */
    @Transactional
    public PackTemplateDto createPackTemplate(PackTemplateDto dto) {
        PackTemplate template = mapperPackTemplate.toEntity(dto);

        if (dto.slots() != null) {
            for (PackSlotDto slotDto : dto.slots()) {
                PackSlot slot = mapperPackSlot.toEntity(slotDto);
                slot.setPackTemplate(template);
                template.getSlots().add(slot);
            }
        }

        PackTemplate saved = repositoryPackTemplate.save(template);
        log.info("Created pack template '{}' with {} slot(s)", saved.getName(), saved.getSlots().size());
        return mapperPackTemplate.toDto(saved);
    }

    /**
     * Mettre à jour un pack template (remplace les slots existants)
     */
    @Transactional
    public Optional<PackTemplateDto> updatePackTemplate(Long id, PackTemplateDto dto) {
        return repositoryPackTemplate.findById(id)
                .map(existing -> {
                    mapperPackTemplate.updateEntity(dto, existing);

                    // Replace slots (orphanRemoval handles DB cleanup)
                    existing.getSlots().clear();
                    if (dto.slots() != null) {
                        for (PackSlotDto slotDto : dto.slots()) {
                            PackSlot slot = mapperPackSlot.toEntity(slotDto);
                            slot.setPackTemplate(existing);
                            existing.getSlots().add(slot);
                        }
                    }

                    PackTemplate saved = repositoryPackTemplate.save(existing);
                    log.info("Updated pack template '{}' with {} slot(s)", saved.getName(), saved.getSlots().size());
                    return mapperPackTemplate.toDto(saved);
                });
    }

    /**
     * Supprimer un pack template
     */
    @Transactional
    public void deletePackTemplate(Long id) {
        PackTemplate template = repositoryPackTemplate.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PackTemplate not found with id: " + id));
        repositoryPackTemplate.delete(template);
        log.info("Deleted pack template '{}'", template.getName());
    }


    //==============================
    //    DROP RATES
    //==============================

    /**
     * Modifier le dropRate de toutes les cartes du pool pour une rareté donnée
     */
    @Transactional
    public int updateDropRateByRarity(Rarity rarity, double dropRate) {
        int count = repositoryCard.updateDropRateByRarityForPoolCards(rarity, dropRate);
        log.info("Updated dropRate to {} for {} pool cards of rarity {}", dropRate, count, rarity);
        return count;
    }


    //==============================
    //    USER ADMIN VIEW
    //==============================

    /**
     * Trouver tous les utilisateurs (vue admin complète)
     */
    public List<UserDto> findAllUsersAdmin() {
        return serviceUser.findAll();
    }

    /**
     * Trouver un utilisateur par son id (vue admin)
     */
    public Optional<UserDto> findUserByIdAdmin(Long id) {
        return serviceUser.findById(id);
    }
}
