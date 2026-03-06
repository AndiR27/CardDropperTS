package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.AdminApi;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.models.PackSlotDto;
import ts.backend_carddropper.models.PackTemplateDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.service.ServiceAdmin;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RestControllerAdmin implements AdminApi {

    private final ServiceAdmin serviceAdmin;


    //==============================
    //    PACK SLOTS
    //==============================

    @Override
    public ResponseEntity<List<PackSlotDto>> getPackSlots() {
        return ResponseEntity.ok(serviceAdmin.findAllPackSlots());
    }

    @Override
    public ResponseEntity<PackSlotDto> createPackSlot(PackSlotDto packSlotDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAdmin.createPackSlot(packSlotDto));
    }


    //==============================
    //    PACK TEMPLATES
    //==============================

    @Override
    public ResponseEntity<List<PackTemplateDto>> getPackTemplates() {
        return ResponseEntity.ok(serviceAdmin.findAllPackTemplates());
    }

    @Override
    public ResponseEntity<PackTemplateDto> getPackTemplateById(Long templateId) {
        return serviceAdmin.findPackTemplateById(templateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<PackTemplateDto> createPackTemplate(PackTemplateDto packTemplateDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceAdmin.createPackTemplate(packTemplateDto));
    }

    @Override
    public ResponseEntity<PackTemplateDto> updatePackTemplate(Long templateId, PackTemplateDto packTemplateDto) {
        return serviceAdmin.updatePackTemplate(templateId, packTemplateDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deletePackTemplate(Long templateId) {
        serviceAdmin.deletePackTemplate(templateId);
        return ResponseEntity.noContent().build();
    }


    //==============================
    //    DROP RATES
    //==============================

    @Override
    public ResponseEntity<Integer> updateDropRateByRarity(Rarity rarity, Double dropRate) {
        return ResponseEntity.ok(serviceAdmin.updateDropRateByRarity(rarity, dropRate));
    }


    //==============================
    //    USERS (ADMIN VIEW)
    //==============================

    @Override
    public ResponseEntity<List<UserDto>> getAdminUsers() {
        return ResponseEntity.ok(serviceAdmin.findAllUsersAdmin());
    }

    @Override
    public ResponseEntity<UserDto> getAdminUserById(Long userId) {
        return serviceAdmin.findUserByIdAdmin(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
