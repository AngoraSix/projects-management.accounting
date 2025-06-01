package com.angorasix.projects.management.accounting.presentation.router

import com.angorasix.commons.reactive.presentation.filter.extractRequestingContributor
import com.angorasix.projects.management.accounting.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.accounting.presentation.handler.ProjectManagementAccountingHandler
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.coRouter

/**
 * Router for all Project Management Integrations related endpoints.
 *
 * @author rozagerardo
 */
class ProjectManagementAccountingRouter(
    private val handler: ProjectManagementAccountingHandler,
    private val apiConfigs: ApiConfigs,
) {
    /**
     *
     * Main RouterFunction configuration for all endpoints related to ProjectManagements.
     *
     * @return the [RouterFunction] with all the routes for ProjectManagements
     */
    fun projectRouterFunction() =
        coRouter {
            apiConfigs.basePaths.projectsManagementAccounting.nest {
                filter { request, next ->
                    extractRequestingContributor(
                        request,
                        next,
                    )
                }
                apiConfigs.basePaths.baseByProjectManagementIdCrudRoute.nest {
                    defineByProjectManagementIdRoutes()
                }
            }
        }

    private fun CoRouterFunctionDsl.defineByProjectManagementIdRoutes() {
        path(apiConfigs.routes.getProjectManagementAccountingStats.path).nest {
            method(
                apiConfigs.routes.getProjectManagementAccountingStats.method,
                handler::getProjectManagementAccountingStats,
            )
        }
    }
}
