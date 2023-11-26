/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.storageuse;

/**
 *
 * @author landreev
 */
public class UploadSessionQuotaLimit {
        private Long totalAllocatedInBytes = 0L; 
        private Long totalUsageInBytes = 0L;
        
        public UploadSessionQuotaLimit(Long allocated, Long used) {
            this.totalAllocatedInBytes = allocated;
            this.totalUsageInBytes = used; 
        }
        
        public Long getTotalAllocatedInBytes() {
            return totalAllocatedInBytes;
        }
        
        public void setTotalAllocatedInBytes(Long totalAllocatedInBytes) {
            this.totalAllocatedInBytes = totalAllocatedInBytes;
        }
        
        public Long getTotalUsageInBytes() {
            return totalUsageInBytes;
        }
        
        public void setTotalUsageInBytes(Long totalUsageInBytes) {
            this.totalUsageInBytes = totalUsageInBytes;
        }
        
        public Long getRemainingQuotaInBytes() {
            if (totalUsageInBytes > totalAllocatedInBytes) {
                return 0L; 
            }
            return totalAllocatedInBytes - totalUsageInBytes;
        }
    }