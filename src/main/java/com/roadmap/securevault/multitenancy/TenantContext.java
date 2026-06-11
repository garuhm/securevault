package com.roadmap.securevault.multitenancy;

public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentCompanyCode = new ThreadLocal<>();

    public static void setTenantId(String tenantId) { currentTenant.set(tenantId); }
    public static String getTenantId() { return currentTenant.get(); }

    public static void setCompanyCode(String companyCode) { currentCompanyCode.set(companyCode); }
    public static String getCompanyCode() { return currentCompanyCode.get(); }

    public static void clear() {
        currentTenant.remove();
        currentCompanyCode.remove();
    }
}