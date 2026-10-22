package pharmacy.common.util;

import pharmacy.common.exception.ValidationException;

/**
 * Functional interface representing a single validation rule applied to a string value.
 *
 * Demonstrates: Abstraction and functional programming in Java.
 */
@FunctionalInterface
public interface InputRule {

    /**
     * Checks whether the given string value satisfies the rule.
     *
     * @param value the raw or trimmed string to test
     * @throws ValidationException if the rule is violated
     */
    void check(String value) throws ValidationException;
}
