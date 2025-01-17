/*
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.antgroup.geaflow.console.core.service.version;

import com.antgroup.geaflow.console.common.util.exception.GeaflowException;
import com.antgroup.geaflow.console.common.util.proxy.ProxyUtil;
import com.antgroup.geaflow.console.core.model.file.GeaflowJarPackage;
import com.antgroup.geaflow.console.core.model.version.GeaflowVersion;
import com.antgroup.geaflow.console.core.service.file.VersionFileFactory;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VersionClassLoader {

    protected final GeaflowVersion version;

    protected final VersionFileFactory versionFileFactory;

    protected final URLClassLoader classLoader;

    protected VersionClassLoader(GeaflowVersion version, VersionFileFactory versionFileFactory) {
        this.version = version;
        this.versionFileFactory = versionFileFactory;
        this.classLoader = createClassLoader();
    }

    public <T> T newInstance(Class<T> clazz, Object... parameters) {
        return ProxyUtil.newInstance(classLoader, clazz, parameters);
    }

    protected void closeClassLoader() {
        try {
            versionFileFactory.deleteVersionFile(version.getName(), version.getEngineJarPackage());
            if (version.getLangJarPackage() != null) {
                versionFileFactory.deleteVersionFile(version.getName(), version.getLangJarPackage());
            }
            classLoader.close();
            log.info("Close classloader of version {}", version.getName());

        } catch (Exception e) {
            log.info("Close classloader of version {} failed", version.getName(), e);
        }
    }

    private URLClassLoader createClassLoader() {
        try {
            String versionName = version.getName();
            GeaflowJarPackage engineJarPackage = version.getEngineJarPackage();
            GeaflowJarPackage langJarPackage = version.getLangJarPackage();
            if (engineJarPackage == null) {
                throw new GeaflowException("Engine jar not found in version {}", versionName);
            }

            // prepare engine jar file
            List<URL> urlList = new ArrayList<>();
            File engineJarFile = versionFileFactory.getVersionFile(versionName, engineJarPackage);
            urlList.add(engineJarFile.toURI().toURL());

            // prepare lang jar file
            if (langJarPackage != null) {
                File langJarFile = versionFileFactory.getVersionFile(versionName, langJarPackage);
                urlList.add(langJarFile.toURI().toURL());
            }

            // create classloader
            ClassLoader extClassLoader = ClassLoader.getSystemClassLoader().getParent();
            return new URLClassLoader(urlList.toArray(new URL[]{}), extClassLoader);

        } catch (Exception e) {
            throw new GeaflowException("Create classloader of version {} failed", version.getName(), e);
        }
    }
}
