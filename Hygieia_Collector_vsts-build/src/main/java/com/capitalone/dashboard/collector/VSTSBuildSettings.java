package com.capitalone.dashboard.collector;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bean to hold settings specific to the Bamboo collector.
 */
@Component
@ConfigurationProperties(prefix = "vsts-build")
public class VSTSBuildSettings {


    private String cron;
    private boolean saveLog = false;
    private List<String> servers;
    private List<String> niceNames;
    private String username;
    private String apiKey;
    private String dockerLocalHostIP; //null if not running in docker on http://localhost
    
	private String proxyHost;
    private String proxyPort;
    private String nonProxy;
    
    

    public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getNonProxy() {
		return nonProxy;
	}

	public void setNonProxy(String nonProxy) {
		this.nonProxy = nonProxy;
	}

	public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public boolean isSaveLog() {
        return saveLog;
    }

    public void setSaveLog(boolean saveLog) {
        this.saveLog = saveLog;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void setDockerLocalHostIP(String dockerLocalHostIP) {
        this.dockerLocalHostIP = dockerLocalHostIP;
    }

    public List<String> getNiceNames() {
        return niceNames;
    }

    public void setNiceNames(List<String> niceNames) {
        this.niceNames = niceNames;
    }

	//Docker NATs the real host localhost to 10.0.2.2 when running in docker
	//as localhost is stored in the JSON payload from jenkins we need
	//this hack to fix the addresses
    public String getDockerLocalHostIP() {
    	
    		//we have to do this as spring will return NULL if the value is not set vs and empty string
    	String localHostOverride = "";
    	if (dockerLocalHostIP != null) {
    		localHostOverride = dockerLocalHostIP;
    	}
        return localHostOverride;
    }
}
