/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.hub.exporter.producer;

import mobi.chouette.exchange.hub.model.HubCorrespondance;
import mobi.chouette.exchange.hub.model.exporter.HubExporterInterface;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.ConnectionLink;


/**
 * convert Timetable to Hub Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
public class HubCorrespondanceProducer extends AbstractProducer {
	public HubCorrespondanceProducer(HubExporterInterface exporter) {
		super(exporter);
	}
	
	private HubCorrespondance hubObject = new HubCorrespondance();

	public boolean save(ConnectionLink neptuneObject, ActionReport report) {

		hubObject.clear();
		hubObject.setCodeArret1(toHubId(neptuneObject.getStartOfLink()));
		hubObject.setIdentifiantArret1(neptuneObject.getStartOfLink().getId());
		hubObject.setCodeArret2(toHubId(neptuneObject.getEndOfLink()));
		hubObject.setIdentifiantArret2(neptuneObject.getEndOfLink().getId());
		
		if (neptuneObject.getLinkDistance() != null)
		   hubObject.setDistance(neptuneObject.getLinkDistance().intValue());
		
		hubObject.setTempsParcours(toHubTime(neptuneObject.getDefaultDuration()));
		
		hubObject.setIdentifiant(neptuneObject.getId());

		try {
			getExporter().getCorrespondanceExporter().export(hubObject);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
