package com.studywithme.report.repository;

import com.studywithme.report.domain.MemberSanction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSanctionRepository extends JpaRepository<MemberSanction, Long> {

	List<MemberSanction> findAllByTargetMemberIdOrderByCreatedAtDescIdDesc(Long targetMemberId);
}
