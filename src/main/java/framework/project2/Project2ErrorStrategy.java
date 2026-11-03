package framework.project2;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;

/**
 * Project 2 diagnostic adaptation for ANTLR recovery.
 *
 * ANTLR's DefaultErrorStrategy is still responsible for the actual recovery
 * algorithm. Do NOT reimplement recoverInline(), single-token insertion, or
 * single-token deletion.
 *
 * Your task is to adapt ANTLR's missing-token and unwanted-token reports to
 * ErrorReporter using the line-number rules defined in the Project 2 spec.
 */
public final class Project2ErrorStrategy extends DefaultErrorStrategy {

    private final ErrorReporter reporter;

    public Project2ErrorStrategy(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        int tokenType = getExpectedTokens(recognizer).getMinElement();
        reporter.reportMissing(tokenType, recognizer.getCurrentToken().getLine() - 1);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (inErrorRecoveryMode(recognizer)) {
            return;
        }

        beginErrorCondition(recognizer);

        reporter.reportExtraneous(
                recognizer.getCurrentToken().getType(),
                recognizer.getCurrentToken().getLine() - 1);
    }
}
