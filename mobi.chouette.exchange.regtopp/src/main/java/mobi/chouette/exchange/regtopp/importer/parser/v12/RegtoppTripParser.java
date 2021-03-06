package mobi.chouette.exchange.regtopp.importer.parser.v12;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.PARSER;
import static mobi.chouette.common.Constant.REFERENTIAL;

import org.apache.commons.lang.StringUtils;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.regtopp.RegtoppConstant;
import mobi.chouette.exchange.regtopp.importer.RegtoppImportParameters;
import mobi.chouette.exchange.regtopp.importer.RegtoppImporter;
import mobi.chouette.exchange.regtopp.importer.index.Index;
import mobi.chouette.exchange.regtopp.importer.index.v11.DaycodeById;
import mobi.chouette.exchange.regtopp.importer.parser.ObjectIdCreator;
import mobi.chouette.exchange.regtopp.importer.parser.RouteKey;
import mobi.chouette.exchange.regtopp.importer.parser.v11.TransportModePair;
import mobi.chouette.exchange.regtopp.model.AbstractRegtoppRouteTMS;
import mobi.chouette.exchange.regtopp.model.AbstractRegtoppTripIndexTIX;
import mobi.chouette.exchange.regtopp.model.enums.AnnouncementType;
import mobi.chouette.exchange.regtopp.model.enums.TransportType;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppDayCodeHeaderDKO;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppDestinationDST;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppTripIndexTIX;
import mobi.chouette.model.Company;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

@Log4j
public class RegtoppTripParser extends mobi.chouette.exchange.regtopp.importer.parser.v11.RegtoppTripParser {

	/*
	 * Validation rules of type III are checked at this step.
	 */
	// TODO. Rename this function "translate(Context context)" or "produce(Context context)", ...
	@Override
	public void parse(Context context) throws Exception {

		// Her tar vi allerede konsistenssjekkede data (ref validate-metode over) og bygger opp tilsvarende struktur i chouette.
		// Merk at import er linje-sentrisk, så man skal i denne klassen returnerer 1 line med x antall routes og stoppesteder, journeypatterns osv

		Referential referential = (Referential) context.get(REFERENTIAL);

		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppImportParameters configuration = (RegtoppImportParameters) context.get(CONFIGURATION);
		String calendarStartDate = (String) context.get(RegtoppConstant.CALENDAR_START_DATE);

		String chouetteLineId = ObjectIdCreator.createLineId(configuration, lineId, calendarStartDate);
		Line line = ObjectFactory.getLine(referential, chouetteLineId);

		Index<RegtoppDestinationDST> destinationIndex = importer.getDestinationById();

		DaycodeById dayCodeIndex = (DaycodeById) importer.getDayCodeById();
		RegtoppDayCodeHeaderDKO dayCodeHeader = dayCodeIndex.getHeader();

		// Add VehicleJourneys
		Index<AbstractRegtoppTripIndexTIX> tripIndex = importer.getTripIndex();
		for (AbstractRegtoppTripIndexTIX abstractTrip : tripIndex) {
			if (abstractTrip.getLineId().equals(lineId)) {
				RegtoppTripIndexTIX trip = (RegtoppTripIndexTIX) abstractTrip;

				if (trip.getNotificationType() == AnnouncementType.Announced) {

					// This is where we get the line number
					line.setNumber(trip.getLineNumberVisible());

					RouteKey routeKey = new RouteKey(trip.getLineId(), trip.getDirection(), trip.getRouteIdRef(), calendarStartDate);
					String chouetteRouteId = ObjectIdCreator.createRouteId(configuration, routeKey);
					Route route = ObjectFactory.getRoute(referential, chouetteRouteId);

					String chouetteJourneyPatternId = ObjectIdCreator.createJourneyPatternId(configuration, routeKey);
					JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential, chouetteJourneyPatternId);

					String chouetteVehicleJourneyId = ObjectIdCreator.createServiceJourneyId(configuration, trip.getLineId(), trip.getTripId(),
							calendarStartDate);
					VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, chouetteVehicleJourneyId);

					// Add operator company
					Company operator = createOperator(referential, configuration, trip.getOperatorCode());
					vehicleJourney.setCompany(operator);

					addFootnote(referential,trip.getFootnoteId1Ref(), vehicleJourney, importer,configuration,calendarStartDate);
					addFootnote(referential,trip.getFootnoteId2Ref(), vehicleJourney, importer,configuration, calendarStartDate);

					RegtoppDestinationDST departureText = destinationIndex.getValue(trip.getDestinationIdDepartureRef()); // Turens bestemmelsessted
					RegtoppDestinationDST arrivalText = destinationIndex.getValue(trip.getDestinationIdArrivalRef()); // Turens startsted

					// TODO unsure
					if (departureText != null && arrivalText != null) {
						if(departureText.getDestinationText().equals(arrivalText.getDestinationText())) {
							vehicleJourney.setPublishedJourneyName(arrivalText.getDestinationText());
						} else {
							vehicleJourney.setPublishedJourneyName(arrivalText.getDestinationText() + " -> " + departureText.getDestinationText());
						}
					} else if (departureText != null) {
						vehicleJourney.setPublishedJourneyName(departureText.getDestinationText());
					} else if (arrivalText != null) {
						vehicleJourney.setPublishedJourneyName(arrivalText.getDestinationText());
					}

					vehicleJourney.setPublishedJourneyIdentifier(StringUtils.trimToNull(trip.getLineNumberVisible()));
					TransportModePair transportModePair = convertTypeOfService(trip.getTypeOfService());
					transportModes.add(transportModePair);
					vehicleJourney.setTransportMode(transportModePair.transportMode);
					vehicleJourney.setTransportSubMode(transportModePair.subMode);
					vehicleJourney.setJourneyPattern(journeyPattern);
					vehicleJourney.setRoute(route);

					boolean byRequestOnly = false;
					if (trip.getTypeOfService() == TransportType.FlexibleBus) {
						byRequestOnly = true;
						line.setFlexibleService(Boolean.TRUE);
					}

					// Link to timetable
					linkVehicleJourneyToTimetable(referential, configuration, trip, vehicleJourney, dayCodeHeader);

					// TODO this must be precomputed instead of iterating over tens of thousands of records for each trip.
					for (AbstractRegtoppRouteTMS vehicleStop : importer.getRouteIndex()) {
						if (vehicleStop.getLineId().equals(lineId)) {
							if (vehicleStop.getRouteId().equals(trip.getRouteIdRef())) {
								if (vehicleStop.getDirection() == trip.getDirection()) {

									String chouetteStopPointId = ObjectIdCreator.createStopPointId(configuration, routeKey,
											vehicleStop.getSequenceNumberStop());

									StopPoint stopPoint = referential.getStopPoints().get(chouetteStopPointId);
									if (stopPoint != null) {
										addVehicleJourneyAtStop(configuration,referential, vehicleJourney, trip.getDepartureTime(), stopPoint, vehicleStop.getDriverTimeArrival(),
												vehicleStop.getDriverTimeDeparture(), byRequestOnly);
									} else {
										log.warn("Not adding VehicleJourneyAtStop since StopPoint with id " + chouetteStopPointId + " is missing");
									}
								}
							}
						}
					}

				} else {
					// TODO log info message in action report about this
					log.info("Skipping unannouced trip: " + trip);
				}
			}
		}
		estimateMissingPassingTimes(referential);
	}

	static {
		ParserFactory.register(RegtoppTripParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new RegtoppTripParser();
			}
		});
	}

}
