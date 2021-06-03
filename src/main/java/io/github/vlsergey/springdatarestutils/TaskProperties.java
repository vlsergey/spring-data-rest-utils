package io.github.vlsergey.springdatarestutils;

import java.util.List;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;

@Data
public class TaskProperties {

    private boolean addXLinkedEntity;

    private boolean addXSortable;

    private String basePackage;

    private String enumSuffix;

    private Info info;

    private int linkDepth;

    private String linkTypeName;

    private String outputUri;

    private String repositoryDetectionStrategy;

    private List<Server> servers;

    private String typeSuffix;

    private String withLinksTypeSuffix;

}
