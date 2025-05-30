package edu.harvard.iq.dataverse.datavariable;

import edu.harvard.iq.dataverse.FileMetadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VariableMetadataUtil {

    public static boolean compareVariableMetadata(FileMetadata fmdo, FileMetadata fmdn) {
        Collection<VariableMetadata> vmlo = fmdo.getVariableMetadatas();
        Collection<VariableMetadata> vmln = fmdn.getVariableMetadatas();
    
        if (vmlo.size() != vmln.size()) {
            return false;
        }
    
        Map<Long, VariableMetadata> vmnMap = new HashMap<>();
        for (VariableMetadata vmn : vmln) {
            vmnMap.put(vmn.getDataVariable().getId(), vmn);
        }
    
        for (VariableMetadata vmo : vmlo) {
            Long id = vmo.getDataVariable().getId();
            VariableMetadata vmn = vmnMap.get(id);
            if (vmn == null || !compareVarMetadata(vmo, vmn)) {
                return false;
            }
        }
    
        return true;
    }

    public static boolean  compareVarMetadata(VariableMetadata vmOld, VariableMetadata vmNew) {
        boolean thesame = true;

        if (checkDiffEmpty(vmOld.getNotes(), vmNew.getNotes())) {
            thesame = false;
        } else if (checkDiffEmpty(vmNew.getNotes(), vmOld.getNotes())) {
            thesame = false;
        }else if (checkDiff(vmOld.getNotes(), vmNew.getNotes())) {
            thesame = false;
        } else if ( checkDiffEmpty(vmOld.getUniverse(), vmNew.getUniverse())) {
            thesame = false;
        } else if (checkDiffEmpty(vmNew.getUniverse(), vmOld.getUniverse())) {
            thesame = false;
        } else if (checkDiff(vmOld.getUniverse(),vmNew.getUniverse())) {
            thesame = false;
        } else if (checkDiffEmpty(vmOld.getInterviewinstruction(),vmNew.getInterviewinstruction())) {
            thesame = false;
        } else if ( checkDiffEmpty(vmNew.getInterviewinstruction(),vmOld.getInterviewinstruction())) {
            thesame = false;
        } else if (checkDiff(vmOld.getInterviewinstruction(),vmNew.getInterviewinstruction())) {
            thesame = false;
        } else  if (checkDiffEmpty(vmOld.getLiteralquestion(),vmNew.getLiteralquestion())) {
            thesame = false;
        } else if (checkDiffEmpty(vmNew.getLiteralquestion(),vmOld.getLiteralquestion())) {
            thesame = false;
        } else if (checkDiff(vmOld.getLiteralquestion(),vmNew.getLiteralquestion())) {
            thesame = false;
        } else if (checkDiff(vmOld.getPostquestion(),vmNew.getPostquestion())) {
            thesame = false;
        } else if (checkDiffEmpty(vmNew.getPostquestion(),vmOld.getPostquestion())) {
            thesame = false;
        } else if (checkDiff(vmOld.getPostquestion(),vmNew.getPostquestion())) {
            thesame = false;
        } else  if (checkDiffEmpty(vmOld.getLabel(),vmNew.getLabel())) {
            thesame = false;
        } else if  (checkDiffEmpty(vmNew.getLabel(),vmOld.getLabel())) {
            thesame = false;
        } else if (checkDiff(vmOld.getLabel(),vmNew.getLabel())) {
            thesame = false;
        } else if (vmOld.isIsweightvar() != vmNew.isIsweightvar() ) {
            thesame = false;
        } else if (vmOld.isWeighted() != vmNew.isWeighted()) {
            thesame = false;
        } else if (vmOld.isWeighted() == vmNew.isWeighted()) {
            if (vmOld.isWeighted() ){
                Long oldWeightId = vmOld.getWeightvariable().getId();
                Long newWeightId = vmNew.getWeightvariable().getId();
                if ( !oldWeightId.equals(newWeightId) ) {
                    thesame = false;
                }
            }
        }

        return thesame;

    }

    public static boolean checkDiffEmpty(String str1, String str2) {
        if (str1 == null && str2 != null && !str2.trim().equals("")) {
            return true;
        }
        return false;

    }
    public static boolean checkDiff(String str1, String str2) {
        if (str1 != null && str2 != null && !str1.equals(str2)) {
            return true;
        }
        return false;
    }
}
