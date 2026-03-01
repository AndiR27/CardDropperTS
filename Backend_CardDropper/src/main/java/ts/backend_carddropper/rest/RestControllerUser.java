package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.UserApi;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.service.ServiceUser;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestControllerUser implements UserApi {

    private final ServiceUser serviceUser;


    //==============================
    //    CRUD (admin)
    //==============================

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(serviceUser.findAll());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceUser.create(userDto));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(Long userId, UserDto userDto) {
        return serviceUser.update(userId, userDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(Long userId) {
        serviceUser.delete(userId);
        return ResponseEntity.noContent().build();
    }


    //==============================
    //    LOOKUP
    //==============================

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
}
