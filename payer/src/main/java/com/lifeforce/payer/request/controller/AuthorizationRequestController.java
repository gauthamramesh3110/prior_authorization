package com.lifeforce.payer.request.controller;

import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationResponse;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests")
public class AuthorizationRequestController {

    private final AuthorizationRequestService authorizationRequestService;
    public AuthorizationRequestController(AuthorizationRequestService authorizationRequestService) {
        this.authorizationRequestService = authorizationRequestService;
    }

    @PostMapping("")
    public ResponseEntity<HttpAuthorizationResponse> createRequest(@RequestBody @Validated HttpAuthorizationRequest request, BindingResult result) {
        if  (result.hasErrors()) {
            System.err.println(result.getAllErrors());
        }

        HttpAuthorizationResponse response = authorizationRequestService.createAuthorizationRequest(request);

        if (ResponseStatus.SUBMITTED.equals(response.responseStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
