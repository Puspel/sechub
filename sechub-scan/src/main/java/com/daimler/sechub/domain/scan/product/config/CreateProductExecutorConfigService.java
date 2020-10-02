package com.daimler.sechub.domain.scan.product.config;

import static com.daimler.sechub.sharedkernel.validation.AssertValidation.*;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.daimler.sechub.sharedkernel.Profiles;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.logging.AuditLogService;
import com.daimler.sechub.sharedkernel.usecases.admin.config.UseCaseAdministratorAddsExecutorConfiguration;

@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
@Profile(Profiles.ADMIN_ACCESS)
@Service
public class CreateProductExecutorConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(CreateProductExecutorConfigService.class);

    @Autowired
    ProductExecutorConfigRepository repository;

    @Autowired
    ProductExecutorConfigValidation validation;
    
    @Autowired
    AuditLogService auditLogService;

    /* @formatter:off */
    @UseCaseAdministratorAddsExecutorConfiguration(
            @Step(number = 1, 
            name = "Service call", 
            description = "Service creates a new product executor configuration with empty setup and being not enabled"))
    /* @formatter:on */
    public UUID createProductExecutorConfig(ProductExecutorConfig configFromUser) {
        
        auditLogService.log("Wants to create product execution configuration '{}' for executor:{}, V{}", configFromUser.getName(), configFromUser.getProductIdentifier(), configFromUser.getExecutorVersion());
        
        ProductExecutorConfig config = new ProductExecutorConfig();
        config.setEnabled(false); // must be done later when also setup is configured etc.
        config.setExecutorVersion(configFromUser.getExecutorVersion());
        config.setProductIdentifier(configFromUser.getProductIdentifier());
        config.setName(configFromUser.getName());
        
        prepareInitialSetup(config);
        
        assertValid(config, validation);
        

        ProductExecutorConfig stored = repository.save(config);
        UUID uuid = stored.getUUID();

        LOG.info("Created product execution configuration '{}' with uuidD={} for executor:{}, V{}", stored.getName(), uuid, stored.getProductIdentifier(),stored.getExecutorVersion());

        return uuid;

    }

    private void prepareInitialSetup(ProductExecutorConfig config) {
        /*
         * TODO Albert Tregnaghi, 2020-07-23: maybe later we could directly ask products
         * about mandatory parts when supported (like PDS does)
         */

        /* create a simple empty setup */
        ProductExecutorConfigSetup setup = new ProductExecutorConfigSetup();
        config.setSetup(setup.toJSON());

    }


}
