package mobi.chouette.exchange.netexprofile.importer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "netexprofile-import")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"parseSiteFrames", "validateAgainstSchema", "validateAgainstProfile", "continueOnLineErrors", "cleanOnErrors", "objectIdPrefix"})
public class NetexprofileImportParameters extends AbstractImportParameter {

	@Getter
	@Setter
	@XmlElement(name = "parse_site_frames", required = false)
	private boolean parseSiteFrames = true;

	@Getter
	@Setter
	@XmlElement(name = "validate_against_schema", required = false)
	private boolean validateAgainstSchema = true;

	@Getter
	@Setter
	@XmlElement(name = "validate_against_profile", required = false)
	private boolean validateAgainstProfile = true;

	@Getter
	@Setter
	@XmlElement(name = "continue_on_line_errors", required = false)
	private boolean continueOnLineErrors = false;

	@Getter
	@Setter
	@XmlElement(name = "clean_on_error", required = false)
	private boolean cleanOnErrors = false;
	
	@Getter@Setter
	@XmlElement(name = "object_id_prefix", required=true)
	private String objectIdPrefix;


}
