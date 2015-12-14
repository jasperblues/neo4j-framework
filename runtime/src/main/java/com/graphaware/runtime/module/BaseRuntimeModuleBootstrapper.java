/*
 * Copyright (c) 2013-2015 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.runtime.module;

import com.graphaware.common.policy.*;
import com.graphaware.runtime.config.BaseTxDrivenModuleConfiguration;
import com.graphaware.runtime.config.TxDrivenModuleConfiguration;
import com.graphaware.runtime.config.function.StringToNodeInclusionPolicy;
import com.graphaware.runtime.config.function.StringToNodePropertyInclusionPolicy;
import com.graphaware.runtime.config.function.StringToRelationshipInclusionPolicy;
import com.graphaware.runtime.config.function.StringToRelationshipPropertyInclusionPolicy;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract base-class for {@link RuntimeModuleBootstrapper} implementations for {@link TxDrivenModule}s.
 *
 * @param <C> type of {@link TxDrivenModuleConfiguration} used to configure the module.
 */
public abstract class BaseRuntimeModuleBootstrapper<C extends BaseTxDrivenModuleConfiguration<C>> implements RuntimeModuleBootstrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BaseRuntimeModuleBootstrapper.class);

    protected static final String INITIALIZE_UNTIL = "initializeUntil";

    protected static final String NODES = "nodes";
    protected static final String NODE_PROPERTIES = "node.properties";
    protected static final String RELATIONSHIPS = "relationships";
    protected static final String RELATIONSHIP_PROPERTIES = "relationship.properties";

    /**
     * Produce default configuration for the module.
     *
     * @return default config.
     */
    protected abstract C defaultConfiguration();

    /**
     * {@inheritDoc}
     */
    @Override
    public RuntimeModule bootstrapModule(String moduleId, Map<String, String> config, GraphDatabaseService database) {
        C configuration = defaultConfiguration();

        if (configExists(config, INITIALIZE_UNTIL)) {
            configuration = configuration.withInitializeUntil(Long.valueOf(config.get(INITIALIZE_UNTIL)));
            logInitUntil(moduleId, configuration);
        }

        if (configExists(config, NODES)) {
            NodeInclusionPolicy policy = StringToNodeInclusionPolicy.getInstance().apply(config.get(NODES));
            LOG.info("Nodes Inclusion Policy set to {}", policy);
            configuration = configuration.with(policy);
        }

        if (configExists(config, NODE_PROPERTIES)) {
            NodePropertyInclusionPolicy policy = StringToNodePropertyInclusionPolicy.getInstance().apply(config.get(NODE_PROPERTIES));
            LOG.info("Node Properties Inclusion Policy set to {}", policy);
            configuration = configuration.with(policy);
        }

        if (configExists(config, RELATIONSHIPS)) {
            RelationshipInclusionPolicy policy = StringToRelationshipInclusionPolicy.getInstance().apply(config.get(RELATIONSHIPS));
            LOG.info("Relationships Inclusion Policy set to {}", policy);
            configuration = configuration.with(policy);
        }

        if (configExists(config, RELATIONSHIP_PROPERTIES)) {
            RelationshipPropertyInclusionPolicy policy = StringToRelationshipPropertyInclusionPolicy.getInstance().apply(config.get(RELATIONSHIP_PROPERTIES));
            LOG.info("Relationship Properties Inclusion Policy set to {}", policy);
            configuration = configuration.with(policy);
        }

        return doBootstrapModule(moduleId, config, database, configuration);
    }

    /**
     * Apply module-specific configuration to the provided configuration, which has already been configured with "initializeUntil"
     * and all {@link InclusionPolicies}. Then bootstrap the module and return it.
     *
     * @param moduleId      ID of the module.
     * @param config        for this module as key-value pairs.
     * @param database      which the module will run on.
     * @param configuration pre-populated with configuration common for all modules, such as "initializeUntil" and all {@link InclusionPolicies}.
     * @return fully configured runtime module.
     */
    protected abstract RuntimeModule doBootstrapModule(String moduleId, Map<String, String> config, GraphDatabaseService database, C configuration);

    /**
     * Check if a configuration has been specified.
     *
     * @param config config passed in to the bootstrapper.
     * @param key    to check for.
     * @return true iff the passed in config contains the key and the value is not empty.
     */
    protected final boolean configExists(Map<String, String> config, String key) {
        return config.get(key) != null && config.get(key).length() > 0;
    }

    private void logInitUntil(String moduleId, C configuration) {
        LOG.info(moduleId + " (re-)initialize until set to {}", initUntilAsString(configuration));
        if (configuration.initializeUntil() != TxDrivenModuleConfiguration.ALWAYS && configuration.initializeUntil() != TxDrivenModuleConfiguration.NEVER) {
            long now = System.currentTimeMillis();
            LOG.info("That's " + Math.abs(now - configuration.initializeUntil()) + " ms in the " + (now > configuration.initializeUntil() ? "past" : "future"));
        }
    }

    private String initUntilAsString(C configuration) {
        if (configuration.initializeUntil() == TxDrivenModuleConfiguration.NEVER) {
            return "NEVER";
        }
        if (configuration.initializeUntil() == TxDrivenModuleConfiguration.ALWAYS) {
            return "ALWAYS";
        }
        return String.valueOf(configuration.initializeUntil());
    }
}
