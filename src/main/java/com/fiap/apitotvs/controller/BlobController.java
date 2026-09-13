package com.fiap.apitotvs.controller;

import com.fiap.apitotvs.dto.response.MeetRegisterResponse;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.service.BlobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/blobs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BlobController {

    private final BlobService blobService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MeetRegisterResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        MeetRegisterResponse response = blobService.upload(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MeetRegisterResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(blobService.listByUser(user));
    }
}
