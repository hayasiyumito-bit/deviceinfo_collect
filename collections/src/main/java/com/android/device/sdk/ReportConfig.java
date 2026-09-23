package com.android.device.sdk;

/**
 * 上报配置。宿主 App(如 YumyHook 管理器)构造后交给 {@link DeviceCollectSdk}。
 *
 * <p>典型用法：
 * <pre>{@code
 * ReportConfig cfg = new ReportConfig("http://47.93.79.211:8000", "<api-key>")
 *         .appPackage(BuildConfig.APPLICATION_ID)
 *         .appVersion(BuildConfig.VERSION_NAME)
 *         .gzip(true);
 * DeviceCollectSdk.collectAndReport(context, cfg);
 * }</pre>
 */
public final class ReportConfig {

    private final String baseUrl;
    private final String apiKey;
    private String reportPath = "/api/v1/report";
    private String appPackage;
    private String appVersion;
    private boolean gzip = true;
    private boolean includeRisk = false;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 20_000;
    private long rootProbeWaitMs = 2_000L;

    /**
     * @param baseUrl 服务端根地址，如 {@code http://47.93.79.211:8000}(不含路径)
     * @param apiKey  服务端 {@code COLLECTSVC_API_KEY},作为 {@code X-Api-Key} 头发送
     */
    public ReportConfig(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl required");
        }
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    public ReportConfig reportPath(String path) { this.reportPath = path; return this; }
    public ReportConfig appPackage(String pkg) { this.appPackage = pkg; return this; }
    public ReportConfig appVersion(String ver) { this.appVersion = ver; return this; }
    public ReportConfig gzip(boolean enabled) { this.gzip = enabled; return this; }

    /**
     * 是否在上报里包含风控检测。默认 {@code false}。
     * <p>置 {@code true} 前请阅读 {@link DeviceCollectSdk#collect(android.content.Context, long, boolean)}
     * 的警告：静默后台执行风控会触发 phantom process killer 导致宿主闪退。
     */
    public ReportConfig includeRisk(boolean enabled) { this.includeRisk = enabled; return this; }
    public ReportConfig connectTimeoutMs(int ms) { this.connectTimeoutMs = ms; return this; }
    public ReportConfig readTimeoutMs(int ms) { this.readTimeoutMs = ms; return this; }
    public ReportConfig rootProbeWaitMs(long ms) { this.rootProbeWaitMs = ms; return this; }

    public String endpoint() { return baseUrl + reportPath; }
    public String apiKey() { return apiKey; }
    public String appPackage() { return appPackage; }
    public String appVersion() { return appVersion; }
    public boolean gzip() { return gzip; }
    public boolean includeRisk() { return includeRisk; }
    public int connectTimeoutMs() { return connectTimeoutMs; }
    public int readTimeoutMs() { return readTimeoutMs; }
    public long rootProbeWaitMs() { return rootProbeWaitMs; }
}
