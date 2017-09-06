package mobi.chouette.exchange.netexprofile.exporter.writer;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DAY_TYPES;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.DAY_TYPE_ASSIGNMENTS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.OPERATING_PERIODS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.SERVICE_CALENDAR_FRAME;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;

public class ServiceCalendarFrameWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller) {

		String serviceCalendarFrameId = NetexProducerUtils.createUniqueId(context, SERVICE_CALENDAR_FRAME);

        try {
            writer.writeStartElement(SERVICE_CALENDAR_FRAME);
            writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
            writer.writeAttribute(ID, serviceCalendarFrameId);
            writeDayTypesElement(writer, exportableNetexData,marshaller);

            if (CollectionUtils.isNotEmpty(exportableNetexData.getSharedOperatingPeriods())) {
                writeOperatingPeriodsElement(writer, exportableNetexData,marshaller);
            }

            writeDayTypeAssignmentsElement(writer, exportableNetexData,marshaller);
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(DAY_TYPES);
            for (DayType dayType : exportableData.getSharedDayTypes().values()) {
                marshaller.marshal(netexFactory.createDayType(dayType), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypeAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(DAY_TYPE_ASSIGNMENTS);
            for (DayTypeAssignment dayTypeAssignment : exportableData.getSharedDayTypeAssignments()) {
                marshaller.marshal(netexFactory.createDayTypeAssignment(dayTypeAssignment), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeOperatingPeriodsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(OPERATING_PERIODS);
            for (OperatingPeriod operatingPeriod : exportableData.getSharedOperatingPeriods()) {
                marshaller.marshal(netexFactory.createOperatingPeriod(operatingPeriod), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
