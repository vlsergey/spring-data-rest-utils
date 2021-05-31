package io.github.vlsergey.springdatarestutils;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class PluginImpl implements Plugin<Project> {

    @Override
    public void apply(Project project) {
	final PluginProperties ownConfig = project.getExtensions().create("springdatarestutils",
		PluginProperties.class);

	project.task("generateOpenAPIForSpringDataREST", (Task task) -> {
//	    task.dependsOn(":compileJava");
	    task.doLast(new ToOpenApiAction(project, ownConfig));
	});
    }
}
