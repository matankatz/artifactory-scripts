#!/usr/bin/env groovy
import groovy.transform.Field
import groovyx.net.http.HttpResponseException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.Repository
import org.jfrog.artifactory.client.model.builder.LocalRepositoryBuilder
import org.jfrog.artifactory.client.model.builder.RemoteRepositoryBuilder
import org.jfrog.artifactory.client.model.builder.VirtualRepositoryBuilder
import org.jfrog.artifactory.client.model.impl.CustomPackageTypeImpl
import org.jfrog.artifactory.client.model.repository.settings.impl.*

@GrabResolver(name = 'jcenter', root = 'http://jcenter.bintray.com/')
@Grapes([
        @Grab(group = 'org.jfrog.artifactory.client', module = 'artifactory-java-client-api', version = '2.5.2'),
        @Grab(group = 'org.jfrog.artifactory.client', module = 'artifactory-java-client-services', version = '2.5.2'),
        @GrabExclude(group = 'org.codehaus.groovy', module = 'groovy-xml')])

//Variables
@Field Artifactory artifactory
@Field String artifactoryUrl
@Field String artifactoryUser
@Field String artifactoryPassword
@Field Map<String, List> repositorySettings

def usage() {
    println("Usage:")
    println("createRepositories.groovy <artifactory url> <admin user> <admin password> <all| list of package types seperated by comma>")
}

def validateArgs() {
    if (args.size() < 4) {
        usage()
        System.exit(1)
    }

    if (!args[0].startsWith("http")) {
        usage()
        System.exit(1)
    }

    artifactoryUrl = args[0]
    artifactoryUser = args[1]
    artifactoryPassword = args[2]
}

void initArtifactoryClient() {
    artifactory = ArtifactoryClientBuilder.create()
            .setUrl(artifactoryUrl)
            .setUsername(artifactoryUser)
            .setPassword(artifactoryPassword)
            .build()
}

void initRepositories() {
    repositorySettings = [
            "bower"    : [new BowerRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://github.com/"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "chef"     : [new ChefRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://supermarket.chef.io"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "cocoapods": [new CocoaPodsRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://github.com/")],
            "conan"    : [new ConanRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder()],
            "debian"   : [new DebianRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("http://archive.ubuntu.com/ubuntu/")],
            "docker"   : [new DockerRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://registry-1.docker.io/"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "gems"     : [new GemsRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://rubygems.org/"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "gitlfs"   : [new GitLfsRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder()],
            "gradle"   : [new GradleRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "helm"     : [new CustomRepositorySettingsImpl(new CustomPackageTypeImpl("helm")), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://storage.googleapis.com/kubernetes-charts"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "ivy"      : [new IvyRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "maven"    : [new MavenRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "npm"      : [new NpmRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://registry.npmjs.org"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "nuget"    : [new NugetRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://www.nuget.org/"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "opkg"     : [new OpkgRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://downloads.openwrt.org/chaos_calmer/15.05.1/")],
            "composer" : [new ComposerRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://github.com/")],
            "pypi"     : [new PypiRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://pypi.python.org"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "puppet"   : [new PuppetRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://forgeapi.puppetlabs.com/"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "sbt"      : [new SbtRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "vagrant"  : [new VagrantRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder()],
            "rpm"      : [new RpmRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "generic"  : [new GenericRepositorySettingsImpl(), artifactory.repositories().builders().localRepositoryBuilder(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "p2"       : [new P2RepositorySettingsImpl(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://jcenter.bintray.com"), artifactory.repositories().builders().virtualRepositoryBuilder()],
            "vcs"      : [new VcsRepositorySettingsImpl(), artifactory.repositories().builders().remoteRepositoryBuilder().url("https://github.com/")]
    ]
}

void validateArtifactoryAvailability() {
    if (! artifactory.system().ping()) {
        System.err.println("Artifactory is not available. either provided URL is wrong, or username and password are incorrect.")
        System.exit(1)
    }
}

void createRepositories() {
    if (args[3] == "all") {
        repositorySettings.each { type, value ->
            List createdRepos = []
            for (int i = 1; i < value.size(); i++) {
                String repoName = type
                if (value[i] instanceof LocalRepositoryBuilder) {
                    repoName += "-local"
                    createdRepos.add(repoName)
                } else if (value[i] instanceof RemoteRepositoryBuilder) {
                    repoName += "-remote"
                    createdRepos.add(repoName)
                } else if (value[i] instanceof VirtualRepositoryBuilder) {
                    value[i].repositories(createdRepos)
                }
                Repository repo = value[i].key(repoName).repositorySettings(value[0]).build()
                try{
                    artifactory.repositories().create(0, repo)
                } catch (HttpResponseException hre) {
                    if (hre.statusCode == 400 && ! hre.response.data.text.contains("already exists")) {
                        throw hre
                    }
                }
            }
        }
    }
}

validateArgs()
initArtifactoryClient()
initRepositories()
validateArtifactoryAvailability()
createRepositories()
