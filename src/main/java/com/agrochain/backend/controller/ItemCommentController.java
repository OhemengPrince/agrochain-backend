package com.agrochain.backend.controller;

import com.agrochain.backend.dto.ItemCommentRequest;
import com.agrochain.backend.dto.ItemCommentResponse;
import com.agrochain.backend.dto.ReactionRequest;
import com.agrochain.backend.model.ItemType;
import com.agrochain.backend.service.ItemCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemCommentController {

    private final ItemCommentService itemCommentService;

    @GetMapping("/{itemType}/{itemId}/comments")
    public ResponseEntity<List<ItemCommentResponse>> list(Authentication authentication,
                                                            @PathVariable ItemType itemType,
                                                            @PathVariable Long itemId) {
        return ResponseEntity.ok(itemCommentService.list(itemType, itemId, authentication.getName()));
    }

    @PostMapping("/{itemType}/{itemId}/comments")
    public ResponseEntity<ItemCommentResponse> create(Authentication authentication,
                                                        @PathVariable ItemType itemType,
                                                        @PathVariable Long itemId,
                                                        @Valid @RequestBody ItemCommentRequest request) {
        ItemCommentResponse response = itemCommentService.create(
                itemType, itemId, authentication.getName(), request.getText(), request.getParentId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long commentId) {
        itemCommentService.delete(commentId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comments/{commentId}/react")
    public ResponseEntity<ItemCommentResponse> react(Authentication authentication,
                                                       @PathVariable Long commentId,
                                                       @Valid @RequestBody ReactionRequest request) {
        return ResponseEntity.ok(itemCommentService.react(commentId, authentication.getName(), request.getEmoji()));
    }
}
