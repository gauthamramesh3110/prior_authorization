package com.lifeforce.payer.request.controller;

import com.lifeforce.payer.request.dto.EvidenceSubmission;
import com.lifeforce.payer.request.dto.EvidenceSubmissionResponse;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationResponse;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class AuthorizationRequestController {

    private final AuthorizationRequestService authorizationRequestService;
    private final EvidenceSubmissionService evidenceSubmissionService;

    public AuthorizationRequestController(AuthorizationRequestService authorizationRequestService, EvidenceSubmissionService evidenceSubmissionService) {
        this.authorizationRequestService = authorizationRequestService;
        this.evidenceSubmissionService = evidenceSubmissionService;
    }

    @PatchMapping("/{id}/evidence")
    public EvidenceSubmissionResponse submitEvidence(@PathVariable("id") UUID id, @Valid @RequestBody EvidenceSubmission submission) {
        return evidenceSubmissionService.submitEvidence(id, submission);
    }

    @PostMapping("")
    public ResponseEntity<HttpAuthorizationResponse> createRequest(@RequestBody @Validated HttpAuthorizationRequest request, BindingResult result) {
        if  (result.hasErrors()) {
            System.err.println(result.getAllErrors());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.MALFORMED_REQUEST,
                    result.getAllErrors().getFirst().getDefaultMessage()
            ));
        }

        HttpAuthorizationResponse response = authorizationRequestService.createAuthorizationRequest(request);

        if (ResponseStatus.SUBMITTED.equals(response.responseStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}
