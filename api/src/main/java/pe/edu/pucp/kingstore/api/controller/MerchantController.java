package pe.edu.pucp.kingstore.api.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import pe.edu.pucp.kingstore.api.context.MerchantContext;

import pe.edu.pucp.kingstore.domain.dto.user.MerchantPasswordRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileRequestDTO;

import pe.edu.pucp.kingstore.service.user.MerchantProfileService;


import java.util.*;


@RestController
@RequestMapping("/merchant")
public class MerchantController extends BaseMerchantController {

    private final MerchantProfileService merchantProfileService;

    public MerchantController(MerchantContext merchantContext,
                              MerchantProfileService merchantProfileService) {
        super(merchantContext);
        this.merchantProfileService = merchantProfileService;
    }

    //  GET /profile
    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {
        return handle(() -> ResponseEntity.ok(
                merchantProfileService.toResponseDTO(
                        currentMerchant(authentication))));
    }
    //  PUT /profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication authentication,
                                           @RequestBody MerchantProfileRequestDTO request) {
        return handle(() -> ResponseEntity.ok(
                merchantProfileService.toResponseDTO(
                        merchantProfileService.updateProfile(
                                currentMerchant(authentication), request))));
    }
    //  PATCH /profile/password
    @PatchMapping("/profile/password")
    public ResponseEntity<?> updatePassword(Authentication authentication,
                                            @RequestBody MerchantPasswordRequestDTO request) {
        return handle(() -> {
            merchantProfileService.updatePassword(
                    currentMerchant(authentication), request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        });
    }

 }
