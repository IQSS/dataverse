package edu.harvard.iq.dataverse;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuxiliaryFileServiceBeanTest {

    EntityManager em;
    AuxiliaryFileServiceBean svc;
    TypedQuery<String> query;
    List<String> types;
    DataFile dataFile;

    @BeforeEach
    public void setup() {
        svc = new AuxiliaryFileServiceBean();
        svc.em = mock(EntityManager.class);
        query = mock(TypedQuery.class);
        types = Arrays.asList("DP", "GEOSPATIAL", "NEXT_BIG_THING", "FUTURE_FUN");
        dataFile = new DataFile();
        dataFile.setId(42l);
    }

    @Test
    public void testFindAuxiliaryFileTypesInBundleFalse() {
        System.out.println("testFindAuxiliaryFileTypesInBundleFalse");
        boolean inBundle = false;
        when(this.svc.em.createNamedQuery(ArgumentMatchers.anyString(),ArgumentMatchers.<Class<String>>any())).thenReturn(query);
        when(query.getResultList()).thenReturn(types);
        List<String> result = svc.findAuxiliaryFileTypes(dataFile, inBundle);
        // None of these are in the bundle.
        List<String> expected = Arrays.asList("GEOSPATIAL", "NEXT_BIG_THING", "FUTURE_FUN");
        assertEquals(expected, result);
    }

    @Test
    public void testFindAuxiliaryFileTypesInBundleTrue() {
        System.out.println("testFindAuxiliaryFileTypesInBundleTrue");
        boolean inBundle = true;
        when(this.svc.em.createNamedQuery(ArgumentMatchers.anyString(),ArgumentMatchers.<Class<String>>any())).thenReturn(query);
        when(query.getResultList()).thenReturn(types);
        List<String> result = svc.findAuxiliaryFileTypes(dataFile, inBundle);
        // DP is in the bundle.
        List<String> expected = Arrays.asList("DP");
        assertEquals(expected, result);
    }

}
