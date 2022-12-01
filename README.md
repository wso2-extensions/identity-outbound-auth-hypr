# identity-outbound-auth-hypr

Documentation for building and installing the HYPR components in the IS.

---

## Setup and Installing HYPR Authenticator

**Step 1:** Cloning the project

Clone the `identity-outbound-auth-hypr` repository

**Step 2:** Building the project

Build the project by running `mvn clean install` at the root directory

**Step 3:** Deploying the HYPR Authenticator

- Go to `identity-outbound-auth-hypr/components` →
    - `org.wso2.carbon.identity.application.authenticator.hypr` → `target`
- Copy the `.jar` file
- Go to `<IS_HOME>/repository/components/dropins`
- Paste the `.jar` file into the dropins directory
- Alternatively it's possible to drag and drop the `.jar` file to the dropins directory

**Step 4:** Deploying the HYPR REST API
- Go to `identity-outbound-auth-hypr/components` → `org.wso2.carbon.identity.application.authenticator.hypr.rest`
    - `org.wso2.carbon.identity.application.authenticator.hypr.rest.common`→ `target`
- Copy the `.jar` file
- Go to `<IS_HOME>/repository/deployment/server/webapps/api/WEB-INF/lib`
- Paste the `.jar` file into the lib directory
- Similarly, repeat the above steps for the components;
    - `org.wso2.carbon.identity.application.authenticator.hypr.rest.dispatcher`
    - `org.wso2.carbon.identity.application.authenticator.hypr.rest.v1`

**Step 5:** Deploying HYPR login Page
- Go to `identity-outbound-auth-hypr/components` →
    - `org.wso2.carbon.identity.application.authenticator.hypr` → `src` → `main` → `resources` → `artifacts`
- Copy `hyprlogin.jsp`
- Go to `<IS_HOME>/repository/deployment/server/webapps` → `authenticationendpoint`
- Paste or drop the `JSP` file in the `authenticationendpoint` directory

**Step 6:**
- Go to `<IS_HOME>/repository/deployment/server/webapps/api/WEB-INF`
- Open `bean.xml`
- Add the following lines of codes

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

**Step 7:**
- Go to `<IS_HOME>/repository/resources/conf/templates/repository/conf/identity`
- Open `identity.xml.j2`
- Scroll down to the “ResourceAccessControl” section

Add the following lines for setting access control for hypr rest api
```xml
<ResourceAccessControl>
  <Resource context="(.*)/api/hypr/v1/authentication/(.*)" secured="false" http-method="GET"/>
</ResourceAccessControl>
```



