package org.nustaq.reallive.sys.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * denotes a table that is not actually created + persisted but is a result of computing.
 * Used to just easily define metainformation
 *
 * if denoted at field level, it marks a field which might be rendered at client side but will not be
 * persisted
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Virtual {
}