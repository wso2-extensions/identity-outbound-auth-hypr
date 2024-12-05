# Configuring the HYPR 

You need to configure the HYPR environment and have access to the HYPR control center. Learn how to do it in the [HYPR documentation](https://docs.hypr.com/docs/cc/intro-cc){:target="_blank"}.

## Register application in HYPR

Follow the steps below to register your application in the HYPR control center.

!!! note
You can follow the [HYPR documentation](https://docs.hypr.com/docs/cc/ccInstallCfg/ccInstallCfgAppMgmt/cc-install-cfg-app-mgmt-new){:target="_blank"} for detailed instructions.

1. Go to the HYPR control center and click **Add Application**.

2. Select **Web** as the channel and click **Next**.

3. Select **Custom Solution** as the IdP provider and click **Next**.

4. Enable push notifications and click **Next**.

5. Add your Firebase configurations and click **Next**.

   !!! note
   To enable push notifications, you need to configure Firebase and obtain a project ID and an API key. To learn how to do this, follow the [guide in HYPR](https://docs.hypr.com/docs/cc/ccInstallCfg/ccInstallCfgAppMgmt/cc-install-cfg-app-mgmt-new-configuring-push-notifications-firebase){:target="_blank"}.

6. Complete the app details form and click **Done** to create the application.

7. Select your application from the **Choose an App** menu and note down the App ID.

## Register an End User Account in the Application 

1. Select the created RP application in the HYPR Control Center.

2. Navigate to the Magic Links page from Advanced configs in the side navbar, provide a username, and create a magic link.
3. Copy the generated magic link and redirect to the login page.
4. Select the smartphone and scan the QR code with the HYPR mobile application.

## Create an API token in HYPR

When you register HYPR as a connection in IS, you need to provide an API token, which IS can use to access HYPR APIs.

Follow the steps below to obtain an API token.

1. Go to the HYPR control center and select your application.

2. Under **Advanced Config**, click **Access Tokens**.

3. Click **Create Token**, provide a unique name for your token, select **API Token** as the token type, and click **Next**.

4. Select **User Management** and **Authentication** as the permission types and click **Next**.

5. Take a note of the API token that you have created.

   !!! warning
   The token is only shown once.

