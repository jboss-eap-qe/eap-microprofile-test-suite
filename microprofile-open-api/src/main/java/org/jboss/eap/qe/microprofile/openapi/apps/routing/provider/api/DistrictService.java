package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api;

import java.util.List;

import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;

/**
 * Defines the contract for implementing a service providing operations to manage {@link District} entities
 */
public interface DistrictService {

    /**
     * Retrieves and returns all available districts
     *
     * @return List including all {@link District} instances available
     */
    List<District> getAll();

    /**
     * Retrieves and returns an instance of {@link District} uniquely identified by the given "code" parameter
     *
     * @param code String that uniquely identifies a District
     * @return Instance of {@link District} which matches the given code, null if no matching element is found
     */
    District getByCode(String code);

    /**
     * Updates a given District data
     *
     * @param code String that uniquely identifies a District
     * @param data Provided {@link District} instance used to update existing data
     * @return Instance of {@link District} which matches the given code
     */
    District update(String code, District data);
}
