/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.imports;

/**
 *
 * @author ellenk
 */
public interface ImportUtil {
   public enum ImportType{ 
       /** ? */
       NEW, 

       /** Data is harvested from another Dataverse instance */
       HARVEST
   };
     
}
 