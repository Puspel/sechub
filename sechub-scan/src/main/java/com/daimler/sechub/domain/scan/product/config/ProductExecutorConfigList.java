package com.daimler.sechub.domain.scan.product.config;

import java.util.ArrayList;
import java.util.List;

import com.daimler.sechub.commons.model.JSONable;

public class ProductExecutorConfigList implements JSONable<ProductExecutorConfigList>{
    
    private static final ProductExecutorConfigList IMPORTER = new ProductExecutorConfigList();

    private List<ProductExecutorConfig> executorConfigurations = new ArrayList<>();

    private String type = "executorConfigurationList";
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public List<ProductExecutorConfig> getExecutorConfigurations() {
        return executorConfigurations;
    }

    public void setExecutorConfigurations(List<ProductExecutorConfig> executorConfigurations) {
        this.executorConfigurations = executorConfigurations;
    }

    @Override
    public Class<ProductExecutorConfigList> getJSONTargetClass() {
        return ProductExecutorConfigList.class;
    }
    
    public static ProductExecutorConfigList fromJSONString(String json) {
        return IMPORTER.fromJSON(json);
    }
}
