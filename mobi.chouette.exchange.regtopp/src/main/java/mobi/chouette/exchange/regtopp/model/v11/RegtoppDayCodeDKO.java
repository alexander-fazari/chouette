package mobi.chouette.exchange.regtopp.model.v11;

import java.io.Serializable;

import org.beanio.annotation.Field;
import org.beanio.annotation.Record;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.regtopp.model.RegtoppObject;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Record(name = "entry", minOccurs = 1, maxOccurs = Integer.MAX_VALUE, order = 3)
public class RegtoppDayCodeDKO extends RegtoppObject implements Serializable {

	public static final String FILE_EXTENSION = "DKO";

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	@Field(at = 0, length = 3)
	private String adminCode;

	@Getter
	@Setter
	@Field(at = 3, length = 1)
	private String counter;

	@Getter
	@Setter
	@Field(at = 4, length = 4)
	private String dayCodeId;

	@Getter
	@Setter
	@Field(at = 8, length = 1, minLength = 0,regex = "[01]", maxOccurs = 500)
	private char[] dayCodeHack;

	public String getDayCode() {
		return new String(dayCodeHack).trim();
	}

	@Override
	public String getIndexingKey() {
		return adminCode + counter + dayCodeId;
	}
}
