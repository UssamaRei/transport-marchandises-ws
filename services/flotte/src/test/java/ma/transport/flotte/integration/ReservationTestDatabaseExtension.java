package ma.transport.flotte.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class ReservationTestDatabaseExtension implements ExecutionCondition {
    private static final String DATABASE_URL = "FLOTTE_TEST_DATABASE_URL";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (System.getenv(DATABASE_URL) != null && !System.getenv(DATABASE_URL).isBlank()) {
            return ConditionEvaluationResult.enabled("An isolated Flotte test PostgreSQL was configured");
        }
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available for PostgreSQL Testcontainers");
            }
        } catch (RuntimeException exception) {
            return ConditionEvaluationResult.disabled(
                    "PostgreSQL integration tests require Docker or FLOTTE_TEST_DATABASE_URL");
        }
        return ConditionEvaluationResult.disabled(
                "PostgreSQL integration tests require Docker or FLOTTE_TEST_DATABASE_URL");
    }
}
