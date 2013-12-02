/**
 * Copyright 2013 BancVue, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bancvue.gradle.maven.publish

import com.bancvue.gradle.GradlePluginMixin
import com.bancvue.gradle.JavaExtPlugin
import com.bancvue.gradle.license.LicenseExtPlugin
import com.bancvue.gradle.license.LicenseExtProperties
import com.bancvue.gradle.license.LicenseModel
import com.bancvue.gradle.maven.MavenRepositoryProperties
import com.bancvue.gradle.test.TestExtPlugin
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

@Slf4j
@Mixin(GradlePluginMixin)
class MavenPublishExtPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'maven-publish-ext'

	private Project project
	private MavenRepositoryProperties repositoryProperties

	public void apply(Project project) {
		this.project = project
		this.repositoryProperties = new MavenRepositoryProperties(project)
		project.apply(plugin: JavaExtPlugin.PLUGIN_NAME)
		addArtifactDependencyAndPublishingSupport()
		addPublishingExtExtension()
	}

	private void addPublishingExtExtension() {
		project.extensions.create(MavenPublishExtExtension.NAME, MavenPublishExtExtension, project)
	}

	private void addArtifactDependencyAndPublishingSupport() {
		project.apply(plugin: 'maven-publish')
		renamePublishTasks()
		addMavenLocalAndOrganizationArtifactRepository()
		addOrganizationPublishingRepository()
	}

	private void renamePublishTasks() {
		renamePublishTaskToPublishRemote()
		renamePublishToMavenLocalTaskToPublish()
	}

	private void renamePublishTaskToPublishRemote() {
		Task publish = project.tasks.findByName('publish')
		project.tasks.remove(publish)
		publish.description = 'Publishes all publications produced by this project.'
		renameTask(publish, 'publishRemote')
	}

	private void renamePublishToMavenLocalTaskToPublish() {
		Task publishToMavenLocal = project.tasks.findByName('publishToMavenLocal')
		publishToMavenLocal.description = 'Publishes all Maven publications produced by this project to the local Maven cache.'
		renameTask(publishToMavenLocal, 'publish')
	}

	private void addMavenLocalAndOrganizationArtifactRepository() {
		project.repositories.mavenLocal()
		project.repositories {
			maven {
				name repositoryProperties.name
				url repositoryProperties.publicUrl
			}
		}
	}

	private boolean isSnapshotProject() {
		project.version =~ /SNAPSHOT$/
	}

	private String acquireRepositoryPublishUrl() {
		if (isSnapshotProject()) {
			repositoryProperties.snapshotUrl
		} else {
			repositoryProperties.releaseUrl
		}
	}

	private void addOrganizationPublishingRepository() {
		project.publishing {
			repositories {
				maven {
					name repositoryProperties.name
					url acquireRepositoryPublishUrl()
					if (repositoryProperties.hasCredentialsDefined()) {
						credentials {
							username repositoryProperties.username
							password repositoryProperties.password
						}
					}
				}
			}
		}
	}

}