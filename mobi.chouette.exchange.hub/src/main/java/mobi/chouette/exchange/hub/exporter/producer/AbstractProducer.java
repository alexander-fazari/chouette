package mobi.chouette.exchange.hub.exporter.producer;

import java.sql.Time;
import java.util.Collection;

import lombok.Getter;
import mobi.chouette.exchange.hub.model.HubObject;
import mobi.chouette.exchange.hub.model.exporter.HubExporterInterface;
import mobi.chouette.model.NeptuneIdentifiedObject;

public abstract class AbstractProducer {

	@Getter
	private HubExporterInterface exporter;

	public AbstractProducer(HubExporterInterface exporter) {
		this.exporter = exporter;
	}

//	static protected String toHubId(String neptuneId) {
//		String[] tokens = neptuneId.split(":");
//		return tokens[2];
//	}

	static protected String toHubId(NeptuneIdentifiedObject neptuneObject) {
		if (neptuneObject == null || neptuneObject.getObjectId() == null)
			return null;
		String[] tokens = neptuneObject.getObjectId().split(":");
		return tokens[2];
	}

	static protected boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	static protected boolean isTrue(Boolean b) {
		return b != null && b;
	}

	static protected boolean isEmpty(Collection<? extends Object> s) {
		return s == null || s.isEmpty();
	}

	static protected String getValue(String s) {
		if (isEmpty(s))
			return null;
		else
			return s;

	}

	static protected Integer toHubTime(Time time)
	{
		if (time == null) return null;
		long seconds = time.getTime()/1000;
		return Integer.valueOf((int) seconds);
	}
	
	static protected Integer toInt(String value)
	{
		if (value == null) return null;
		try
		{
			return Integer.decode(value);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}
	
	static protected int toSens(String wayBack)
	{
		return "R".equals(wayBack)? HubObject.SENS_RETOUR : HubObject.SENS_ALLER ;
	}

}
