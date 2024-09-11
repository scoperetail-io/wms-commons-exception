/*
 *  ExceptionConfig.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration class for handling exceptions in the application.
 *
 * <p>This class is annotated with {@code @Configuration} to indicate that it contains configuration
 * for the Spring application context. The {@code @ComponentScan} annotation enables component scanning
 * for automatic discovery of Spring beans within the specified packages.
 *
 * <p>This configuration class is typically used to enable the automatic detection of components, such as
 * exception handlers or related classes, through component scanning.
 */
@Configuration
@ComponentScan
public class ExceptionConfig {
}
