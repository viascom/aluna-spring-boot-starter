package io.viascom.discord.bot.aluna.configuration

import java.lang.annotation.Inherited

/**
 * Annotation that specifies that an element is experimental and may change without notice.
 *
 * NOTE: Using it is not suggested for production environment.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FILE,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.TYPE_PARAMETER
)
@Inherited
annotation class Experimental(val description: String)
