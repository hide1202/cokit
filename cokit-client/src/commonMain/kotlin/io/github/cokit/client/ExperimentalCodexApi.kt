package io.github.cokit.client

/**
 * Marks CoKit APIs that model experimental `codex app-server` protocol
 * behavior.
 *
 * Experimental surfaces require explicit Kotlin API opt-in and the matching
 * app-server initialization capability opt-in before use.
 */
@RequiresOptIn(
    message = "Experimental codex app-server APIs require Kotlin API opt-in and initialization capability opt-in.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class ExperimentalCodexApi
