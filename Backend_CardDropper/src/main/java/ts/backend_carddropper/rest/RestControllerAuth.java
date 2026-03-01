package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.service.ServiceAuth;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RestControllerAuth {

    private final ServiceAuth serviceAuth;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(serviceAuth.getOrCreateCurrentUser());
    }
}