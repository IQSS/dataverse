New OpenID Connect implementation including new log in scenarios :ref:`oidc-log-in` for the current JSF frontend, the new Single Page Application (SPA) frontend, and a generic API usage. The API scenario using Bearer Token authorization is illustrated with a Pythons cript that can be found in `bearer-token-example` directory. This Python script prompts you to log in to the Keycloak in a new browser window using selenium. You can run that script with the following commands:

```shell
    cd bearer-token-example
    ./run.sh
```

This script is safe for production use, as it does not require you to know the client secret or the user credentials. Therefore, you can safely distribute it as a part of your own Python script that lets users run some custom tasks.
