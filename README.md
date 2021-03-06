UIA Comm
================

[![Download](https://api.bintray.com/packages/uia4j/maven/uia-comm/images/download.svg)](https://bintray.com/uia4j/maven/uia-comm/_latestVersion)
[![Build Status](https://travis-ci.org/uia4j/uia-comm.svg?branch=master)](https://travis-ci.org/uia4j/uia-comm)
[![codecov](https://codecov.io/gh/uia4j/uia-comm/branch/master/graph/badge.svg)](https://codecov.io/gh/uia4j/uia-comm)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9766faacb361423b9b6e8e95bf3024d6)](https://www.codacy.com/app/gazer2kanlin/uia-comm?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=uia4j/uia-comm&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/github/license/uia4j/uia-comm.svg)](LICENSE)

Socket client, server & RS232 library.

## Android Support

* SocketClient - tested

* SocketServer - working

* RS232 - __NOT SUPPORT__


## Key Abstraction

### Protocol
Provide protocol interface to define structure abstraction.

### Message Manager
Provide message manager to verify and validate I/O message.

### Call In/Out
Define interface to handle I/O message.

## Maven
Because uia.comm uses [uia.utils](https://github.com/uia4j/uia-utils) deployed on jcenter, configure local Maven __settings.xml__ first.

settings.xml in .m2 directory:
```
<profiles>
    <profile>
        <repositories>
            <repository>
                <snapshots>
                    <enabled>false</enabled>
                </snapshots>
                <id>central</id>
                <name>bintray</name>
                <url>http://jcenter.bintray.com</url>
            </repository>
        </repositories>
        <pluginRepositories>
            <pluginRepository>
                <snapshots>
                    <enabled>false</enabled>
                </snapshots>
                <id>central</id>
                <name>bintray-plugins</name>
                <url>http://jcenter.bintray.com</url>
            </pluginRepository>
        </pluginRepositories>
        <id>bintray</id>
    </profile>
</profiles>
<activeProfiles>
    <activeProfile>bintray</activeProfile>
</activeProfiles>
```
pom.xml in your project:
```
<dependency>
    <groupId>uia</groupId>
    <artifactId>uia.comm</artifactId>
    <version>0.2.1</version>
</dependency>
```

## Dependency Libraries

* [uia.utils](https://github.com/uiaj4/uia-utils) - UIA common utilities

## Copyright and License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
