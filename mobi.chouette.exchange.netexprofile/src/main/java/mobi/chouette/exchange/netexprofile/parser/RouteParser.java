package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.Comparator;
import java.util.List;

import static mobi.chouette.exchange.importer.ParserUtils.objectIdPrefix;
import static mobi.chouette.exchange.importer.ParserUtils.objectIdSuffix;

@Log4j
public class RouteParser implements Parser, Constant {

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        RoutesInFrame_RelStructure routesInFrameStruct = (RoutesInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
        Context parsingContext = (Context) context.get(PARSING_CONTEXT);
        Context routePointContext = (Context) parsingContext.get(RoutePointParser.LOCAL_CONTEXT);
        Context stopAssignmentContext = (Context) parsingContext.get(StopAssignmentParser.LOCAL_CONTEXT);

        List<JAXBElement<? extends LinkSequence_VersionStructure>> routeElements = routesInFrameStruct.getRoute_();

        for (JAXBElement<? extends LinkSequence_VersionStructure> routeElement : routeElements) {
            org.rutebanken.netex.model.Route netexRoute = (org.rutebanken.netex.model.Route) routeElement.getValue();
            mobi.chouette.model.Route chouetteRoute = ObjectFactory.getRoute(referential, netexRoute.getId());

            chouetteRoute.setObjectVersion(NetexParserUtils.getVersion(netexRoute));

            String routeName = netexRoute.getName().getValue();
            chouetteRoute.setName(routeName);

            if (netexRoute.getShortName() != null) {
                chouetteRoute.setPublishedName(netexRoute.getShortName().getValue());
            } else {
                chouetteRoute.setPublishedName(routeName);
            }

            DirectionTypeEnumeration directionType = netexRoute.getDirectionType();
            chouetteRoute.setDirection(directionType == null || directionType.equals(DirectionTypeEnumeration.OUTBOUND) ? PTDirectionEnum.A : PTDirectionEnum.R);

            String lineIdRef = netexRoute.getLineRef().getValue().getRef();
            Line chouetteLine = ObjectFactory.getLine(referential, lineIdRef);
            chouetteRoute.setLine(chouetteLine);

            // TODO find out if this should be set?
            // chouetteRoute.setWayBack(directionType.equals(DirectionTypeEnumeration.OUTBOUND) ? "A" : "R");

            // TODO consider how to handle the inverse route id ref, create instance here?, optional (cardinality 0:1)
            RouteRefStructure inverseRouteRefStructure = netexRoute.getInverseRouteRef();
            if (inverseRouteRefStructure != null) {
                mobi.chouette.model.Route wayBackRoute = ObjectFactory.getRoute(referential, inverseRouteRefStructure.getRef());

                if (wayBackRoute != null) {
                    wayBackRoute.setOppositeRoute(chouetteRoute);
                }
            }

            for (PointOnRoute pointOnRoute : netexRoute.getPointsInSequence().getPointOnRoute()) {
                if (pointOnRoute.getPointRef() != null) {
                    String routePointIdRef = pointOnRoute.getPointRef().getValue().getRef();
                    Context routePointObjectContext = (Context) routePointContext.get(routePointIdRef);
                    String scheduledStopPointId = (String) routePointObjectContext.get(RoutePointParser.STOP_POINT_ID);

                    Context stopAssignmentObjectCtx = (Context) stopAssignmentContext.get(scheduledStopPointId);
                    String quayId = (String) stopAssignmentObjectCtx.get(StopAssignmentParser.QUAY_ID);

                    StopArea stopArea = ObjectFactory.getStopArea(referential, quayId);

                    if (stopArea != null) {
                        String stopPointId = NetexParserUtils.netexId(objectIdPrefix(pointOnRoute.getId()), "StopPoint", objectIdSuffix(pointOnRoute.getId()));
                        StopPoint stopPoint = ObjectFactory.getStopPoint(referential, stopPointId);
                        stopPoint.setObjectVersion(NetexParserUtils.getVersion(pointOnRoute));
                        stopPoint.setContainedInStopArea(stopArea);
                        stopPoint.setRoute(chouetteRoute);
                        stopPoint.setFilled(true);

                        log.debug("Added StopPoint " + scheduledStopPointId + " to Route " +
                                chouetteRoute.getObjectId() + ". ContainedInStopArea is " + stopArea.getObjectId());
                    } else {
                        log.warn("StopArea with id " + quayId + " not found in cache. Not adding StopPoint " + scheduledStopPointId + " to Route.");
                    }
                }
            }

            chouetteRoute.getStopPoints().forEach(stopPoint -> stopPoint.setPosition(chouetteRoute.getStopPoints().indexOf(stopPoint)));
            chouetteRoute.getStopPoints().sort(Comparator.comparingInt(StopPoint::getPosition));
            chouetteRoute.setFilled(true);
        }
    }

    static {
        ParserFactory.register(RouteParser.class.getName(), new ParserFactory() {
            private RouteParser instance = new RouteParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
