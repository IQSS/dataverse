Reset the EJB timer database back to default:
```
<payara install path>/asadmin set configs.config.server-config.ejb-container.ejb-timer-service.timer-datasource=jdbc/__TimerPool
```