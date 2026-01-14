package com.carddemo.card.service;

import com.carddemo.card.dto.CardListResponse;
import com.carddemo.card.dto.CardResponse;
import com.carddemo.card.dto.UpdateCardStatusRequest;
import com.carddemo.card.entity.CreditCard;
import com.carddemo.card.exception.CardException;
import com.carddemo.card.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Card Service
 *
 * Business logic for credit card operations.
 * Replaces COBOL paragraphs from COCRDLIC, COCRDSLC, COCRDUPC.
 *
 * COBOL to Java Mapping:
 *   PROCESS-CARD-LIST      -> getCardsByAccount()
 *   PROCESS-CARD-SELECT    -> getCardDetails()
 *   PROCESS-CARD-UPDATE    -> updateCardStatus()
 *   READ-CARD-FILE         -> repository.findById()
 *   WRITE-CARD-FILE        -> repository.save()
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardService {

    private final CreditCardRepository cardRepository;

    /**
     * Get all cards for an account (COCRDLIC - Card List)
     *
     * Replaces COBOL PROCESS-CARD-LIST paragraph.
     */
    public CardListResponse getCardsByAccount(Long accountId) {
        log.info("Fetching cards for account: {}", accountId);

        List<CreditCard> cards = cardRepository.findByAccountIdOrderByIssuedDateDesc(accountId);

        if (cards.isEmpty()) {
            log.warn("No cards found for account: {}", accountId);
        }

        List<CardResponse> cardResponses = cards.stream()
                .map(CardResponse::fromEntity)
                .collect(Collectors.toList());

        return CardListResponse.of(accountId, cardResponses);
    }

    /**
     * Get all cards with pagination
     */
    public Page<CardResponse> getAllCards(Pageable pageable) {
        log.info("Fetching all cards with pagination");

        return cardRepository.findAll(pageable)
                .map(CardResponse::fromEntity);
    }

    /**
     * Get cards by status
     */
    public Page<CardResponse> getCardsByStatus(String status, Pageable pageable) {
        log.info("Fetching cards with status: {}", status);

        return cardRepository.findByActiveStatus(status, pageable)
                .map(CardResponse::fromEntity);
    }

    /**
     * Get card details (COCRDSLC - Card Select)
     *
     * Replaces COBOL PROCESS-CARD-SELECT paragraph.
     */
    public CardResponse getCardDetails(String cardNumber) {
        log.info("Fetching card details for: ****{}", cardNumber.substring(cardNumber.length() - 4));

        CreditCard card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> CardException.cardNotFound(cardNumber));

        return CardResponse.fromEntity(card);
    }

    /**
     * Update card status (COCRDUPC - Card Update)
     *
     * Replaces COBOL PROCESS-CARD-UPDATE paragraph.
     * Handles: Activate, Block, Close operations.
     */
    @Transactional
    public CardResponse updateCardStatus(String cardNumber, UpdateCardStatusRequest request) {
        log.info("Updating card status for: ****{} to {}",
                cardNumber.substring(cardNumber.length() - 4), request.getStatus());

        CreditCard card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> CardException.cardNotFound(cardNumber));

        // Validate status transition
        validateStatusTransition(card, request.getStatus());

        // Update status
        card.setActiveStatus(request.getStatus());
        CreditCard savedCard = cardRepository.save(card);

        log.info("Card status updated successfully");
        return CardResponse.fromEntity(savedCard);
    }

    /**
     * Block a card (emergency block)
     */
    @Transactional
    public CardResponse blockCard(String cardNumber, String reason) {
        log.warn("Blocking card: ****{}, reason: {}",
                cardNumber.substring(cardNumber.length() - 4), reason);

        CreditCard card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> CardException.cardNotFound(cardNumber));

        if (card.isBlocked()) {
            throw CardException.cardAlreadyBlocked(cardNumber);
        }

        card.setActiveStatus("S");
        CreditCard savedCard = cardRepository.save(card);

        log.info("Card blocked successfully");
        return CardResponse.fromEntity(savedCard);
    }

    /**
     * Find cards expiring soon (for renewal process)
     */
    public List<CardResponse> getCardsExpiringSoon(int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);

        log.info("Finding cards expiring between {} and {}", startDate, endDate);

        return cardRepository.findCardsExpiringBetween(startDate, endDate)
                .stream()
                .map(CardResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search cards by cardholder name
     */
    public List<CardResponse> searchByName(String name) {
        log.info("Searching cards by name: {}", name);

        return cardRepository.searchByEmbossedName(name)
                .stream()
                .map(CardResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get card statistics for an account
     */
    public CardListResponse getCardStats(Long accountId) {
        return getCardsByAccount(accountId);
    }

    /**
     * Get all cards for a customer (across all their accounts)
     */
    public List<CardResponse> getCardsByCustomerId(Integer customerId) {
        log.info("Fetching cards for customer: {}", customerId);

        List<CreditCard> cards = cardRepository.findByCustomerId(customerId);

        if (cards.isEmpty()) {
            log.warn("No cards found for customer: {}", customerId);
        }

        return cards.stream()
                .map(CardResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get card by last 4 digits (for frontend navigation)
     * Note: Returns first matching card - in production should use additional identifiers
     */
    public CardResponse getCardByLastFour(String lastFour) {
        log.info("Fetching card by last 4 digits: {}", lastFour);

        List<CreditCard> cards = cardRepository.findByLastFourDigits(lastFour);

        if (cards.isEmpty()) {
            throw CardException.cardNotFound("****" + lastFour);
        }

        // Return first match - in production, should use additional context
        return CardResponse.fromEntity(cards.get(0));
    }

    /**
     * Reissue a card (create renewal request)
     * In production, this would trigger card production workflow
     */
    @Transactional
    public CardResponse reissueCard(String cardNumber) {
        log.info("Processing reissue request for card: ****{}",
                cardNumber.length() > 4 ? cardNumber.substring(cardNumber.length() - 4) : "****");

        CreditCard card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> CardException.cardNotFound(cardNumber));

        // In production: create reissue record, trigger card production workflow
        // For demo: just log and return current card
        log.info("Reissue request logged for card account: {}", card.getAccountId());

        return CardResponse.fromEntity(card);
    }

    /**
     * Update card status by last 4 digits
     */
    @Transactional
    public CardResponse updateCardStatusByLastFour(String lastFour, UpdateCardStatusRequest request) {
        log.info("Updating card status by last 4: {} to {}", lastFour, request.getStatus());

        CreditCard card = findCardByLastFour(lastFour);
        validateStatusTransition(card, request.getStatus());
        card.setActiveStatus(request.getStatus());
        CreditCard savedCard = cardRepository.save(card);

        log.info("Card status updated successfully");
        return CardResponse.fromEntity(savedCard);
    }

    /**
     * Block card by last 4 digits
     */
    @Transactional
    public CardResponse blockCardByLastFour(String lastFour, String reason) {
        log.warn("Blocking card by last 4: {}, reason: {}", lastFour, reason);

        CreditCard card = findCardByLastFour(lastFour);
        if (card.isBlocked()) {
            throw CardException.cardAlreadyBlocked(card.getCardNumber());
        }

        card.setActiveStatus("S");
        CreditCard savedCard = cardRepository.save(card);

        log.info("Card blocked successfully");
        return CardResponse.fromEntity(savedCard);
    }

    /**
     * Reissue card by last 4 digits
     */
    @Transactional
    public CardResponse reissueCardByLastFour(String lastFour) {
        log.info("Processing reissue request for card ending: {}", lastFour);

        CreditCard card = findCardByLastFour(lastFour);

        // In production: create reissue record, trigger card production workflow
        // For demo: just log and return current card
        log.info("Reissue request logged for card account: {}", card.getAccountId());

        return CardResponse.fromEntity(card);
    }

    /**
     * Helper to find card by last 4 digits
     */
    private CreditCard findCardByLastFour(String lastFour) {
        List<CreditCard> cards = cardRepository.findByLastFourDigits(lastFour);
        if (cards.isEmpty()) {
            throw CardException.cardNotFound("****" + lastFour);
        }
        return cards.get(0);
    }

    // Private helper methods

    /**
     * Validate card status transition rules
     *
     * Business rules from COBOL VALIDATE-STATUS-CHANGE:
     * - Cannot activate a closed card
     * - Cannot close an already closed card
     * - Can always block a card (emergency)
     */
    private void validateStatusTransition(CreditCard card, String newStatus) {
        String currentStatus = card.getActiveStatus();

        // Can always block
        if ("S".equals(newStatus)) {
            return;
        }

        // Cannot reactivate closed card
        if ("N".equals(currentStatus) && "Y".equals(newStatus)) {
            throw CardException.invalidOperation("Cannot reactivate a closed card");
        }

        // Cannot close already closed card
        if ("N".equals(currentStatus) && "N".equals(newStatus)) {
            throw CardException.invalidOperation("Card is already closed");
        }

        // Check expiry for activation
        if ("Y".equals(newStatus) && card.isExpired()) {
            throw CardException.cardExpired(card.getCardNumber());
        }
    }
}
