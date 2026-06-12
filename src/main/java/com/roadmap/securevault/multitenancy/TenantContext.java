package com.roadmap.securevault.multitenancy;

public class TenantContext {
    private static final ThreadLocal<String> currentTenantSchema = new ThreadLocal<>();
    private static final ThreadLocal<String> currentCompanyCode = new ThreadLocal<>();

    public static void setTenantSchema(String tenantSchema) { currentTenantSchema.set(tenantSchema); }
    public static String getTenantSchema() { return currentTenantSchema.get(); }

    public static void setCompanyCode(String companyCode) { currentCompanyCode.set(companyCode); }
    public static String getCompanyCode() { return currentCompanyCode.get(); }

    public static void clear() {
        currentTenantSchema.remove();
        currentCompanyCode.remove();
    }
}