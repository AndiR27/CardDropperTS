package ts.backend_carddropper.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.PackTemplateSlot;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperPackSlot;
import ts.backend_carddropper.mapping.MapperPackTemplate;
import ts.backend_carddropper.models.PackSlotDto;
import ts.backend_carddropper.models.PackTemplateDto;
import ts.backend_carddropper.models.PackTemplateSlotDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackSlot;
import ts.backend_carddropper.repository.RepositoryPackTemplate;

import ts.backend_carddropper.models.GrantPacksRequest;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdmin {

    private final RepositoryPackTemplate repositoryPackTemplate;
    private final RepositoryPackSlot repositoryPackSlot;
    private final RepositoryCard repositoryCard;
    private final MapperPackTemplate mapperPackTemplate;
    private final MapperPackSlot mapperPackSlot;
    private final ServiceUser serviceUser;
    private final ServicePack servicePack;


    //==============================
    //    PACK SLOT CRUD
    //==============================

    public List<PackSlotDto> findAllPackSlots() {
        return repositoryPackSlot.findAll().stream()
                .map(mapperPackSlot::toDto)
                .toList();
    }

    @Transactional
    public PackSlotDto createPackSlot(PackSlotDto dto) {
        PackSlot slot = mapperPackSlot.toEntity(dto);
        PackSlot saved = repositoryPackSlot.save(slot);
        log.info("Created pack slot '{}'", saved.getName());
        return mapperPackSlot.toDto(saved);
    }


    //==============================
    //    PACK TEMPLATE CRUD
    //==============================

    public List<PackTemplateDto> findAllPackTemplates() {
        return repositoryPackTemplate.findAll().stream()
                .map(mapperPackTemplate::toDto)
                .toList();
    }

    public Optional<PackTemplateDto> findPackTemplateById(Long id) {
        return repositoryPackTemplate.findById(id)
                .map(mapperPackTemplate::toDto);
    }

    @Transactional
    public PackTemplateDto createPackTemplate(PackTemplateDto dto) {
        PackTemplate template = mapperPackTemplate.toEntity(dto);

        if (dto.slots() != null) {
            for (PackTemplateSlotDto slotDto : dto.slots()) {
                PackTemplateSlot templateSlot = buildTemplateSlot(template, slotDto);
                template.getSlots().add(templateSlot);
            }
        }

        PackTemplate saved = repositoryPackTemplate.save(template);
        log.info("Created pack template '{}' with {} slot(s)", saved.getName(), saved.getSlots().size());
        return mapperPackTemplate.toDto(saved);
    }

    @Transactional
    public Optional<PackTemplateDto> updatePackTemplate(Long id, PackTemplateDto dto) {
        return repositoryPackTemplate.findById(id)
                .map(existing -> {
                    mapperPackTemplate.updateEntity(dto, existing);

                    existing.getSlots().clear();
                    if (dto.slots() != null) {
                        for (PackTemplateSlotDto slotDto : dto.slots()) {
                            PackTemplateSlot templateSlot = buildTemplateSlot(existing, slotDto);
                            existing.getSlots().add(templateSlot);
                        }
                    }

                    PackTemplate saved = repositoryPackTemplate.save(existing);
                    log.info("Updated pack template '{}' with {} slot(s)", saved.getName(), saved.getSlots().size());
                    return mapperPackTemplate.toDto(saved);
                });
    }

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

    @Transactional
    public int updateDropRateByRarity(Rarity rarity, double dropRate) {
        int count = repositoryCard.updateDropRateByRarityForPoolCards(rarity, dropRate);
        log.info("Updated dropRate to {} for {} pool cards of rarity {}", dropRate, count, rarity);
        return count;
    }


    //==============================
    //    USER ADMIN VIEW
    //==============================

    public List<UserDto> findAllUsersAdmin() {
        return serviceUser.findAll();
    }

    public Optional<UserDto> findUserByIdAdmin(Long id) {
        return serviceUser.findById(id);
    }


    //==============================
    //    GRANT PACKS
    //==============================

    @Transactional
    public void grantPacks(GrantPacksRequest request) {
        if (request.userIds() == null || request.userIds().isEmpty()) {
            servicePack.grantPacksToAll(request.templateId(), request.quantity());
        } else {
            servicePack.grantPacks(request.userIds(), request.templateId(), request.quantity());
        }
    }


    //==============================
    //    PRIVATE HELPERS
    //==============================

    private PackTemplateSlot buildTemplateSlot(PackTemplate template, PackTemplateSlotDto slotDto) {
        PackSlot packSlot = repositoryPackSlot.findById(slotDto.slotId())
                .orElseThrow(() -> new EntityNotFoundException("PackSlot not found with id: " + slotDto.slotId()));

        PackTemplateSlot templateSlot = new PackTemplateSlot();
        templateSlot.setPackTemplate(template);
        templateSlot.setPackSlot(packSlot);
        templateSlot.setCount(slotDto.count());
        return templateSlot;
    }
}
