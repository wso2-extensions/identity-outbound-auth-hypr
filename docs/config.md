# Configuring the HYPR Authenticator
To use the HYPR authenticator with WSO2 Identity Server, first you need to configure  the authenticator with
WSO2 Identity Server. The following steps provide instructions on how to configure the HYPR authenticator with
WSO2 Identity Server using a sample app.

In order to test the approach, First an end user account should be registered in the Application created via the 
HYPR Control Center and the end user should have the HYPR mobile application installed in the smartphone, as well as 
the smartphone registered with the end user account.

After deploying the HYPR Authenticator to WSO2 IS, the Authenticator can be configured from the
WSO2 IS Console.

## Requirements
To use the connector, you'll need:

- A configured HYPR environment.
- A HYPR user account and mobile app to use for testing.

Note: Get the support from HYPR to configure a HYPR Application via the HYPR Control Center.

## Setup and Installing HYPR Connector

**Step 1:** Extract the project artifacts
- Clone the `identity-outbound-auth-hypr` repository.
- Build the project by running mvn clean install the root directory.

Note : The latest project artifacts can also be downloaded from the Connector Store.

**Step 2:** Deploying the HYPR Authenticator

- Navigate to `identity-outbound-auth-hypr/components` → `org.wso2.carbon.identity.application.authenticator.hypr` 
→ `target`.
- Copy the `org.wso2.carbon.identity.application.authenticator.hypr-1.0.0-SNAPSHOT.jar` file.
- Navigate to `<IS_HOME>/repository/components/dropins`.
- Paste the `.jar` file into the dropins directory.
- Alternatively it's possible to drag and drop the `.jar` file to the dropins directory.
- Similarly navigate to `identity-outbound-auth-hypr/components` → 
`org.wso2.carbon.identity.application.authenticator.hypr.common` → `target`.
- Copy the `org.wso2.carbon.identity.application.authenticator.hypr.common-1.0.0-SNAPSHOT.jar` file.
- Navigate to `<IS_HOME>/repository/components/dropins`.
- Paste the `.jar` file into the dropins directory.

**Step 3:** Deploying the HYPR REST API
- Navigate to `identity-outbound-auth-hypr/components` → `org.wso2.carbon.identity.application.authenticator.hypr.rest` 
- -> `org.wso2.carbon.identity.application.authenticator.hypr.rest.common`→ `target`.
- Copy the `org.wso2.carbon.identity.application.authenticator.hypr.rest.common-1.0.0-SNAPSHOT.jar` file.
- Navigate to `<IS_HOME>/repository/deployment/server/webapps/api/WEB-INF/lib`.
- Paste the `.jar` file into the lib directory.
- Similarly, repeat the above steps for the components;
    - `org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher`
    - `org.wso2.carbon.identity.application.authenticator.hypr.rest.v1`

**Step 4:** Deploying HYPR login Page
- Copy `hyprlogin.jsp` in the downloaded artifacts.
- Navigate to `<IS_HOME>/repository/deployment/server/webapps` → `authenticationendpoint`.
- Paste or drop the `JSP` file in the `authenticationendpoint` directory.

**Step 5:** Updating the core files in WSO2 Identity Server
- Navigate to `<IS_HOME>/repository/deployment/server/webapps/api/WEB-INF`.
- Open `bean.xml`.
- Add the following lines of codes.

```xml
<import resource="classpath:META-INF/cxf/hypr-server-v1-cxf.xml"/>
```
```xml
<jaxrs:server id="hyprServer" address="/hypr/v1">
  <jaxrs:serviceBeans>
    <bean class="org.wso2.carbon.identity.application.authenticator.hypr.rest.v1.AuthenticationApi"/>
  </jaxrs:serviceBeans>
  <jaxrs:providers>
    <bean class="com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider">
      <constructor-arg>
        <bean class="com.fasterxml.jackson.databind.ObjectMapper">
          <property name="serializationInclusion" value="NON_NULL"/>
        </bean>
      </constructor-arg>
    </bean>
    <bean class="com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider"/>
    <bean class="org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.JsonProcessingExceptionMapper"/>
    <bean class="org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.APIErrorExceptionMapper"/>
    <bean class="org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.InputValidationExceptionMapper"/>
    <bean class="org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher.DefaultExceptionMapper"/>
  </jaxrs:providers>
</jaxrs:server>
```

- Navigate to `<IS_HOME>/repository/resources/conf/templates/repository/conf/identity`.
- Open `identity.xml.j2`.
- Scroll down to the “ResourceAccessControl” section.
- Add the following lines for setting access control for hypr rest api inside the “<ResourceAccessControl>” config.

```xml
  <Resource context="(.*)/api/hypr/v1/authentication/(.*)" secured="false" http-method="GET"/>
```


### _The WSO2 Console’s UI for HYPR authenticator section as follows_
![Configuring HYPR in WSO2 Console](images/wso2Console.png)

#### Base URL
This refers to the Base URL  you received from the HYPR upon creating a tenant for your organization.
Example :
```
https://<organization name>.hypr.com
```

#### Relying Party App ID
This refers to the App ID you received for the application you created in the HYPR Control Center.
Example :
```
<Application ID of the HYPR App>
```
Follow the following steps to extract the App ID From the HYPR Control Center.
- Navigate to the HYPR Control Central via the link provided to you from HYPR and log in using your HYPR credentials.
- Select your app from the `Choose an App` drop-down list. Next you are navigated to the Dashboard of the application. 
- Finally, click the App Settings in the top right corner. Note down the App ID.

#### API Token
This refers to an API token newly generated specifically for the HYPR App via the control center.
Example :
```
<Generated API Token for the HYPR App>
```
Follow the following steps to generate a new API token From the HYPR Control Center.
- Navigate to the HYPR Control Central via the link provided to you from HYPR and log in using your HYPR credentials.
- Select your app from the `Choose an App` drop-down list. Next you are navigated to the Dashboard of the application.
- Under `Advanced Config` click Access Token.
- Then click on `Generate Token`.
- Finally, add a new name to your API token,  select API Access Token from selective options provided and click on `Create Token`.

### _The HYPR authenticator's flow as follows_
![HYPR Authentication Demo Flow](images/HYPRAuthenticatorDemoFlow.png)