package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ToOpenApiAction implements Action<Task> {

    private final PluginProperties pluginProperties;
    private final Project project;

    public ToOpenApiAction(Project project, PluginProperties pluginProperties) {
	this.project = project;
	this.pluginProperties = pluginProperties;
    }

    @Override
    @SneakyThrows
    public void execute(Task taskImpl) {
	final List<URL> urls = new ArrayList<>();

	SourceSetContainer sourceSetContainer = project.getConvention().getByType(SourceSetContainer.class);
	sourceSetContainer.getAsMap().forEach((String name, SourceSet set) -> {
	    final Consumer<? super File> addToClassPath = file -> {
		try {
		    final URL url = file.toURI().toURL();
		    urls.add(url);
		    log.debug("Added to classPath: {}", url);
		} catch (MalformedURLException e) {
		    e.printStackTrace();
		}
	    };

	    set.getCompileClasspath().forEach(addToClassPath);
	    set.getOutput().getClassesDirs().forEach(addToClassPath);
	});

	urls.add(ToOpenApiAction.class.getProtectionDomain().getCodeSource().getLocation());

	try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
		ClassLoader.getSystemClassLoader())) {

	    final AtomicReference<Throwable> excHolder = new AtomicReference<>(null);
	    final String propsJson = new ObjectMapper().writeValueAsString(pluginProperties.toTaskProperties());
	    final Object[] constructorArgs = new Object[] { project.getDisplayName(), project.getVersion().toString(),
		    propsJson };

	    final Thread thread = new Thread(() -> {
		try {
		    final Class<?> implClass = Class.forName(ToOpenApiAction.class.getName() + "Impl", true,
			    classLoader);
		    final Constructor<?> constructor = implClass.getConstructor(String.class, String.class,
			    String.class);
		    final Object impl = constructor.newInstance(constructorArgs);
		    implClass.getMethod("executeWithinUrlClassLoader").invoke(impl);
		} catch (Exception exc) {
		    throw new RuntimeException(exc);
		}
	    }, this.getClass().getName());
	    thread.setContextClassLoader(classLoader);
	    thread.setUncaughtExceptionHandler((t, exc) -> {
		log.error(exc.getMessage(), exc);
		excHolder.set(exc);
	    });
	    thread.start();
	    thread.join();

	    final Throwable exc = excHolder.get();
	    if (exc != null) {
		throw exc;
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}