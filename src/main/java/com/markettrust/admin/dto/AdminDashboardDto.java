package com.markettrust.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private long totalUsers;
    private long totalSellers;
    private long activeProducts;
    private long totalOrders;
    private long completedOrders;
    private long pendingKyc;
    private long openReports;
    private long openDisputes;
    private long totalCreditsInCirculation;
    private long platformFeeCollected;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalSellers() { return totalSellers; }
    public void setTotalSellers(long totalSellers) { this.totalSellers = totalSellers; }

    public long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getPendingKyc() { return pendingKyc; }
    public void setPendingKyc(long pendingKyc) { this.pendingKyc = pendingKyc; }

    public long getOpenReports() { return openReports; }
    public void setOpenReports(long openReports) { this.openReports = openReports; }

    public long getOpenDisputes() { return openDisputes; }
    public void setOpenDisputes(long openDisputes) { this.openDisputes = openDisputes; }

    public long getTotalCreditsInCirculation() { return totalCreditsInCirculation; }
    public void setTotalCreditsInCirculation(long totalCreditsInCirculation) { this.totalCreditsInCirculation = totalCreditsInCirculation; }

    public long getPlatformFeeCollected() { return platformFeeCollected; }
    public void setPlatformFeeCollected(long platformFeeCollected) { this.platformFeeCollected = platformFeeCollected; }
}
