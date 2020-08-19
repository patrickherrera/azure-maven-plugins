/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.rest.LogLevel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class SpringServiceClient {

    protected static final String NO_CLUSTER = "No cluster named %s found in subscription %s";

    private String subscriptionId;
    private AppPlatformManager springManager;

    public SpringServiceClient(AzureTokenCredentials azureTokenCredentials, String subscriptionId, String userAgent) {
        this(azureTokenCredentials, subscriptionId, userAgent, LogLevel.NONE);
    }

    public SpringServiceClient(AzureTokenCredentials azureTokenCredentials, String subscriptionId, String userAgent, LogLevel logLevel) {
        subscriptionId = StringUtils.isEmpty(subscriptionId) ? azureTokenCredentials.defaultSubscriptionId() : subscriptionId;
        this.subscriptionId = subscriptionId;
        this.springManager = AppPlatformManager.configure()
                .withLogLevel(logLevel)
                .withUserAgent(userAgent)
                .authenticate(azureTokenCredentials, subscriptionId);
    }

    public SpringAppClient newSpringAppClient(String subscriptionId, String cluster, String app) {
        final SpringAppClient.Builder builder = new SpringAppClient.Builder();
        return builder.withSubscriptionId(subscriptionId)
                .withSpringServiceClient(this)
                .withClusterName(cluster)
                .withAppName(app)
                .build();
    }

    public SpringAppClient newSpringAppClient(SpringConfiguration configuration) {
        return newSpringAppClient(configuration.getSubscriptionId(), configuration.getClusterName(), configuration.getAppName());
    }

    public List<ServiceResourceInner> getAvailableClusters() {
        final PagedList<ServiceResourceInner> clusterList = getSpringManager().inner().services().list();
        clusterList.loadAll();
        return new ArrayList<>(clusterList);
    }

    public ServiceResourceInner getClusterByName(String cluster) {
        final List<ServiceResourceInner> clusterList = getAvailableClusters();
        return clusterList.stream().filter(appClusterResourceInner -> appClusterResourceInner.name().equals(cluster))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(String.format(NO_CLUSTER, cluster, subscriptionId)));
    }

    public String getResourceGroupByCluster(String clusterName) {
        final ServiceResourceInner cluster = getClusterByName(clusterName);
        final String[] attributes = cluster.id().split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public AppPlatformManager getSpringManager() {
        return springManager;
    }
}
