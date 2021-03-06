package mobi.chouette.exchange.regtopp.importer.version;

import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.regtopp.importer.RegtoppImporter;
import mobi.chouette.exchange.regtopp.importer.parser.LineSpecificParser;
import mobi.chouette.exchange.regtopp.importer.parser.ParseableFile;
import mobi.chouette.exchange.regtopp.importer.parser.v11.RegtoppConnectionLinkParser;
import mobi.chouette.exchange.regtopp.importer.parser.v11.RegtoppRouteParser;
import mobi.chouette.exchange.regtopp.importer.parser.v11.RegtoppStopParser;
import mobi.chouette.exchange.regtopp.importer.parser.v11.RegtoppTripParser;
import mobi.chouette.exchange.regtopp.model.v11.*;
import mobi.chouette.exchange.regtopp.validation.RegtoppException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class Regtopp11DVersionHandler implements VersionHandler {

	@Override
	public void registerFileForIndex(RegtoppImporter importer, Path fileName, String extension) {
		switch (extension) {

		case "TIX": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppTripIndexTIX.class }), RegtoppException.ERROR.TIX_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.TRIP_INDEX.name(), parseableFile);
			importer.registerFileForIndex(RegtoppImporter.INDEX.LINE_BY_TRIPS.name(), parseableFile);
			break;
		}
		case "TDA": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppRouteTDA.class }), RegtoppException.ERROR.TDA_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.ROUTE_BY_LINE_NUMBER.name(), parseableFile);
			break;
		}
		case "HPL": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppStopHPL.class }), RegtoppException.ERROR.HPL_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.STOP_BY_ID.name(), parseableFile);
			break;
		}
		case "DKO": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(),
					Arrays.asList(new Class[] { RegtoppDayCodeHeaderDKO.class, RegtoppDayCodeDKO.class }), RegtoppException.ERROR.DKO_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.DAYCODE_BY_ID.name(), parseableFile);
			break;
		}
		case "DST": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppDestinationDST.class }), RegtoppException.ERROR.DST_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.DESTINATION_BY_ID.name(), parseableFile);
			break;
		}
		case "MRK": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppFootnoteMRK.class }), RegtoppException.ERROR.MRK_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.REMARK_BY_ID.name(), parseableFile);
			break;
		}
		case "LIN": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppLineLIN.class }), RegtoppException.ERROR.LIN_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.LINE_BY_ID.name(), parseableFile);
			break;
		}
		case "GAV": {
			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppPathwayGAV.class }), RegtoppException.ERROR.GAV_INVALID_FIELD_VALUE);
			importer.registerFileForIndex(RegtoppImporter.INDEX.PATHWAY_BY_INDEXING_KEY.name(), parseableFile);
			break;
		}
//		case "SAM": {
//			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppPathwayGAV.class }), file);
//			importer.registerFileForIndex(RegtoppImporter.INDEX.INTERCHANGE.name(), parseableFile);
//			break;
//		}
//		case "SON": {
//			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppZoneSON.class }), file);
//			importer.registerFileForIndex(RegtoppImporter.INDEX.ZONE_BY_ID.name(), parseableFile);
//			break;
//		}
//		case "VLP": {
//			ParseableFile parseableFile = new ParseableFile(fileName.toFile(), Arrays.asList(new Class[] { RegtoppVehicleJourneyVLP.class }), file);
//			importer.registerFileForIndex(RegtoppImporter.INDEX.VEHICLE_JOURNEY.name(), parseableFile);
//			break;
//		}
		}
	}

	@Override
	public Parser createStopParser() throws ClassNotFoundException, IOException {
		return (RegtoppStopParser) ParserFactory.create(RegtoppStopParser.class.getName());
	}

	@Override
	public LineSpecificParser createRouteParser() throws ClassNotFoundException, IOException {
		return (RegtoppRouteParser) ParserFactory.create(RegtoppRouteParser.class.getName());
	}

	@Override
	public LineSpecificParser createTripParser() throws ClassNotFoundException, IOException {
		return (RegtoppTripParser) ParserFactory.create(RegtoppTripParser.class.getName());
	}

	@Override
	public Parser createConnectionLinkParser() throws ClassNotFoundException, IOException {
		return (RegtoppConnectionLinkParser) ParserFactory.create(RegtoppConnectionLinkParser.class.getName());
	}

	@Override
	public String[] getMandatoryFileExtensions() {
		return new String[] {
				RegtoppTripIndexTIX.FILE_EXTENSION,
				RegtoppRouteTDA.FILE_EXTENSION,
				RegtoppStopHPL.FILE_EXTENSION,
				RegtoppDayCodeDKO.FILE_EXTENSION };
	}

	@Override
	public String[] getOptionalFileExtensions() {
		return new String[] {
				RegtoppDestinationDST.FILE_EXTENSION,
				RegtoppFootnoteMRK.FILE_EXTENSION,
				RegtoppPathwayGAV.FILE_EXTENSION,
				RegtoppLineLIN.FILE_EXTENSION };
	}

}
