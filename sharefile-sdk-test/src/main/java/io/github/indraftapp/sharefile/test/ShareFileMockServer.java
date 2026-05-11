package io.github.indraftapp.sharefile.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Starts a dynamic-port WireMock server and exposes it plus ShareFile test properties to Spring
 * tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = ShareFileMockServerConfiguration.class,
    initializers = ShareFileMockServerInitializer.class)
public @interface ShareFileMockServer {}
