package com.angorasix.projects.management.accounting.infrastructure.eventsourcing.repository

import com.angorasix.projects.management.accounting.infrastructure.eventsourcing.querymodel.ContributorAccountView
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

interface ContributorAccountViewRepository :
    CoroutineCrudRepository<ContributorAccountView, String>,
    CoroutineSortingRepository<ContributorAccountView, String>
