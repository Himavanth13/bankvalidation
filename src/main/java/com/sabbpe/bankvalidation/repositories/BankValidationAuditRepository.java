package com.sabbpe.bankvalidation.repositories;

import com.sabbpe.bankvalidation.entity.BankValidationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankValidationAuditRepository extends JpaRepository<BankValidationAudit, Long> {
}
