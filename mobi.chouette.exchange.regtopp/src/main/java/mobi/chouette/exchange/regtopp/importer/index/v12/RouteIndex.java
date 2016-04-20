package mobi.chouette.exchange.regtopp.importer.index.v12;

import static mobi.chouette.exchange.regtopp.RegtoppConstant.*;

import mobi.chouette.exchange.regtopp.importer.index.IndexImpl;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppRouteTMS;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileContentParser;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppException;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppImporter;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;

public abstract class RouteIndex extends IndexImpl<RegtoppRouteTMS> {

	public RouteIndex(RegtoppValidationReporter validationReporter, FileContentParser fileParser) throws Exception {
		super(validationReporter, fileParser);
	}

	@Override
	public boolean validate(RegtoppRouteTMS bean, RegtoppImporter dao) {
		boolean result = true;

		if (bean.getDestinationId().equals(DESTINATION_NULL_REF) || dao.getDestinationById().containsKey(bean.getDestinationId())){
			bean.getOkTests().add(RegtoppException.ERROR.TMS_INVALID_OPTIONAL_ID_REFERENCE);
		} else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(RegtoppRouteTMS.FILE_EXTENSION, bean.getRecordLineNumber(), "Destinasjonsnr", bean.getDestinationId(), RegtoppException.ERROR.TMS_INVALID_OPTIONAL_ID_REFERENCE, "Unreferenced id.")));
			result = false;
		}

		if (bean.getRemarkId().equals(FOOTNOTE_NULL_REF) || dao.getFootnoteById().containsKey(bean.getRemarkId())){
			bean.getOkTests().add(RegtoppException.ERROR.TMS_INVALID_OPTIONAL_ID_REFERENCE);
		} else {
			bean.getErrors().add(new RegtoppException(new FileParserValidationError(RegtoppRouteTMS.FILE_EXTENSION, bean.getRecordLineNumber(), "Merknadssnr", bean.getRemarkId(), RegtoppException.ERROR.TMS_INVALID_OPTIONAL_ID_REFERENCE, "Unreferenced id.")));
			result = false;
		}

		return result;
	}

}