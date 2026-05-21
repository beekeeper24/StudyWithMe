package com.studywithme.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notification.outbox", name = "worker-enabled", havingValue = "true")
public class NotificationOutboxWorker {

	private final NotificationOutboxProcessor notificationOutboxProcessor;

	public NotificationOutboxWorker(NotificationOutboxProcessor notificationOutboxProcessor) {
		this.notificationOutboxProcessor = notificationOutboxProcessor;
	}

	@Scheduled(fixedDelayString = "${app.notification.outbox.poll-interval-ms:5000}")
	public void processPending() {
		notificationOutboxProcessor.processPending(100);
	}
}
