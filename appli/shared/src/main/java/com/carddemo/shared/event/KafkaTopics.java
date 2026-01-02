package com.carddemo.shared.event;

/**
 * Kafka topic constants.
 *
 * Replaces: MQ Series queue definitions from mainframe
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class
    }

    /**
     * Topic for transaction events (8 partitions, 7 days retention)
     * Partition key: accountId
     */
    public static final String TRANSACTIONS = "carddemo.transactions";

    /**
     * Topic for card events (4 partitions, 7 days retention)
     * Partition key: cardNumber
     */
    public static final String CARDS = "carddemo.cards";

    /**
     * Topic for account events (4 partitions, 7 days retention)
     * Partition key: accountId
     */
    public static final String ACCOUNTS = "carddemo.accounts";

    /**
     * Dead Letter Queue for failed events (4 partitions, 30 days retention)
     * Partition key: eventId
     */
    public static final String DLQ = "carddemo.dlq";

    /**
     * Consumer group for notification service
     */
    public static final String NOTIFICATION_GROUP = "notification-service";
}
