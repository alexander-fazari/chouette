package mobi.chouette.exchange.netexprofile.importer.validation.norway;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.ExternalReferenceValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.ExternalReferenceValidatorFactory;
import mobi.chouette.exchange.validation.report.ValidationReporter;

@Log4j
public class StopPlaceRegistryIdValidator implements ExternalReferenceValidator {

	public static final String NAME = "StopPlaceRegistryIdValidator";

	private Set<String> stopPlaceCache = new HashSet<>();

	private Set<String> quayCache = new HashSet<>();

	private String quayEndpoint;

	private String stopPlaceEndpoint;

	private long lastUpdated = 0;

	public final long timeToLiveMs = 1000 * 60 * 60 * 5; // 5 minutes

	public StopPlaceRegistryIdValidator() {

		String quayEndpointPropertyKey = "iev.stop.place.register.mapping.quay";
		quayEndpoint = System.getProperty(quayEndpointPropertyKey);
		if (quayEndpoint == null) {
			log.warn("Could not find property named " + quayEndpointPropertyKey + " in iev.properties");
			quayEndpoint = "https://api-test.rutebanken.org/tiamat/1.0/quay/id_mapping?recordsPerRoundTrip=220000";
		}

		String stopPlaceEndpointPropertyKey = "iev.stop.place.register.mapping.stopplace";
		stopPlaceEndpoint = System.getProperty(stopPlaceEndpointPropertyKey);
		if (stopPlaceEndpoint == null) {
			log.warn("Could not find property named " + stopPlaceEndpointPropertyKey + " in iev.properties");
			stopPlaceEndpoint = "https://api-test.rutebanken.org/tiamat/1.0/stop_place/id_mapping?recordsPerRoundTrip=220000";
		}
	}

	@Override
	public Set<IdVersion> validateReferenceIds(Context context, Set<IdVersion> externalIds) {

		ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();

		if (lastUpdated < System.currentTimeMillis() - timeToLiveMs) {
			// Fetch data and populate caches
			log.info("Cache is old, refreshing quay and stopplace cache");
			boolean stopPlaceOk = populateCache(stopPlaceCache, stopPlaceEndpoint);
			boolean quayOK = populateCache(quayCache, quayEndpoint);

			if (quayOK && stopPlaceOk) {
				lastUpdated = System.currentTimeMillis();
			} else {
				log.error("Error updating caches");
			}
		}

		log.info("About to validate external " + externalIds.size() + " ids");

		Set<IdVersion> invalidIds = new HashSet<>();

		Set<IdVersion> idsToCheck = isOfSupportedTypes(externalIds);

		for (IdVersion id : idsToCheck) {
			if (id.getId().contains(":Quay:") && !quayCache.contains(id.getId())) {

				invalidIds.add(id);
				validationReporter.addCheckPointReportError(context,
						AbstractNorwayNetexProfileValidator._1_NETEX_SERVICE_FRAME_JOURNEY_PATTERN_PASSENGERSTOPASSIGNMENT_QUAYREF,
						DataLocationHelper.findDataLocation(id));

			} else if (id.getId().contains(":StopPlace:") && !stopPlaceCache.contains(id.getId()))  {
				invalidIds.add(id);
			}
		}

		log.info("Found " + invalidIds.size() + " ids invalid");

		return invalidIds;
	}

	public static class DefaultExternalReferenceValidatorFactory extends ExternalReferenceValidatorFactory {
		@Override
		protected ExternalReferenceValidator create(Context context) {
			ExternalReferenceValidator instance = (ExternalReferenceValidator) context.get(NAME);
			if (instance == null) {
				instance = new StopPlaceRegistryIdValidator();
				context.put(NAME, instance);
			}
			return instance;
		}
	}

	static {
		ExternalReferenceValidatorFactory.factories.put(StopPlaceRegistryIdValidator.class.getName(),
				new StopPlaceRegistryIdValidator.DefaultExternalReferenceValidatorFactory());
	}

	private boolean populateCache(Set<String> cache, String u) {
		cache.clear();
		HttpURLConnection connection = null;

		try {
			URL url = new URL(u);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.connect();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = rd.readLine()) != null) {
				String[] split = StringUtils.split(line, ",");
				if (split.length == 2) {
					cache.add(split[0]);
					cache.add(split[1]);
				} else {
					log.error("NSR contains illegal mappings: " + u + " " + line);
				}
			}
			rd.close();
			return true;
		} catch (Exception e) {
			log.error("Error getting NSR cache for url " + u, e);
		} finally {
			connection.disconnect();
		}

		return false;

	}

	@Override
	public Set<IdVersion> isOfSupportedTypes(Set<IdVersion> externalIds) {
		// These are the references we want to check externally
		return externalIds.stream().filter(e -> e.getId().contains(":Quay:") || e.getId().contains(":StopPlace:")).collect(Collectors.toSet());
	}

}