package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.UserNeedEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import no.rutebanken.netex.model.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

@Log4j
public class LineParser implements Parser, Constant {

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        LinesInFrame_RelStructure contextData = (LinesInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
        List<JAXBElement<? extends DataManagedObjectStructure>> netexLineElements = contextData.getLine_();

        for (JAXBElement<? extends DataManagedObjectStructure> netexLineElement : netexLineElements) {
            no.rutebanken.netex.model.Line netexLine = (no.rutebanken.netex.model.Line) netexLineElement.getValue();
            parseLine(referential, netexLine);
        }
    }

    private void parseLine(Referential referential, no.rutebanken.netex.model.Line netexLine) {
        mobi.chouette.model.Line chouetteLine = ObjectFactory.getLine(referential, netexLine.getId());

        //ModificationEnumeration modification = netexLine.getModification(); // how to handle in chouette? can be: new, delete, revise or delta

        // mandatory
        MultilingualString lineName = netexLine.getName();
        if (lineName != null) {
            String lineNameValue = lineName.getValue();
            if (StringUtils.isNotEmpty(lineNameValue)) {
                chouetteLine.setName(lineNameValue);
            }
        }

        // optional
        MultilingualString description = netexLine.getDescription();
        if (description != null) {
            String descriptionValue = description.getValue();
            if (StringUtils.isNotEmpty(descriptionValue)) {
                chouetteLine.setComment(descriptionValue);
            }
        }

        // mandatory
        AllVehicleModesOfTransportEnumeration transportMode = netexLine.getTransportMode();
        if (transportMode != null) {
            TransportModeNameEnum transportModeName = NetexUtils.toTransportModeNameEnum(transportMode.value());
            chouetteLine.setTransportModeName(transportModeName);
        }

        // optional, how to handle in chouette model?
        //TransportSubmodeStructure transportSubmode = netexLine.getTransportSubmode();

        // optional
        String url = netexLine.getUrl();
        if (StringUtils.isNotEmpty(url)) {
            chouetteLine.setUrl(url);
        }

        // mandatory
        String publicCode = netexLine.getPublicCode();
        if (StringUtils.isNotEmpty(publicCode)) {
            chouetteLine.setNumber(publicCode);
        }

        // optional
        PrivateCodeStructure privateCode = netexLine.getPrivateCode();
        if (privateCode != null) {
            String privateCodeValue = privateCode.getValue();
            if (StringUtils.isNotEmpty(privateCodeValue)) {
                chouetteLine.setRegistrationNumber(privateCodeValue);
            }
        }

        // mandatory
        OperatorRefStructure operatorRef = netexLine.getOperatorRef();
        if (operatorRef != null) {
            String operatorRefValue= operatorRef.getRef();
            if (StringUtils.isNotEmpty(operatorRefValue)) {
                Company company = ObjectFactory.getCompany(referential, operatorRefValue);
                chouetteLine.setCompany(company);
            }
        }

        // mandatory
        //Boolean monitored = netexLine.isMonitored(); // how to handle in chouette?

        // optional
        // TODO: create separate parser for every RouteRefStructure instances
        RouteRefs_RelStructure routeRefsStruct = netexLine.getRoutes();
        if (routeRefsStruct != null) {
            List<RouteRefStructure> routeRefs = routeRefsStruct.getRouteRef();
            for (RouteRefStructure routeRef : routeRefs) {
                mobi.chouette.model.Route chouetteRoute = ObjectFactory.getRoute(referential, routeRef.getRef());
                chouetteRoute.setLine(chouetteLine);
            }
        }

        // mandatory
        // TODO: create separate parser for every Suitability instances
        AccessibilityAssessment accessibilityAssessment = netexLine.getAccessibilityAssessment();
        if (accessibilityAssessment != null) {
            LimitationStatusEnumeration mobilityImpairedAccess = accessibilityAssessment.getMobilityImpairedAccess();
            chouetteLine.setMobilityRestrictedSuitable(ParserUtils.getBoolean(mobilityImpairedAccess.value()));

            Suitabilities_RelStructure suitabilitiesStruct = accessibilityAssessment.getSuitabilities();
            if (suitabilitiesStruct != null) {
                List<Suitability> suitabilities = suitabilitiesStruct.getSuitability();
                List<UserNeedEnum> userNeeds = new ArrayList<UserNeedEnum>();
                for (Suitability suitability : suitabilities) {
                    // TODO: parse needs here...
                    // UserNeedEnum userNeed = ParserUtils.getEnum(UserNeedEnum.class, need...);
                    // See: https://rutebanken.atlassian.net/wiki/display/PUBLIC/framework#framework-Suitability and https://rutebanken.atlassian.net/wiki/display/PUBLIC/framework#framework-AccessibilityLimitation
                }
                chouetteLine.setUserNeeds(userNeeds);
            }
        }

        // TODO: add remaining, optional fields here... see: https://rutebanken.atlassian.net/wiki/display/PUBLIC/network#network-Line

        // finally
        chouetteLine.setFilled(true);
    }

    static {
        ParserFactory.register(LineParser.class.getName(), new ParserFactory() {
            private LineParser instance = new LineParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}