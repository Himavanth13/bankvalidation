package com.sabbpe.bankvalidation.repositories;

import com.sabbpe.bankvalidation.entity.TransactionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionTokenRepository extends JpaRepository<TransactionToken, String> {

	Optional<TransactionToken> findByTransactionToken(String transactionToken);

	@Query(value = """
	SELECT * FROM master_transactions
	WHERE client_id = :clientId
	  AND transaction_timestamp = :timestamp
	  AND processor = :processor
	ORDER BY initiated_at DESC
	LIMIT 1
	""", nativeQuery = true)
	Optional<TransactionToken> findLatestByClientIdAndTransactionTimestampAndProcessor(
			@Param("clientId") String clientId,
			@Param("timestamp") LocalDateTime timestamp,
			@Param("processor") String processor
	);
}