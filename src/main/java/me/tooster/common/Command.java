package me.tooster.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Command {

    /**
     * Aliasing the commands
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Alias {
        String[] value() default {};
    }

    /**
     * @return Returns list of commands from cached array, so the reference is always the same object
     */
    Command[] cachedValues();

    Command withArgs(String... args) {

    }
}
