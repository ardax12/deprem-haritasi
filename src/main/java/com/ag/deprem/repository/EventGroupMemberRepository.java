package com.ag.deprem.repository;

import com.ag.deprem.model.entity.EventGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventGroupMemberRepository extends JpaRepository<EventGroupMember, UUID> {

    Optional<EventGroupMember> findBySourceReportId(UUID sourceReportId);

    List<EventGroupMember> findByEventGroupId(UUID eventGroupId);

    @Query("SELECT m FROM EventGroupMember m JOIN FETCH m.eventGroup WHERE m.sourceReport.id = :reportId")
    Optional<EventGroupMember> findByReportIdWithGroup(@Param("reportId") UUID reportId);
}
