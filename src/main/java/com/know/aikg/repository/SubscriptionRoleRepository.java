package com.know.aikg.repository;

import com.know.aikg.entity.SubscriptionRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRoleRepository extends JpaRepository<SubscriptionRole, String> {
    List<SubscriptionRole> findByStatus(Boolean status);
    List<SubscriptionRole> findByReaderEmail(String readerEmail);
    List<SubscriptionRole> findByArea(String area);
} 