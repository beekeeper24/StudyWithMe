package com.studywithme.report.repository;

import com.studywithme.report.domain.ContentReport;
import com.studywithme.report.domain.ContentReportStatus;
import com.studywithme.report.domain.ContentReportTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

	boolean existsByTargetTypeAndTargetIdAndReporterMemberId(
		ContentReportTargetType targetType,
		Long targetId,
		Long reporterMemberId
	);

	boolean existsByIdAndStatusAndAssignedAdminMemberIdIsNull(Long id, ContentReportStatus status);

	List<ContentReport> findAllByStatusOrderByCreatedAtDescIdDesc(ContentReportStatus status);

	List<ContentReport> findAllByOrderByCreatedAtDescIdDesc();
}
