/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.featured;

import com.beust.jcommander.internal.Lists;
import edu.harvard.iq.dataverse.dataverse.DataverseLinkingService;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.FeaturedDataversesSorting;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author skraffmiller
 */
@Stateless
public class FeaturedDataverseServiceBean {
    private static final Logger logger = LoggerFactory.getLogger(FeaturedDataverseServiceBean.class);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DataverseRepository dataverseRepository;
    @Inject
    private DataverseFeaturedDataverseRepository dataverseFeaturedDataverseRepository;
    @Inject
    private DataverseLinkingService linkingService;
    @Inject
    private DataverseDatasetCountService datasetCountService;

    private final List<FeaturedDataversesSorting> automaticSorting = Lists.newArrayList(
            FeaturedDataversesSorting.BY_NAME_ASC,
            FeaturedDataversesSorting.BY_NAME_DESC,
            FeaturedDataversesSorting.BY_DATASET_COUNT
    );

    // -------------------- LOGIC --------------------

    public List<Dataverse> findByDataverseId(Long dataverseId) {
        return dataverseFeaturedDataverseRepository.findByDataverseIdOrderByDisplayOrder(dataverseId);
    }

    public List<Dataverse> sortFeaturedDataverses(List<Dataverse> featuredDataverses, FeaturedDataversesSorting sorting) {
        if (sorting == FeaturedDataversesSorting.BY_NAME_ASC) {
            return featuredDataverses.stream().sorted(Comparator.comparing(Dataverse::getName)).collect(Collectors.toList());
        } else if (sorting == FeaturedDataversesSorting.BY_NAME_DESC) {
            return featuredDataverses.stream().sorted(Comparator.comparing(Dataverse::getName).reversed()).collect(Collectors.toList());
        } else if (sorting == FeaturedDataversesSorting.BY_DATASET_COUNT) {
            return orderByDatasetCount(featuredDataverses);
        } else {
            return featuredDataverses;
        }
    }

    public List<Dataverse> findByDataverseIdQuick(Long dataverseId) {
        List<Object[]> searchResults = em.createNativeQuery("SELECT d.id, d.alias, d.name, dt.logo FROM DataverseFeaturedDataverse f "
                + " JOIN dataverse d ON d.id = f.featureddataverse_id"
                + " LEFT JOIN dataversetheme dt ON dt.dataverse_id = d.id"
                + " WHERE f.dataverse_id = ?1 order by f.displayOrder")
                .setParameter(1, dataverseId)
                .getResultList();

        List<Dataverse> ret = new ArrayList<>();

        for (Object[] result : searchResults) {
            Long id = (Long) result[0];
            String alias = (String) result[1];
            String name = (String) result[2];
            String logo = (String) result[3];

            Dataverse dataverse = new Dataverse();
            dataverse.setId(id);
            dataverse.setAlias(alias);
            dataverse.setName(name);

            if (logo != null) {
                DataverseTheme theme = new DataverseTheme();
                theme.setLogo(logo);
                dataverse.setDataverseTheme(theme);
            }

            ret.add(dataverse);
        }

        return ret;
    }

    public List<Dataverse> findFeaturableDataverses(Long dataverseId) {
        List<Dataverse> featurableDataverses = new ArrayList<>();
        featurableDataverses.addAll(dataverseRepository.findPublishedByOwnerId(dataverseId));
        featurableDataverses.addAll(linkingService.findLinkedDataverses(dataverseId));
        return featurableDataverses;
    }

    public void delete(DataverseFeaturedDataverse dataverseFeaturedDataverse) {
        dataverseFeaturedDataverseRepository.mergeAndDelete(dataverseFeaturedDataverse);
    }

    public void deleteFeaturedDataversesFor(Dataverse d) {
        em.createNamedQuery("DataverseFeaturedDataverse.removeByOwnerId")
                .setParameter("ownerId", d.getId())
                .executeUpdate();
    }

    public void create(int diplayOrder, Long featuredDataverseId, Long dataverseId) {
        create(diplayOrder, dataverseRepository.getById(featuredDataverseId), dataverseId);
    }

    public void create(int diplayOrder, Dataverse featuredDataverse, Long dataverseId) {
        DataverseFeaturedDataverse dataverseFeaturedDataverse = new DataverseFeaturedDataverse();

        dataverseFeaturedDataverse.setDisplayOrder(diplayOrder);

        Dataverse dataverse = dataverseRepository.getById(dataverseId);
        dataverseFeaturedDataverse.setDataverse(dataverse);

        dataverseFeaturedDataverse.setFeaturedDataverse(featuredDataverse);

        dataverseFeaturedDataverseRepository.save(dataverseFeaturedDataverse);
    }

    public void refreshFeaturedDataversesAutomaticSorting() {
        dataverseFeaturedDataverseRepository.findByDataversesBySorting(automaticSorting)
                .forEach(dataverse -> {
                    List<Dataverse> currentOrder = findByDataverseId(dataverse.getId());
                    List<Dataverse> newOrder = findFeaturedDataversesSortedBy(dataverse.getId(), dataverse.getFeaturedDataversesSorting());

                    if (currentOrder.equals(newOrder)) {
                        return;
                    }

                    deleteFeaturedDataversesFor(dataverse);

                    for (int idx = 0; idx < newOrder.size(); idx++) {
                        create(idx, newOrder.get(idx), dataverse.getId());
                    }
                });
    }

    // -------------------- PRIVATE --------------------

    private List<Dataverse> findFeaturedDataversesSortedBy(Long dataverseId, FeaturedDataversesSorting sorting) {
        if (sorting == FeaturedDataversesSorting.BY_NAME_ASC) {
            return dataverseFeaturedDataverseRepository.findByDataverseIdOrderByNameAsc(dataverseId);
        } else if (sorting == FeaturedDataversesSorting.BY_NAME_DESC) {
            return dataverseFeaturedDataverseRepository.findByDataverseIdOrderByNameDesc(dataverseId);
        } else if (sorting == FeaturedDataversesSorting.BY_DATASET_COUNT) {
            return orderByDatasetCount(findByDataverseId(dataverseId));
        } else {
            return dataverseFeaturedDataverseRepository.findByDataverseIdOrderByDisplayOrder(dataverseId);
        }
    }

    private List<Dataverse> orderByDatasetCount(List<Dataverse> featuredDataverses) {
        Map<Long, Dataverse> dataversesLookup = featuredDataverses.stream().collect(Collectors
                .toMap(Dataverse::getId, Function.identity()));

        List<Dataverse> orderedDv = featuredDataverses.stream().findFirst()
                .map(Dataverse::getOwner)
                .map(parent -> datasetCountService.countDatasetsInChildrenOf(parent).stream()
                        .filter(c -> dataversesLookup.containsKey(c.getDataverseId()))
                        .sorted(Comparator.comparing(DataverseDatasetCount::getDatasetCount).reversed())
                        .map(c -> dataversesLookup.remove(c.getDataverseId()))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        return Stream.concat(orderedDv.stream(), dataversesLookup.values().stream()).collect(Collectors.toList());
    }
}
