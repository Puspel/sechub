// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.admin.ui.action.pds;

import java.util.Optional;
import java.util.UUID;

import com.daimler.sechub.developertools.admin.DeveloperAdministration.PDSAdministration;
import com.daimler.sechub.developertools.admin.ui.UIContext;
import com.daimler.sechub.developertools.admin.ui.cache.InputCacheIdentifier;

public class FetchPDSJobErrorStreamAction extends AbstractPDSAction {
    private static final long serialVersionUID = 1L;

    public FetchPDSJobErrorStreamAction(UIContext context) {
        super("Fetch PDS Job error stream", context);
    }

    @Override
    protected void executePDS(PDSAdministration pds) {
        
        Optional<String> pdsJobUUID = getUserInput("PDS job uuid", InputCacheIdentifier.PDS_JOBUUID);
        if (!pdsJobUUID.isPresent()) {
            output("canceled - pds job uuid not set");
            return;
        }
        String result = pds.getJobErrorStream(UUID.fromString(pdsJobUUID.get()));
        
        outputAsTextOnSuccess(result);
        
    }

}