package mobi.chouette.exchange.netexprofile.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.*;
import mobi.chouette.model.type.DayTypeEnum;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.sql.Time;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j
public class NetexProducerUtils {

    private static final String OBJECT_ID_SPLIT_CHAR = ":";
    private static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Europe/Oslo");

    public static boolean isSet(Object... objects) {
        for (Object val : objects) {
            if (val != null) {
                if (val instanceof String) {
                    if (!((String) val).isEmpty())
                        return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] generateIdSequence(int totalInSequence) {
        String[] idSequence = new String[totalInSequence];
        AtomicInteger incrementor = new AtomicInteger(1);

        for (int i = 0; i < totalInSequence; i++) {
            idSequence[i] = String.valueOf(incrementor.getAndAdd(1));
        }

        return idSequence;
    }

    public static ZoneOffset getZoneOffset(ZoneId zoneId) {
        return zoneId == null ? null : zoneId.getRules().getOffset(Instant.now(Clock.system(zoneId)));
    }

    public static OffsetTime toOffsetTimeUtc(Time time) {
        return time == null ? null : time.toLocalTime().atOffset(getZoneOffset(LOCAL_ZONE_ID)).withOffsetSameInstant(ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    public static OffsetDateTime toOffsetDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) date;
            ZonedDateTime zonedDateTime = sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault());
            return OffsetDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.systemDefault());
        }
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault());
    }

    public static AllVehicleModesOfTransportEnumeration toVehicleModeOfTransportEnum(String value) {
        if (value == null)
            return null;
        else if (value.equals("Air"))
            return AllVehicleModesOfTransportEnumeration.AIR;
        else if (value.equals("Train"))
            return AllVehicleModesOfTransportEnumeration.RAIL;
        else if (value.equals("LongDistanceTrain"))
            return AllVehicleModesOfTransportEnumeration.INTERCITY_RAIL;
        else if (value.equals("LocalTrain"))
            return AllVehicleModesOfTransportEnumeration.URBAN_RAIL;
        else if (value.equals("Metro"))
            return AllVehicleModesOfTransportEnumeration.METRO;
        else if (value.equals("Tramway"))
            return AllVehicleModesOfTransportEnumeration.TRAM;
        else if (value.equals("Coach"))
            return AllVehicleModesOfTransportEnumeration.COACH;
        else if (value.equals("Bus"))
            return AllVehicleModesOfTransportEnumeration.BUS;
        else if (value.equals("Ferry"))
            return AllVehicleModesOfTransportEnumeration.WATER;
        else if (value.equals("Walk"))
            return AllVehicleModesOfTransportEnumeration.SELF_DRIVE;
        else if (value.equals("Trolleybus"))
            return AllVehicleModesOfTransportEnumeration.TROLLEY_BUS;
        else if (value.equals("Taxi"))
            return AllVehicleModesOfTransportEnumeration.TAXI;
        else if (value.equals("Other"))
            return AllVehicleModesOfTransportEnumeration.UNKNOWN;
        else
            return AllVehicleModesOfTransportEnumeration.UNKNOWN;
    }

    public static List<DayTypeEnum> convertDayOfWeek(DayOfWeekEnumeration dayOfWeek) {
        List<DayTypeEnum> days = new ArrayList<>();

        switch (dayOfWeek) {
            case MONDAY:
                days.add(DayTypeEnum.Monday);
                break;
            case TUESDAY:
                days.add(DayTypeEnum.Tuesday);
                break;
            case WEDNESDAY:
                days.add(DayTypeEnum.Wednesday);
                break;
            case THURSDAY:
                days.add(DayTypeEnum.Thursday);
                break;
            case FRIDAY:
                days.add(DayTypeEnum.Friday);
                break;
            case SATURDAY:
                days.add(DayTypeEnum.Saturday);
                break;
            case SUNDAY:
                days.add(DayTypeEnum.Sunday);
                break;
            case EVERYDAY:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case WEEKDAYS:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                break;
            case WEEKEND:
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case NONE:
                // None
                break;
        }
        return days;
    }

    @SuppressWarnings("unchecked")
    public static List<DayOfWeekEnumeration> toDayOfWeekEnumeration(List<DayTypeEnum> dayTypeEnums) {
        EnumSet actualDaysOfWeek = EnumSet.noneOf(DayTypeEnum.class);
        for (DayTypeEnum dayTypeEnum : dayTypeEnums) {
            actualDaysOfWeek.add(dayTypeEnum);
        }

        if (actualDaysOfWeek.isEmpty()) {
            return Collections.singletonList(DayOfWeekEnumeration.NONE);
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday,
                DayTypeEnum.Wednesday, DayTypeEnum.Thursday, DayTypeEnum.Friday))) {
            return Collections.singletonList(DayOfWeekEnumeration.WEEKDAYS);
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Saturday, DayTypeEnum.Sunday))) {
            return Collections.singletonList(DayOfWeekEnumeration.WEEKEND);
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday, DayTypeEnum.Wednesday,
                DayTypeEnum.Thursday, DayTypeEnum.Friday, DayTypeEnum.Saturday, DayTypeEnum.Sunday))) {
            return Collections.singletonList(DayOfWeekEnumeration.EVERYDAY);
        }

        List<DayOfWeekEnumeration> dayOfWeekEnumerations = new ArrayList<>();

        for (DayTypeEnum dayTypeEnum : dayTypeEnums) {
            switch (dayTypeEnum) {
                case Monday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.MONDAY);
                    break;
                case Tuesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.TUESDAY);
                    break;
                case Wednesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.WEDNESDAY);
                    break;
                case Thursday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.THURSDAY);
                    break;
                case Friday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.FRIDAY);
                    break;
                case Saturday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SATURDAY);
                    break;
                case Sunday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SUNDAY);
                    break;
            }
        }

        return dayOfWeekEnumerations;
    }

    private static String netexModelName(NeptuneIdentifiedObject model) {
        if (model == null)
            return null;
        if (model instanceof StopArea) {
            return "StopArea";
        } else if (model instanceof AccessPoint) {
            return "AccessPoint";
        } else if (model instanceof Company) {
            return "Operator";
        } else if (model instanceof AccessLink) {
            return "AccessLink";
        } else if (model instanceof StopPoint) {
            return "StopPoint";
        } else if (model instanceof Network) {
            return "GroupOfLine";
        } else if (model instanceof Line) {
            return "Line";
        } else if (model instanceof Route) {
            return "Route";
        } else if (model instanceof GroupOfLine) {
            return "GroupOfLine";
        } else if (model instanceof JourneyPattern) {
            return "JourneyPattern";
        } else if (model instanceof ConnectionLink) {
            return "ConnectionLink";
        } else if (model instanceof Timetable) {
            return "Timetable";
        } else if (model instanceof VehicleJourney) {
            return "ServiceJourney";
        } else {
            return null;
        }
    }

    public static String netexId(NeptuneIdentifiedObject model) {
        return model == null ? null : model.objectIdPrefix() + OBJECT_ID_SPLIT_CHAR + netexModelName(model) + OBJECT_ID_SPLIT_CHAR + model.objectIdSuffix();
    }

    public static String netexId(String objectIdPrefix, String elementName, String objectIdSuffix) {
        return objectIdPrefix + OBJECT_ID_SPLIT_CHAR + elementName + OBJECT_ID_SPLIT_CHAR + objectIdSuffix;
    }

    public static String objectIdPrefix(String objectId) {
        return objectId.split(OBJECT_ID_SPLIT_CHAR).length > 2 ? objectId.split(OBJECT_ID_SPLIT_CHAR)[0].trim() : "";
    }

    public static String objectIdSuffix(String objectId) {
        return objectId.split(OBJECT_ID_SPLIT_CHAR).length > 2 ? objectId.split(OBJECT_ID_SPLIT_CHAR)[2].trim() : "";
    }

}
