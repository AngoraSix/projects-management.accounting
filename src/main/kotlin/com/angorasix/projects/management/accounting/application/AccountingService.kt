package com.angorasix.projects.management.accounting.application

import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import org.springframework.cloud.stream.function.StreamBridge

/**
 *
 *
 * @author rozagerardo
 */
class AccountingService(
//    private val repository: IntegrationAssetRepository,
    private val streamBridge: StreamBridge,
    private val amqpConfigs: AmqpConfigurations,
)
