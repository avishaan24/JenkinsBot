import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStream

def sendPostRequest(String urlString, String payload) {
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

def url = "http://localhost:3978/api/notify"
def buildUser = manager.envVars["BUILD_USER"]
def buildUserId = manager.envVars["BUILD_USER_ID"]
def buildUserEmail = manager.envVars["BUILD_USER_EMAIL"]
def buildNumber = manager.envVars['BUILD_NUMBER']
def buildUrl =manager.envVars['BUILD_URL']

def buildResult = manager.build.result
def payload = "{\"build_result\": \"${buildResult}\", \"build_user\": \"${buildUser}\", \"build_user_id\": \"${buildUserId}\", \"build_user_email\": \"${buildUserEmail}\", \"build_number\": \"${buildNumber}\", \"build_url\": \"${buildUrl}\"}"



sendPostRequest(url, payload)