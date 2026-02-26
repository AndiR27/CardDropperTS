package ts.backend_carddropper.rest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.UserApi;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.service.ServiceCard;
import ts.backend_carddropper.service.ServiceUser;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestControllerUser implements UserApi {

    private final ServiceUser serviceUser;
    private final ServiceCard serviceCard;


    //==============================
    //    CRUD
    //==============================

    @Override
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(serviceUser.findAll());
    }

    @Override
    public ResponseEntity<UserDto> getUserById(Long userId) {
        return serviceUser.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserDto> getUserByUsername(String username) {
        return serviceUser.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserDto> getUserByEmail(String email) {
        return serviceUser.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserDto> createUser(UserDto userDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(serviceUser.create(userDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<UserDto> updateUser(Long userId, UserDto userDto) {
        return serviceUser.update(userId, userDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long userId) {
        try {
            serviceUser.delete(userId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    //==============================
    //    USER-CARD RELATIONS
    //==============================

    @Override
    public ResponseEntity<List<CardDto>> getCardsOwnedByUser(Long userId) {
        try {
            return ResponseEntity.ok(serviceUser.getCardsOwned(userId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<List<CardDto>> getCardsCreatedByUser(Long userId) {
        try {
            return ResponseEntity.ok(serviceCard.findAllByCreatedById(userId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<List<CardDto>> getCardsTargetingUser(Long userId) {
        try {
            return ResponseEntity.ok(serviceCard.findAllByTargetUserId(userId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<CardDto> createCardForUser(Long userId, CardDto cardDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(serviceUser.createCard(userId, cardDto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    //==============================
    //    TRADE / PACK / MERGE
    //==============================

    @Override
    public ResponseEntity<Void> tradeCards(Long userId, Long myCardId, Long targetUserId, Long theirCardId) {
        try {
            serviceUser.tradeCard(userId, myCardId, targetUserId, theirCardId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<List<CardDto>> openPack(Long userId, List<Long> requestBody) {
        try {
            return ResponseEntity.ok(serviceUser.openPack(userId, requestBody));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    public ResponseEntity<CardDto> mergeCards(Long userId, List<Long> requestBody) {
        try {
            return ResponseEntity.ok(serviceUser.mergeCards(userId, requestBody));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
