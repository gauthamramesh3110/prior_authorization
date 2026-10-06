package com.lifeforce.payer.request.controller;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.dto.RequestDetails;
import com.lifeforce.payer.request.dto.RequestSummary;
import com.lifeforce.payer.request.dto.EvidenceSubmission;
import com.lifeforce.payer.request.dto.EvidenceSubmissionResponse;
import com.lifeforce.payer.request.dto.AuthorizationSubmission;
import com.lifeforce.payer.request.dto.AuthorizationSubmissionResponse;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import com.lifeforce.payer.request.service.RequestQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class AuthorizationRequestController {

    private final AuthorizationRequestService authorizationRequestService;
    private final EvidenceSubmissionService evidenceSubmissionService;
    private final RequestQueryService requestQueryService;

    public AuthorizationRequestController(AuthorizationRequestService authorizationRequestService, EvidenceSubmissionService evidenceSubmissionService, RequestQueryService requestQueryService) {
        this.authorizationRequestService = authorizationRequestService;
        this.evidenceSubmissionService = evidenceSubmissionService;
        this.requestQueryService = requestQueryService;
    }

    @GetMapping("")
    public ResponseEntity<List<RequestSummary>> getProviderRequests(@RequestParam("providerId") UUID providerId, @RequestParam(name = "status", required = false) RequestStatus requestStatus) {
        return ResponseEntity.ok(requestQueryService.getProviderRequests(providerId, requestStatus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDetails> getProviderRequestDetails(@PathVariable("id") UUID requestId, @RequestParam("providerId") UUID providerId) {
        return ResponseEntity.ok(requestQueryService.getProviderRequestDetails(requestId, providerId));
    }

    @PatchMapping("/{id}/evidence")
    public EvidenceSubmissionResponse submitEvidence(@PathVariable("id") UUID requestId, @Valid @RequestBody EvidenceSubmission submission) {
        return evidenceSubmissionService.submitEvidence(requestId, submission);
    }

    @PostMapping("")
    public ResponseEntity<AuthorizationSubmissionResponse> submitAuthorizationRequest(@RequestBody @Valid AuthorizationSubmission submission) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorizationRequestService.submitAuthorizationRequest(submission));
    }
}
