package com.edumanage.subscriptions.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.subscriptions.dto.AssignPackageRequest;
import com.edumanage.subscriptions.dto.SubscriptionDto;
import com.edumanage.subscriptions.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/platform/subscriptions")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<SubscriptionDto>>> listSubscriptions() {
        var subscriptions = subscriptionService.listAllSubscriptions();
        return ResponseEntity.ok(new ApiResponse<>(true, "Subscriptions fetched", subscriptions, null, LocalDateTime.now(), "/api/platform/subscriptions"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionDto>> getSubscription(@PathVariable Long id) {
        var subscription = subscriptionService.getSubscription(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subscription fetched", subscription, null, LocalDateTime.now(), "/api/platform/subscriptions/" + id));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<SubscriptionDto>> assignPackage(@RequestParam Long schoolId,
                                                                      @Valid @RequestBody AssignPackageRequest request) {
        var subscription = subscriptionService.assignPackage(schoolId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Package assigned", subscription, null, LocalDateTime.now(), "/api/platform/subscriptions/assign"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionDto>> updateSubscription(@PathVariable Long id,
                                                                           @RequestParam(required = false) String action) {
        if ("activate".equalsIgnoreCase(action)) {
            var subscription = subscriptionService.activateSubscription(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Subscription activated", subscription, null, LocalDateTime.now(), "/api/platform/subscriptions/" + id));
        } else if ("suspend".equalsIgnoreCase(action)) {
            var subscription = subscriptionService.suspendSubscription(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Subscription suspended", subscription, null, LocalDateTime.now(), "/api/platform/subscriptions/" + id));
        }
        throw new BadRequestException("Invalid action");
    }
}
