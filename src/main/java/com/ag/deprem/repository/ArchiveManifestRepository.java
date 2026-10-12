package com.ag.deprem.repository;

import com.ag.deprem.model.entity.ArchiveManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchiveManifestRepository extends JpaRepository<ArchiveManifest, UUID> {
    Optional<ArchiveManifest> findByArchiveVersion(String archiveVersion);
}
