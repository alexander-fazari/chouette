package mobi.chouette.exchange.importer.updater.netex;

import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.rutebanken.netex.model.Zone_VersionStructure;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

public class StopPlaceMapper {

	/**
	 * Map stop area with contained stop areas.
	 * 
	 * @param stopArea
	 *            Typically stop areas of {@link ChouetteAreaEnum#StopPlace} or
	 *            {@link ChouetteAreaEnum#CommercialStopPoint}
	 * @return NeTEx stop place
	 */
	public StopPlace mapStopAreaToStopPlace(StopArea stopArea) {
		StopPlace stopPlace = createStopPlace(stopArea);
		if (stopArea.getContainedStopAreas().size() > 0) {
			stopPlace.setQuays(new Quays_RelStructure());
			for (StopArea children : stopArea.getContainedStopAreas()) {
				Quay quay = createQuay(children);
				stopPlace.getQuays().getQuayRefOrQuay().add(quay);
			}
		}

		return stopPlace;
	}

	public StopArea mapStopPlaceToStopArea(Referential referential, StopPlace stopPlace) {
		StopArea stopArea = createStopArea(referential, stopPlace);

		Quays_RelStructure quays = stopPlace.getQuays();
		if (quays != null) {
			for (Object q : quays.getQuayRefOrQuay()) {
				StopArea boardingPosition = createBoardingPosition(referential, (Quay) q);
				boardingPosition.setParent(stopArea);
			}
		}

		return stopArea;
	}

	private StopArea createStopArea(Referential referential, StopPlace stopPlace) {
		StopArea stopArea = ObjectFactory.getStopArea(referential, stopPlace.getId());
		stopArea.setAreaType(ChouetteAreaEnum.CommercialStopPoint);

		mapCentroid(stopPlace, stopArea);
		mapName(stopPlace, stopArea);

		return stopArea;

	}

	private StopArea createBoardingPosition(Referential referential, Quay quay) {

		StopArea boardingPosition = ObjectFactory.getStopArea(referential, quay.getId());
		boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
		mapCentroid(quay, boardingPosition);
		mapName(quay, boardingPosition);
		return boardingPosition;
	}

	private StopPlace createStopPlace(StopArea stopArea) {
		StopPlace stopPlace = new StopPlace();
		mapId(stopArea, stopPlace);
		mapCentroid(stopArea, stopPlace);
		mapName(stopArea, stopPlace);
		return stopPlace;
	}

	private Quay createQuay(StopArea stopArea) {
		Quay quay = new Quay();
		mapId(stopArea, quay);
		mapCentroid(stopArea, quay);
		mapName(stopArea, quay);
		return quay;
	}

	private void mapId(StopArea stopArea, Zone_VersionStructure zone) {
		zone.setId(stopArea.getObjectId());
	}

	public void mapCentroid(StopArea stopArea, Zone_VersionStructure zone) {
		if(stopArea.getLatitude() != null && stopArea.getLongitude() != null) {
			zone.setCentroid(new SimplePoint_VersionStructure().withLocation(
					new LocationStructure().withLatitude(stopArea.getLatitude()).withLongitude(stopArea.getLongitude())));
		}
	}

	public void mapCentroid(Zone_VersionStructure zone, StopArea stopArea) {
		if(zone.getCentroid() != null && zone.getCentroid().getLocation() != null) {
			LocationStructure location = zone.getCentroid().getLocation();
			stopArea.setLatitude(location.getLatitude());
			stopArea.setLongitude(location.getLongitude());
			stopArea.setLongLatType(LongLatTypeEnum.WGS84);
		}
	}

	public void mapName(StopArea stopArea, Zone_VersionStructure zone) {

		zone.setName(new MultilingualString().withValue(stopArea.getName()).withLang("no").withTextIdType(""));

	}

	public void mapName(Zone_VersionStructure zone, StopArea stopArea) {
		stopArea.setName(zone.getName().getValue());
	}

	public void mapTransportMode(StopPlace sp, TransportModeNameEnum mode) {
		switch (mode) {
		case Air:
			sp.setStopPlaceType(StopTypeEnumeration.AIRPORT);
			break;
		case Train:
		case LongDistanceTrain_2:
		case LongDistanceTrain:
		case LocalTrain:
		case RapidTransit:
			sp.setStopPlaceType(StopTypeEnumeration.RAIL_STATION);
			break;
		case Metro:
			sp.setStopPlaceType(StopTypeEnumeration.METRO_STATION);
			break;
		case Tramway:
			sp.setStopPlaceType(StopTypeEnumeration.TRAM_STATION);
			break;
		case Shuttle:
		case Coach:
		case Bus:
		case Trolleybus:
			sp.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);
			break;
		case Ferry:
			sp.setStopPlaceType(StopTypeEnumeration.HARBOUR_PORT);
			break;
		case Waterborne:
			sp.setStopPlaceType(StopTypeEnumeration.FERRY_STOP);
			break;
		default:

		}
	}

}