package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;

/**
 * In-memory implementation of {@link DistrictService}
 */
@ApplicationScoped
public class InMemoryDistrictService implements DistrictService {

    private final static List<District> DISTRICTS = new ArrayList<>();

    @PostConstruct
    private void populateDistricts() {
        DISTRICTS.add(new DistrictEntity("DN", "Northern district"));
        DISTRICTS.add(new DistrictEntity("DS", "Southern district"));
        DISTRICTS.add(new DistrictEntity("DW", "Western district"));
        DISTRICTS.add(new DistrictEntity("DE", "Eastern district"));
    }

    @Override
    public List<District> getAll() {
        return DISTRICTS;
    }

    @Override
    public District getByCode(String code) {
        return DISTRICTS.stream().filter(d -> d.getCode().equals(code)).findFirst().orElse(null);
    }

    @Override
    public District update(String code, District data) {
        District old = getByCode(code);
        if (old == null)
            throw new IllegalArgumentException(String.format("District %s no found"));
        old.setName(data.getName());
        old.setObsolete(data.getObsolete());
        return old;
    }
}
