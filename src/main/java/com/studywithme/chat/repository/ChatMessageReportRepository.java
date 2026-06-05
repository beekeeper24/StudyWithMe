package com.studywithme.chat.repository;

import com.studywithme.chat.domain.ChatMessageReport;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageReportRepository extends JpaRepository<ChatMessageReport, Long> {

	boolean existsByMessageIdAndReporterMemberId(Long messageId, Long reporterMemberId);

	boolean existsByIdAndStatusAndAssignedAdminMemberIdIsNull(Long id, ChatMessageReportStatus status);

	List<ChatMessageReport> findAllByStatusOrderByCreatedAtDescIdDesc(ChatMessageReportStatus status);

	List<ChatMessageReport> findAllByOrderByCreatedAtDescIdDesc();
}
