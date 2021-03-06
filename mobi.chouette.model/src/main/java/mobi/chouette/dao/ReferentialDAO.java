package mobi.chouette.dao;

import mobi.chouette.model.dto.ReferentialInfo;
import org.checkerframework.checker.i18n.qual.LocalizableKey;

import java.time.LocalDateTime;
import java.util.List;


public interface ReferentialDAO {

    List<String> getReferentials();

    void createReferential(ReferentialInfo referentialInfo);

    void createMigratedReferential(ReferentialInfo referentialInfo);

    boolean updateReferential(ReferentialInfo referentialInfo);

    boolean updateMigratedReferential(ReferentialInfo referentialInfo);

    boolean deleteReferential(ReferentialInfo referentialInfo);

    LocalDateTime getLastUpdateTimestamp();

    void setLastUpdateTimestamp(LocalDateTime lastUpdateTimestamp);



}
