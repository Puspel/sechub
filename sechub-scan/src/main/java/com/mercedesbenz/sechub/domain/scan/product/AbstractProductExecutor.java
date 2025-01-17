// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.domain.scan.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.mercedesbenz.sechub.adapter.AdapterExecutionResult;
import com.mercedesbenz.sechub.commons.model.ScanType;
import com.mercedesbenz.sechub.commons.model.SecHubCodeScanConfiguration;
import com.mercedesbenz.sechub.commons.model.SecHubConfigurationModel;
import com.mercedesbenz.sechub.commons.model.SecHubDataConfigurationObject;
import com.mercedesbenz.sechub.commons.model.SecHubDataConfigurationObjectInfo;
import com.mercedesbenz.sechub.commons.model.SecHubDataConfigurationObjectInfoFinder;
import com.mercedesbenz.sechub.commons.model.SecHubDataConfigurationType;
import com.mercedesbenz.sechub.commons.model.SecHubFileSystemConfiguration;
import com.mercedesbenz.sechub.commons.model.SecHubFileSystemContainer;
import com.mercedesbenz.sechub.commons.model.SecHubMessagesList;
import com.mercedesbenz.sechub.commons.model.SecHubSourceDataConfiguration;
import com.mercedesbenz.sechub.domain.scan.NetworkLocationProvider;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetInfoFactory;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetProductServerDataProvider;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetProductServerDataSuppport;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetRegistry.NetworkTargetInfo;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetType;
import com.mercedesbenz.sechub.domain.scan.resolve.NetworkTargetResolver;
import com.mercedesbenz.sechub.sharedkernel.UUIDTraceLogID;
import com.mercedesbenz.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.mercedesbenz.sechub.sharedkernel.execution.SecHubExecutionContext;
import com.mercedesbenz.sechub.sharedkernel.execution.SecHubExecutionException;
import com.mercedesbenz.sechub.sharedkernel.resilience.ResilientActionExecutor;

/**
 * An abstract product executor implementation
 *
 * @author Albert Tregnaghi
 *
 */
public abstract class AbstractProductExecutor implements ProductExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractProductExecutor.class);

    protected final ResilientActionExecutor<ProductResult> resilientActionExecutor;

    @Autowired
    protected NetworkTargetResolver targetResolver;

    private ScanType scanType;

    private ProductIdentifier productIdentifier;

    private SecHubDataConfigurationObjectInfoFinder configObjectFinder;

    private int version;

    protected AbstractProductExecutor(ProductIdentifier productIdentifier, int version, ScanType scanType) {
        if (scanType == null) {
            throw new IllegalArgumentException("Scan type may not be null!");
        }
        if (productIdentifier == null) {
            throw new IllegalArgumentException("Product identifier may not be null!");
        }
        this.scanType = scanType;
        this.productIdentifier = productIdentifier;
        this.version = version;

        /*
         * we create here our own resilient action executor instance - only for this
         * executor instance - without spring injection
         */
        this.resilientActionExecutor = new ResilientActionExecutor<>();
        this.configObjectFinder = new SecHubDataConfigurationObjectInfoFinder();
    }

    public final int getVersion() {
        return version;
    }

    public final ProductIdentifier getIdentifier() {
        return productIdentifier;
    }

    @Override
    public ScanType getScanType() {
        return scanType;
    }

    protected ProductResult updateCurrentProductResult(AdapterExecutionResult adapterResult, ProductExecutorContext executorContext) {
        SecHubMessagesList messagesList = new SecHubMessagesList(adapterResult.getProductMessages());

        ProductResult productResult = executorContext.getCurrentProductResult(); // product result is set by callback
        productResult.setResult(adapterResult.getProductResult());
        productResult.setMessages(messagesList.toJSON());

        return productResult;
    }

    @Override
    public final List<ProductResult> execute(SecHubExecutionContext context, ProductExecutorContext executorContext) throws SecHubExecutionException {
        UUIDTraceLogID traceLogId = context.getTraceLogId();
        SecHubConfiguration config = context.getConfiguration();

        if (!canExecute(config)) {
            LOG.debug("Cannot execute given configuration {}", traceLogId);
            return Collections.emptyList();
        }

        ProductExecutorData data = createExecutorData(context, executorContext, traceLogId);

        configureSourceCodeHandlingIfNecessary(data);
        configureNetworkTargetHandlingIfNecessary(data);

        return startExecution(data);
    }

    private void configureSourceCodeHandlingIfNecessary(ProductExecutorData data) {
        if (scanType != ScanType.CODE_SCAN) {
            return;
        }
        // the information about paths is interesting for debugging but also necessary
        // for our integration tests - see mocked_setup.json
        Set<String> paths = new LinkedHashSet<>();
        data.codeUploadFileSystemFolderPaths = paths;

        SecHubConfiguration configuration = data.getSechubExecutionContext().getConfiguration();
        Optional<SecHubCodeScanConfiguration> codeScanOpt = configuration.getCodeScan();
        if (!codeScanOpt.isPresent()) {
            return;
        }
        SecHubCodeScanConfiguration codeScan = codeScanOpt.get();
        addFileSystemParts(paths, codeScan);
        Set<String> usedNames = codeScan.getNamesOfUsedDataConfigurationObjects();
        if (usedNames.isEmpty()) {
            return;
        }
        List<SecHubDataConfigurationObjectInfo> found = configObjectFinder.findDataObjectsByName(configuration, usedNames);
        for (SecHubDataConfigurationObjectInfo info : found) {
            if (info.getType() != SecHubDataConfigurationType.SOURCE) {
                continue;
            }
            SecHubDataConfigurationObject config = info.getDataConfigurationObject();
            if (!(config instanceof SecHubSourceDataConfiguration)) {
                LOG.warn("source object data was not expected {} but {}", SecHubSourceDataConfiguration.class, config.getClass());
                continue;
            }
            SecHubSourceDataConfiguration sourceDataConfig = (SecHubSourceDataConfiguration) config;
            addFileSystemParts(paths, sourceDataConfig);
        }
    }

    private void addFileSystemParts(Set<String> paths, SecHubFileSystemContainer container) {
        Optional<SecHubFileSystemConfiguration> fileSystemOpt = container.getFileSystem();

        if (!fileSystemOpt.isPresent()) {
            return;
        }
        SecHubFileSystemConfiguration fileSystem = fileSystemOpt.get();

        paths.addAll(fileSystem.getFiles());
        paths.addAll(fileSystem.getFolders());
    }

    protected abstract void customize(ProductExecutorData data);

    protected abstract List<ProductResult> executeByAdapter(ProductExecutorData data) throws Exception;

    private List<ProductResult> startExecution(ProductExecutorData data) throws SecHubExecutionException {

        LOG.debug("Executing {}", data.traceLogId);
        try {
            List<ProductResult> targetResults = new ArrayList<>();

            if (data.networkTargetInfoList == null) {
                /* no special network target handling necessary - so just start one time */
                executeByAdapterAndSetTime(data, targetResults);
            } else {
                /* network target handling necessary */
                if (data.networkTargetInfoList.isEmpty()) {
                    LOG.warn("{} Was not able to execute because not even one target registry info found!", data.traceLogId);
                } else {
                    for (NetworkTargetInfo info : data.networkTargetInfoList) {
                        /* change current registry info */
                        data.currentNetworkTargetInfo = info;

                        executeByAdapterAndSetTime(data, targetResults);
                    }
                }
            }

            return targetResults;

        } catch (SecHubExecutionException e) {
            throw e;
        } catch (Exception e) {
            /*
             * every other exception is wrapped to a SecHub execution exception which is
             * handled
             */
            throw new SecHubExecutionException(getIdentifier() + " execution failed." + data.traceLogId, e);
        }
    }

    private void configureNetworkTargetHandlingIfNecessary(ProductExecutorData data) {
        if (!isUsingNetworkTargets()) {
            return;
        }
        /* check preconditions */
        NetworkTargetProductServerDataProvider networkTargetDataProvider = data.networkTargetDataProvider;
        if (networkTargetDataProvider == null) {
            throw new IllegalStateException("No network target data provider set, but necessary for scantype: " + scanType
                    + "\nInject this at customize method inside " + getClass().getName());
        }

        NetworkLocationProvider networkLocationProvider = data.networkLocationProvider;
        if (networkLocationProvider == null) {
            throw new IllegalStateException("No network location provier set, but necessary for scantype: " + scanType
                    + "\nInject this at customize method inside " + getClass().getName());
        }

        NetworkTargetProductServerDataSuppport networkTargetProductServerDataSupport = new NetworkTargetProductServerDataSuppport(networkTargetDataProvider);
        data.networkTargetProductServerDataSupport = networkTargetProductServerDataSupport;

        NetworkTargetInfoFactory targetInfoFactory = new NetworkTargetInfoFactory(targetResolver, getClass().getSimpleName());

        List<NetworkTargetInfo> targetRegistryInfoList = new ArrayList<>();
        for (NetworkTargetType networkTargetType : NetworkTargetType.values()) {
            if (!networkTargetType.isValid()) {
                continue;
            }
            NetworkTargetInfo infoForThisNetworkTargetType = targetInfoFactory.createInfo(networkTargetType, data.traceLogId, networkLocationProvider,
                    networkTargetProductServerDataSupport);
            if (infoForThisNetworkTargetType.containsAtLeastOneTarget()) {
                targetRegistryInfoList.add(infoForThisNetworkTargetType);
            }
        }
        data.networkTargetInfoList = targetRegistryInfoList;
    }

    private ProductExecutorData createExecutorData(SecHubExecutionContext context, ProductExecutorContext executorContext, UUIDTraceLogID traceLogId) {
        ProductExecutorData data = new ProductExecutorData();
        data.productExecutorContext = executorContext;
        data.sechubExecutionContext = context;
        data.traceLogId = traceLogId;
        if (traceLogId == null) {
            data.traceLogIdAsString = "null";
        } else {
            data.traceLogIdAsString = traceLogId.toString();
        }

        customize(data);

        return data;
    }

    private void executeByAdapterAndSetTime(ProductExecutorData data, List<ProductResult> targetResults) throws Exception {
        LocalDateTime started = LocalDateTime.now();

        List<ProductResult> productResults = executeByAdapter(data);

        if (productResults != null) {
            LocalDateTime ended = LocalDateTime.now();

            for (ProductResult productResult : productResults) {
                productResult.setStarted(started);
                productResult.setEnded(ended);
            }
            targetResults.addAll(productResults);
        }
    }

    private boolean canExecute(SecHubConfigurationModel config) {
        switch (scanType) {

        case CODE_SCAN:
            return config.getCodeScan().isPresent();
        case INFRA_SCAN:
            return config.getInfraScan().isPresent();
        case WEB_SCAN:
            return config.getWebScan().isPresent();
        case LICENSE_SCAN:
            return config.getLicenseScan().isPresent();
        case UNKNOWN:
            return false;
        default:
            return false;

        }
    }

    private boolean isUsingNetworkTargets() {
        switch (scanType) {

        case INFRA_SCAN:
        case WEB_SCAN:
            return true;

        default:
            return false;

        }
    }

    @Override
    public String toString() {
        return "AbstractProductExecutor [" + (productIdentifier != null ? "productIdentifier=" + productIdentifier + ", " : "") + "version=" + version + ", "
                + (scanType != null ? "scanType=" + scanType : "") + "]";
    }

}
