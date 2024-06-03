import groovy.json.JsonOutput
import hudson.model.Result
import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStream
import jenkins.model.Jenkins
import hudson.model.*

// Method to send a POST request using HttpURLConnection
def sendPostRequestToURL(String urlString, String payload) {
    URL url = new URL(urlString)
    HttpURLConnection connection = (HttpURLConnection) url.openConnection()
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setDoOutput(true)

    OutputStream outputStream = connection.getOutputStream()
    outputStream.write(payload.getBytes("UTF-8"))
    outputStream.flush()
    outputStream.close()

    int responseCode = connection.getResponseCode()

    println "Response code: ${responseCode}"

    if (responseCode == HttpURLConnection.HTTP_OK) {
        def response = new StringBuilder()
        def reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))

        String line
        while ((line = reader.readLine()) != null) {
            response.append(line)
        }

        reader.close()
        println "Response body: ${response.toString()}"
    } else {
        println "Request failed"
    }

    connection.disconnect()
}

// Collect userIds of users whose successful builds occurred within the time frame and meet the parameter constraint
def collectUsersInTimeFrame() {
    // Replace with your job name in jenkins
    def jobName = "YOUR_JOB_NAME"
    def currentJob = Jenkins.instance.getItemByFullName(jobName)
    def jobBuilds = currentJob.getBuilds()


    // Get current time
    def currentTimeMillis = System.currentTimeMillis()

    // Time frame (2 minutes in milliseconds)
    def timeFrame = 2 * 60 * 1000

    // Collect userIds of users whose successful builds occurred within the time frame and meet the parameter constraint
    def usersInTimeFrame = []

    // Get the parameters of the most recent build
    def recentBuild = jobBuilds.first()
    def recentParameters = getBuildParameters(recentBuild)
    def causeAction = recentBuild.getAction(hudson.model.CauseAction.class)
    def userId = ""
    if(causeAction != null){
        def causes = causeAction.getCauses()
        causes.each{ cause->
            if (cause instanceof hudson.model.Cause.UserIdCause) {
                userId = cause.getUserId()
            }
        }
    }

    jobBuilds.each { build ->
        def buildStartTimeMillis = build.getTimeInMillis()
        def buildDurationMillis = build.getDuration()
        def buildEndTimeMillis = buildStartTimeMillis + buildDurationMillis

        if(build != recentBuild){
            // Check if the build started within the time frame
            if ((currentTimeMillis - buildStartTimeMillis <= timeFrame) || (currentTimeMillis - buildEndTimeMillis <= timeFrame)) {
                // Check if the build result is success
                if (build.result == Result.SUCCESS || build.isBuilding()) {
                    // Get parameters of the current build
                    def currentParameters = getBuildParameters(build)

                    // Compare parameters of the recent build with the parameters of the current build
                    if (currentParameters == recentParameters) {
                        causeAction = build.getAction(hudson.model.CauseAction.class)
                        if (causeAction != null) {
                            def causes = causeAction.getCauses()
                            causes.each { cause ->
                                if (cause instanceof hudson.model.Cause.UserIdCause) {
                                    if(userId != cause.getUserId())
                                    // Add userId to the list if the build was successful and meets the parameter constraint
                                    usersInTimeFrame.add(cause.getUserId())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Convert list to set to remove duplicates
    return usersInTimeFrame as Set;
}

// Method to get the parameters of a build
def getBuildParameters(build) {
    def parametersAction = build.getAction(hudson.model.ParametersAction.class)
    def parameters = [:]
    if (parametersAction != null) {
        parametersAction.each { parameter ->
            parameters[parameter.name] = parameter.value.toString()
        }
    }
    return parameters
}


// Main script execution
def users = collectUsersInTimeFrame()
def url = 'http://localhost:3978/api/check'
def payload = JsonOutput.toJson([users: users.toList()])

// Send POST request to the endpoint with the list of users
sendPostRequestToURL(url, payload)