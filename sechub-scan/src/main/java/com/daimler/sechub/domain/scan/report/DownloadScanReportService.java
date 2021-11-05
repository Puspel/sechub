// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.domain.scan.ScanAssertService;
import com.daimler.sechub.domain.scan.SecHubReportProductTransformerService;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.error.NotFoundException;
import com.daimler.sechub.sharedkernel.logging.AuditLogService;
import com.daimler.sechub.sharedkernel.usecases.user.execute.UseCaseUserDownloadsJobReport;
import com.daimler.sechub.sharedkernel.validation.UserInputAssertion;

@Service
public class DownloadScanReportService {

    @Autowired
    ScanAssertService scanAssertService;

    @Autowired
    SecHubReportProductTransformerService secHubResultService;

    @Autowired
    ScanReportRepository reportRepository;

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    UserInputAssertion assertion;

    @UseCaseUserDownloadsJobReport(@Step(number = 3, name = "Resolve scan report result"))
    public ScanSecHubReport getScanSecHubReport(String projectId, UUID jobUUID) {
        /* validate */
        assertion.isValidProjectId(projectId);
        assertion.isValidJobUUID(jobUUID);

        scanAssertService.assertUserHasAccessToProject(projectId);
        scanAssertService.assertProjectAllowsReadAccess(projectId);

        /* audit */
        auditLogService.log("starts download of report for job: {}", jobUUID);

        
        ScanReport report = reportRepository.findBySecHubJobUUID(jobUUID);

        if (report == null) {
            throw new NotFoundException("Report not found or you have no access to report!");
        }
        scanAssertService.assertUserHasAccessToReport(report);

        return new ScanSecHubReport(report);
    }

}
