package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.ShopCheckoutCommand;
import trazzo.back.saasglobal.application.port.in.ShopCheckoutUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.ShopCheckoutRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.ShopCheckoutResponse;

/**
 * Public, unauthenticated self-signup endpoint for the marketing site's /shop checkout form
 * (see SecurityConfig permitAll) — mirrors /requests and /public/plans.
 */
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopCheckoutController {

    private final ShopCheckoutUseCase shopCheckoutUseCase;

    @PostMapping("/checkout")
    public ResponseEntity<ShopCheckoutResponse> checkout(@Valid @RequestBody ShopCheckoutRequest request) {
        var command = new ShopCheckoutCommand(
                request.planId(),
                request.firstName(),
                request.lastNamePaterno(),
                request.lastNameMaterno(),
                request.documentType(),
                request.documentNumber(),
                request.email(),
                request.phone(),
                request.ruc(),
                request.companyName(),
                request.businessName(),
                request.address(),
                request.anotherAdmin(),
                request.adminFirstName(),
                request.adminLastNamePaterno(),
                request.adminLastNameMaterno(),
                request.adminDocumentType(),
                request.adminDocumentNumber(),
                request.adminEmail(),
                request.adminPhone());
        return ResponseEntity.ok(ShopCheckoutResponse.from(shopCheckoutUseCase.checkout(command)));
    }
}
