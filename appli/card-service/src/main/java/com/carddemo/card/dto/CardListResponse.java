package com.carddemo.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Card List Response DTO
 *
 * Wraps list of cards with metadata (replaces BMS COCRDLIC screen data).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardListResponse {

    private Long accountId;
    private List<CardResponse> cards;
    private int totalCards;
    private int activeCards;
    private int expiredCards;
    private int blockedCards;

    /**
     * Create summary from card list
     */
    public static CardListResponse of(Long accountId, List<CardResponse> cards) {
        int active = 0, expired = 0, blocked = 0;

        for (CardResponse card : cards) {
            if (card.isExpired()) {
                expired++;
            } else if ("S".equals(card.getStatus())) {
                blocked++;
            } else if (card.isActive()) {
                active++;
            }
        }

        return CardListResponse.builder()
                .accountId(accountId)
                .cards(cards)
                .totalCards(cards.size())
                .activeCards(active)
                .expiredCards(expired)
                .blockedCards(blocked)
                .build();
    }
}
