package com.capitalone.dashboard.collector;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.model.SCM;
import com.capitalone.dashboard.model.VSTSBuild;
import com.capitalone.dashboard.model.VSTSBuildChanges;
import com.capitalone.dashboard.model.VSTSBuildChangesResponse;
import com.capitalone.dashboard.model.VSTSBuildDefinition;
import com.capitalone.dashboard.model.VSTSBuildDefinitionResponse;
import com.capitalone.dashboard.model.VSTSBuildJob;
import com.capitalone.dashboard.model.VSTSBuildResponse;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.util.Supplier;

/**
 * BambooClient implementation that uses RestTemplate and JSONSimple to fetch
 * information from Bamboo instances.
 */
@Component
public class DefaultVSTSBuildClient implements VSTSBuildClient {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultVSTSBuildClient.class);

	private final RestOperations rest;
	private final VSTSBuildSettings settings;
	private final BuildRepository buildRepository;

	private static final String JOBS_URL_SUFFIX = "/_apis/build/definitions?api-version=4.1";
	private static final String JOBS_RESULT_SUFFIX = "/_apis/build/builds?definitions=${definitionsId}&statusFilter=completed&api-version=4.1&minTime=${minTime}";
	private static final String BUILD_DETAILS_URL_SUFFIX_CHANGES = "_apis/build/builds/${buildId}/changes?api-version=4.1&$top=500";
	private static final String REGEX_CONST = ".*";
	private static final int FIRST_RUN_HISTORY_DEFAULT = 110;
	private static final String DATE_FORMAT_QUERY_VSTS = "MM/dd/yyyy'%20'HH:mm:ss";
	
	@Autowired
	public DefaultVSTSBuildClient(Supplier<RestOperations> restOperationsSupplier, VSTSBuildSettings settings,
			BuildRepository buildRepository) {
		this.rest = restOperationsSupplier.get();
		this.settings = settings;
		this.buildRepository = buildRepository;
		System.getProperties().put("http.proxyHost", this.settings.getProxyHost());
		System.getProperties().put("http.proxyPort", this.settings.getProxyPort());
		System.getProperties().put("https.proxyHost", this.settings.getProxyHost());
		System.getProperties().put("https.proxyPort", this.settings.getProxyPort());
		System.getProperties().put("http.nonProxyHosts", this.settings.getNonProxy());
	}

	@Override
	public List<VSTSBuildJob> getInstanceJobs(String instanceUrl) {

		List<VSTSBuildJob> buildDefinitionsJob = new ArrayList<>();
		try {

			String url = joinURL(instanceUrl, JOBS_URL_SUFFIX);
			ResponseEntity<VSTSBuildDefinitionResponse> responseEntity = makeRestCall(url,
					VSTSBuildDefinitionResponse.class);
			
			if(responseEntity == null) {
				return buildDefinitionsJob;
			}
			
			List<VSTSBuildDefinition> responseBuildDefinitions = responseEntity.getBody().getValue();

			buildDefinitionsJob = responseBuildDefinitions.stream()
					.filter(definition -> definition.getName().toUpperCase().matches(REGEX_CONST))
					.map(definition -> {

						VSTSBuildJob vSTSBuildJob = new VSTSBuildJob();
						vSTSBuildJob.setInstanceUrl(instanceUrl);
						vSTSBuildJob.setJobName(definition.getName());
						vSTSBuildJob.setJobUrl(definition.getUrl());
						vSTSBuildJob.setJobId(definition.getId());

						return vSTSBuildJob;

					}).collect(Collectors.toList());

			return buildDefinitionsJob;

		} catch (MalformedURLException mfe) {
			LOG.error("malformed url for loading jobs", mfe);
		} catch (Exception e) {
			LOG.error("Parsing build: " + instanceUrl, e);
			throw e;
		}

		return buildDefinitionsJob;
	}
	
	@Override
	public List<Build> getBuildDetails(String instanceUrl, VSTSBuildJob job) {

		List<Build> builds = new ArrayList<>();

		try {

			String urlBuilds = JOBS_RESULT_SUFFIX.replace("${definitionsId}", job.getJobId())
					.replace("${minTime}", getDateForCommits(0));
			String resultUrlBuilds = joinURL(instanceUrl, urlBuilds);
			ResponseEntity<VSTSBuildResponse> responseEntity = makeRestCall(resultUrlBuilds,VSTSBuildResponse.class);
			List<VSTSBuild> responseBuild = responseEntity.getBody().getValue();
			
			builds = responseBuild
					.stream()
				    .filter(build -> 
				  		isNewBuild(buildRepository, job, build.getId())
				  		&& (StringUtils.isNotBlank(build.getFinishTime()) && !build.getFinishTime().equals("0"))
				  		&& (StringUtils.isNotBlank(build.getStartTime()) && !build.getStartTime().equals("0"))
	  		        )
				    .map(buildnew -> {
				    
			    	LOG.info("Analysing build id " + buildnew.getId());
				
					Build build = new Build();
					build.setCollectorItemId(job.getId());
					build.setTimestamp(System.currentTimeMillis());
					build.setNumber(buildnew.getId());
					build.setBuildUrl(buildnew.get_links().getWeb().getHref());
					try {
						
						long startTime = new DateTime(buildnew.getStartTime()).getMillis();
						long finishTime = new DateTime(buildnew.getFinishTime()).getMillis();
						
						build.setStartTime(startTime);
						build.setEndTime(finishTime);
						build.setDuration(build.getEndTime() - build.getStartTime());
					
					} catch (NumberFormatException e) {
						LOG.error("Parsing date: " + instanceUrl + "plan id" + job.getJobId() + "plan id" + buildnew.getId() , e);
					}
					
					build.setBuildStatus(getBuildStatus(buildnew.getResult()));
					build.setStartedBy(buildnew.getRequestedFor().getDisplayName());
					build.setLog(buildnew.getBuildNumber());
					
					try {
						addChangeSets(build, instanceUrl);
					} catch (Exception e) {
						return null;
					}
					
					return build;
				
			}).collect(Collectors.toList());
				

		} catch (Exception e) {
			LOG.error("Parsing build: " + instanceUrl + "plan id" + job.getJobId(), e);
		}

		return builds;
	}
	
	private String getDateForCommits(long lastUpdate) {
		lastUpdate = 0;
		Date dt;
		if (lastUpdate == 0) {
			dt = getDate(new Date(), -FIRST_RUN_HISTORY_DEFAULT, 0);
		} else {
			dt = getDate(new Date(lastUpdate), 0, -10);
		}
		DateFormat df = new SimpleDateFormat(DATE_FORMAT_QUERY_VSTS);
		return df.format(dt);
	}
	
	private Date getDate(Date dateInstance, int offsetDays, int offsetMinutes) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateInstance);
		cal.add(Calendar.DATE, offsetDays);
		cal.add(Calendar.MINUTE, offsetMinutes);
		return cal.getTime();
	}

	private boolean isNewBuild(BuildRepository buildRepository, VSTSBuildJob job, String buildId) {
		return buildRepository.findByCollectorItemIdAndNumber(job.getId(), buildId) == null;
	}

	// This method will rebuild the API endpoint because the buildUrl obtained via
	// Jenkins API
	// does not save the auth user info and we need to add it back.
	public static String rebuildJobUrl(String build, String server)
			throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
		URL instanceUrl = new URL(server);
		String userInfo = instanceUrl.getUserInfo();
		String instanceProtocol = instanceUrl.getProtocol();

		// decode to handle spaces in the job name.
		URL buildUrl = new URL(URLDecoder.decode(build, "UTF-8"));
		String buildPath = buildUrl.getPath();

		String host = buildUrl.getHost();
		int port = buildUrl.getPort();
		URI newUri = new URI(instanceProtocol, userInfo, host, port, buildPath, null, null);
		return newUri.toString();
	}

	/**
	 * Grabs changeset information for the given build.
	 *
	 * @param build     a Build
	 * @param buildJson the build JSON object
	 * @throws Exception 
	 */
	private void addChangeSets(Build build, String instanceUrl) throws Exception {

		String urlBuild = BUILD_DETAILS_URL_SUFFIX_CHANGES.replace("${buildId}", build.getNumber());
		ResponseEntity<VSTSBuildChangesResponse> responseEntity = null;
		
			int attempts = 0;
			
			Exception oe = null;
			while (attempts < 3) {
				attempts++;
				try {
					String urlNew = joinURL(instanceUrl, urlBuild);
					responseEntity = makeRestCall(urlNew, VSTSBuildChangesResponse.class);
					
					if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
						break;
					}
				} catch (MalformedURLException mfe) {
					LOG.error("Malformed url for loading build details" + mfe.getMessage() + ". URL =" + build.getBuildUrl());
				} catch (Exception e) {
					oe = e;
				}
			}
			
			if (responseEntity == null || !HttpStatus.OK.equals(responseEntity.getStatusCode())) {
				LOG.error("Parsing build: " + instanceUrl , oe);
				throw oe;
			}
			
			List<VSTSBuildChanges> responseBuildChange = responseEntity.getBody().getValue();

			responseBuildChange.stream().forEach(change -> {
				long timestamp = new DateTime(change.getTimestamp()).getMillis();

				SCM scm = new SCM();
				scm.setScmAuthor(change.getAuthor().getDisplayName());
				scm.setScmCommitLog(change.getMessage());
				scm.setScmCommitTimestamp(timestamp);
				scm.setScmRevisionNumber(change.getId());
				scm.setScmUrl(change.getLocation());

				build.getSourceChangeSet().add(scm);
			});

		

	}
	
	private BuildStatus getBuildStatus(String status) {

		switch (status) {
		case "succeeded":
			return BuildStatus.Success;
		case "partiallySucceeded":
			return BuildStatus.Unstable;
		case "failed":
			return BuildStatus.Failure;
		case "canceled":
			return BuildStatus.Aborted;
		default:
			return BuildStatus.Unknown;
		}
	}

	protected ResponseEntity<String> makeRestCall(String sUrl) throws MalformedURLException {
		URI thisuri = URI.create(sUrl);
		String apikey = thisuri.getUserInfo();

		// get userinfo from URI or settings (in spring properties)
		if (StringUtils.isEmpty(apikey) && (this.settings.getApiKey() != null)) {
			apikey = this.settings.getApiKey();
		}
		// Basic Auth only.
		if (StringUtils.isNotEmpty(apikey)) {
			return rest.exchange(thisuri, HttpMethod.GET, new HttpEntity<>(createHeaders(apikey)), String.class);
		} else {
			return rest.exchange(thisuri, HttpMethod.GET, null, String.class);
		}

	}

	protected HttpHeaders createHeaders(final String apiKey) {

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, apiKey);
		headers.set(HttpHeaders.ACCEPT, "application/json");
		return headers;
	}

	protected String getLog(String buildUrl) {
		try {
			return makeRestCall(joinURL(buildUrl, "consoleText")).getBody();
		} catch (MalformedURLException mfe) {
			LOG.error("malformed url for build log", mfe);
		}

		return "";
	}

	// join a base url to another path or paths - this will handle trailing or
	// non-trailing /'s
	public static String joinURL(String base, String... paths) throws MalformedURLException {
		StringBuilder result = new StringBuilder(base);
		for (String path : paths) {
			String p = path.replaceFirst("^(\\/)+", "");
			if (result.lastIndexOf("/") != result.length() - 1) {
				result.append('/');
			}
			result.append(p);
		}
		return result.toString();
	}

	private <T> ResponseEntity<T> makeRestCall(String url, Class<T> responseType) {
		URI thisuri = URI.create(url);
		String apikey = this.settings.getApiKey();
		return rest.exchange(thisuri, HttpMethod.GET, new HttpEntity<>(createHeaders(apikey)), responseType);
	}
}
