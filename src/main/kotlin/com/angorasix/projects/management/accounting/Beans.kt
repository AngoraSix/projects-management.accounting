package com.angorasix.projects.management.accounting

import com.angorasix.projects.management.accounting.application.IntegrationAccountingService
import com.angorasix.projects.management.accounting.infrastructure.security.ProjectManagementIntegrationsSecurityConfiguration
import com.angorasix.projects.management.accounting.messaging.handler.ProjectsManagementAccountingMessagingHandler
import com.angorasix.projects.management.accounting.presentation.handler.ProjectManagementAccountingHandler
import com.angorasix.projects.management.accounting.presentation.router.ProjectManagementAccountingRouter
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

val beans = beans {
    bean {
        ProjectManagementIntegrationsSecurityConfiguration.springSecurityFilterChain(ref())
    }
    bean<ProjectManagementAccountingHandler>()
    bean<ProjectsManagementAccountingMessagingHandler>()
    bean<IntegrationAccountingService>()
    bean {
        ProjectManagementAccountingRouter(ref(), ref()).projectRouterFunction()
    }
}

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) =
        beans.initialize(context)
}
