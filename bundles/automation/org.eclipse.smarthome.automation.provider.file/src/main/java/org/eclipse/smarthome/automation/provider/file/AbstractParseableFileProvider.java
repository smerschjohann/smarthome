/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.provider.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.service.file.AbstractFileProvider;

/**
 * This class is base for {@link ModuleTypeProvider} and {@link TemplateProvider}, responsible for importing the
 * automation objects from local file system.
 * <p>
 * It provides functionality for tracking {@link Parser} services and provides common functionality for notifying the
 * {@link ProviderChangeListener}s for adding, updating and removing the {@link ModuleType}s or {@link Template}s.
 *
 * @author Ana Dimova - Initial Contribution
 * @author Simon Merschjohann - Refactoring
 *
 */
public abstract class AbstractParseableFileProvider<E> extends AbstractFileProvider<E> {

    protected String defaultFormat = Parser.FORMAT_JSON;

    /**
     * This Map provides structure for fast access to the {@link Parser}s. This provides opportunity for high
     * performance at runtime of the system.
     */
    private Map<String, Parser<E>> parsers = new ConcurrentHashMap<String, Parser<E>>();

    /**
     * This Map holds URL resources that waiting for a parser to be loaded.
     */
    private Map<String, List<URL>> urls = new ConcurrentHashMap<String, List<URL>>();

    public AbstractParseableFileProvider(String root) {
        super(root, new String[] { "automation" });
    }

    @Override
    public void deactivate() {
        super.deactivate();
        parsers.clear();
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties
     */
    public void addParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.put(parserType, parser);
        List<URL> value = urls.remove(parserType);
        if (value != null && !value.isEmpty()) {
            for (URL url : value) {
                importFile(parserType, url);
            }
        }
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties
     */
    public void removeParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.remove(parserType);
    }

    @Override
    protected void importFile(URL url) {
        String parserType = getParserType(url);
        importFile(parserType, url);
    }

    /**
     * This method is responsible for importing a set of Automation objects from a specified URL resource.
     *
     * @param parserType is relevant to the format that you need for conversion of the Automation objects in text.
     * @param url a specified URL for import.
     */
    protected void importFile(String parserType, URL url) {
        Parser<E> parser = parsers.get(parserType);
        if (parser != null) {
            InputStream is = null;
            InputStreamReader inputStreamReader = null;
            try {
                is = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                inputStreamReader = new InputStreamReader(bis);
                Set<E> providedObjects = parser.parse(inputStreamReader);
                updateProvidedObjectsHolder(url, providedObjects);
            } catch (ParsingException e) {
                logger.debug(e.getMessage(), e);
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            } finally {
                if (inputStreamReader != null) {
                    try {
                        inputStreamReader.close();
                    } catch (IOException e) {
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else {
            synchronized (urls) {
                List<URL> value = urls.get(parserType);
                if (value == null) {
                    value = new ArrayList<URL>();
                    urls.put(parserType, value);
                }
                value.add(url);
            }
            logger.debug("Parser {} not available", parserType, new Exception());
        }
    }

    private String getParserType(URL url) {
        String fileName = url.getPath();
        if (fileName.lastIndexOf(".") == -1) {
            return defaultFormat;
        }
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (fileExtension.equals("txt")) {
            return defaultFormat;
        }
        return fileExtension;
    }
}
