package com.ecommerce.payment.webhook;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
public class BbWebhookController {

    private final BbWebhookService bbWebhookService;

    @PostMapping("/bb")
    public ResponseEntity<Void> handleBankWebhook(@Valid @RequestBody BbWebhookRequest request) {
        bbWebhookService.handle(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
