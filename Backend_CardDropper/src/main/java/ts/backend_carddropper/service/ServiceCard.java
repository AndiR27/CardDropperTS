package ts.backend_carddropper.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.utils.ImageUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Value("${app.card-images.dir}")
    private String cardImagesDir;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @PostConstruct
    void initImageDirectory() throws IOException {
        Files.createDirectories(Paths.get(cardImagesDir));
        log.info("Card images directory: {}", cardImagesDir);
    }


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
     * Mettre à jour une carte (admin)
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
     * Supprimer une carte par son id (admin)
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
    //    IMAGE UPLOAD
    //==============================

    /**
     * Crée une carte avec une image uploadée.
     * Le créateur est défini par creatorId, la carte entre dans le pool (pas de propriétaire).
     * L'image est stockée dans {baseDir}/{username}/{cardId}_{sanitizedFilename}.
     */
    @Transactional
    public CardDto createCardWithImage(CardDto cardDto, MultipartFile image, Long creatorId, String username) {
        validateImage(image);

        User creator = findUserOrThrow(creatorId);

        // Construire l'entité et appliquer les relations
        Card card = mapperCard.toEntity(cardDto);
        card.setCreatedBy(creator);
        if (cardDto.targetUserId() != null) {
            card.setTargetUser(findUserOrThrow(cardDto.targetUserId()));
        }
        Card saved = repositoryCard.save(card);

        // Construire le nom de fichier et stocker l'image
        String sanitizedName = ImageUtils.sanitizeFilename(image.getOriginalFilename());
        String filename = saved.getId() + "_" + sanitizedName;

        try {
            Path userDir = Paths.get(cardImagesDir, username);
            Files.createDirectories(userDir);
            Path target = userDir.resolve(filename);
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored card image: {}/{}", username, filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store card image", e);
        }

        // Mettre à jour l'imageUrl avec le chemin relatif complet (username/filename)
        String relativePath = username + "/" + filename;
        saved.setImageUrl(relativePath);
        Card updated = repositoryCard.save(saved);

        log.info("Created card '{}' (rarity={}) with image: {}", updated.getName(), updated.getRarity(), relativePath);
        return mapperCard.toDto(updated);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException("Invalid image type. Allowed: PNG, JPEG, WebP");
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image too large. Maximum size: 5MB");
        }
    }

    //==============================
    //       PRIVATE METHODS
    //==============================

    private User findUserOrThrow(Long id) {
        return repositoryUser.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    /**
     * Applique les relations User (creator, target) sur une entité Card
     * à partir des IDs portés par le DTO.
     */
    private void applyUserRelations(CardDto dto, Card card) {
        if (dto.createdById() != null)  card.setCreatedBy(findUserOrThrow(dto.createdById()));
        if (dto.targetUserId() != null) card.setTargetUser(findUserOrThrow(dto.targetUserId()));
    }
}
