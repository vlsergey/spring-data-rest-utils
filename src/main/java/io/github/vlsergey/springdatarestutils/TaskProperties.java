package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;

@Data
public class TaskProperties {

    private boolean addXLinkedEntity = false;

    private boolean addXSortable = false;

    private String basePackage = null;

    private String baseTypePrefix = "Base";

    private String defaultTypeSuffix = "";

    private String enumTypeSuffix = "";

    private Info info = new Info();

    private String linksTypeSuffix = "Links";

    private String linkTypeName = "Link";

    private String outputUri = new File("api.yaml").toURI().toString();

    private String patchTypeSuffix = "Patch";

    private String repositoryDetectionStrategy = "DEFAULT";

    private String requestTypeSuffix = "Request";

    private List<Server> servers = new ArrayList<>(singletonList(new Server().url("/api")));

    private String withLinksTypeSuffix = "WithLinks";

}
