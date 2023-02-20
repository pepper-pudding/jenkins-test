#!/usr/bin/env groovy

def call() {
    properties([parameters([
    string(defaultValue: 'test', description: 'test or verify', name: 'testType', trim: false),
    string(defaultValue: 'master', description: '', name: 'branch', trim: false),
    string(defaultValue: 'openjdk8', description: '', name: 'jdk_version', trim: false),
])])

node {
    stage("git") {
        git url: 'https://github.com/hyunil-shin/java-maven-junit-helloworld.git', branch: params.branch
    }
    
    stage('build') {
        // "PATH+JDK=${tool 'openjdk10'}/bin"
        // mvn: mvn-3.3.9, mvn-3.6.0, mvn-3.6.2
        // jdk: openjdk8, openjdk9, openjdk10
        def jdk =  tool params.jdk_version
        //sh 'rm -rf target/'
        withEnv(["PATH+MAVEN=${tool 'mvn-3.8.6'}/bin", 
                "JAVA_HOME=${jdk}"]) {
            sh 'ls -al $JAVA_HOME'
            sh 'javac -version'
            sh 'mvn --version'
            sh script: "mvn clean ${params.testType}", returnStatus: true
        }
    }
    
    stage('report') {
        junit 'target/surefire-reports/*.xml'
        jacoco execPattern: 'target/**.exec'
        
        println(currentBuild.result)
        if(currentBuild.result == "FAILURE" || currentBuild.result == "UNSTABLE") {
            emailext body: "${BUILD_URL}<br>${getTestResult()}", subject: 'test failed', to: 'y.j@nhn.com', attachLog: true, mimeType: 'text/html'
        }
    }
}

import hudson.tasks.test.AbstractTestResultAction

@NonCPS
def getTestResult() {
    AbstractTestResultAction testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    if (testResultAction == null) {
        return "no test result"
    }

    def total = testResultAction.totalCount
    def failed = testResultAction.failCount
    def skipped = testResultAction.skipCount
    
    def summary = ""
    
        summary += """<div><a href='${BUILD_URL}' target="_blank">Test Result</a> (no testrail report)</div>
            total: ${total}, fail: ${failed}, skip: ${skipped}</div>
            """

    def failedTests = testResultAction.getFailedTests();
    def html = "<table><thead><tr><th style='padding: 15px;background: #04AA6D'>Failed Tests</th></tr></thead><tbody>"
    failedTests.each { test ->
        def tr = "<tr><td style='overflow:hidden;border: 1px solid #ddd;padding: 15px'>" + test.fullDisplayName + "</td></tr>"
        html += tr       
    }
    html += "</tbody></table>"
    return summary + html
}
}
