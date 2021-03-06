package mobi.chouette.exchange.netexprofile.importer.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;

import mobi.chouette.common.TimeUtil;
import mobi.chouette.model.VehicleJourneyAtStop;

import org.rutebanken.netex.model.TimetabledPassingTime;

public class NetexTimeConversionUtil {

	public static void populatePassingTimeUtc(TimetabledPassingTime passingTime, boolean arrival, VehicleJourneyAtStop vj) {
	    if((arrival && vj.getArrivalTime() == null || (!arrival && vj.getDepartureTime() == null))) {
	    	return;
	    }

		int dayOffset = arrival ? vj.getArrivalDayOffset() : vj.getDepartureDayOffset();
		LocalTime localTime = TimeUtil.toLocalTimeFromJoda(arrival ? vj.getArrivalTime() : vj.getDepartureTime());

		if(arrival) {
			passingTime.setArrivalTime(localTime);
			if(dayOffset != 0) {
				passingTime.setArrivalDayOffset(BigInteger.valueOf(dayOffset));
			}
		} else {
			passingTime.setDepartureTime(localTime);
			if(dayOffset != 0) {
				passingTime.setDepartureDayOffset(BigInteger.valueOf(dayOffset));
			}
		}
	}

	public static void parsePassingTime(TimetabledPassingTime passingTime, boolean arrival, VehicleJourneyAtStop vj) {
	    if((arrival && passingTime.getArrivalTime() == null || (!arrival && passingTime.getDepartureTime() == null))) {
	    	return;
	    }
	
	    LocalTime localTime = arrival ? passingTime.getArrivalTime() : passingTime.getDepartureTime();
	    BigInteger dayOffset = arrival? passingTime.getArrivalDayOffset() : passingTime.getDepartureDayOffset();
	    if(dayOffset == null) {
	    	dayOffset = BigInteger.ZERO;
	    }

	    
		if(arrival) {
			vj.setArrivalTime(TimeUtil.toJodaLocalTime(localTime));

			if(!BigDecimal.ZERO.equals(dayOffset)) {
				vj.setArrivalDayOffset(dayOffset.intValue());
			}
		} else {
			vj.setDepartureTime(TimeUtil.toJodaLocalTime(localTime));

			if(!BigDecimal.ZERO.equals(dayOffset)) {
				vj.setDepartureDayOffset(dayOffset.intValue());
			}
		}
	}

}
