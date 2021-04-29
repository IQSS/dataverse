/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataverse.DataverseLinkingService;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * @author skraffmiller
 */
@Stateless
public class FeaturedDataverseServiceBean {
    private static final Logger logger = Logger.getLogger(FeaturedDataverseServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DataverseRepository dataverseRepository;
    @Inject
    private DataverseFeaturedDataverseRepository dataverseFeaturedDataverseRepository;
    @Inject
    private DataverseLinkingService linkingService;
    
    @EJB
    DataverseDao dataverseDao;

    public List<Dataverse> findByDataverseId(Long dataverseId) {
        return dataverseFeaturedDataverseRepository.findByDataverseId(dataverseId).stream()
                .map(DataverseFeaturedDataverse::getFeaturedDataverse)
                .collect(toList());
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
        DataverseFeaturedDataverse dataverseFeaturedDataverse = new DataverseFeaturedDataverse();

        dataverseFeaturedDataverse.setDisplayOrder(diplayOrder);

        Dataverse dataverse = dataverseRepository.getById(dataverseId);
        dataverseFeaturedDataverse.setDataverse(dataverse);

        Dataverse featuredDataverse = dataverseRepository.getById(featuredDataverseId);
        dataverseFeaturedDataverse.setFeaturedDataverse(featuredDataverse);

        dataverseFeaturedDataverseRepository.save(dataverseFeaturedDataverse);
    }


}
