
# WMS commons exception

The commons-exception is a global exception handler for REST endpoints where:- 
- Returns 400 status code for Invalid format and application exceptions. 
- Returns 500 status code for server errors.
- Returns 404 status code for resource not found exceptions. 

For more details, kindly refer the design doc.

[Low Level Design Documentation](https://autozone1com.sharepoint.com/:w:/r/sites/SupplyChainWMSRewrite-AZRIMS/Shared%20Documents/AZ%20RIMS%20-%20WMS%20ReBuild%20SR%20and%20AZ/Scope%20Retail%20Documents/Design/LLD/Common/LLD_Common_v0.2.docx?d=w88756a53e5c84212997e94674ee678c1&csf=1&web=1&e=E7Xv8t)
## System Requirements

### Local Development Environment

- Maven version 3.5+.
- Java Version 17 JDK.
- Intellij Community Edition, Spring Tools IDE, Eclipse IDE or VSCode

## Usage/Examples


### Importing the commons-exception

- Ensure you have the below repositories setup for the WMS service you are about to implement:-

```bash
wms-template-service
    |_wms-template-command/query/message-handler
            |_wms-template-service-layer
                |_wms-template-io
                |_wms-template-dao
```

- To use the commons-exception, include the internal and external bom in either ```wms-template-command-handler``` or ```wms-template-query-handler``` like so

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>az.supplychain.wms</groupId>
            <artifactId>wms-bom-external</artifactId>
            <version>${wms-bom-external.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>az.supplychain.wms</groupId>
            <artifactId>wms-bom-internal</artifactId>
            <version>${wms-bom-internal.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

```
- Finally include the commons-exception under the dependencies section like so

```xml    
<dependencies>
    <dependency>
        <groupId>az.supplychain.wms</groupId>
        <artifactId>wms-commons-exception</artifactId>
    </dependency>
</dependencies>
```
**Note:** Recommended for use in command and query handler jars for now!
### Using the commons-exception

- Import the exception config in your config class like so

```java
    import az.supplychain.wms.ExceptionConfig;

    @Configuration
    @ComponentScan
    @Import({..., ExceptionConfig.class})
    public class SomeHandlerConfig {

    }
```
- Now you can raise the exceptions from your controllers for the error scenarios and the exception handler would handle sending the response and status code for the same.
- Below table lists the class of exceptions you can raise:-

|Exception class | Response status code|
|:----|:----|
|```ApplicationException``` | 400|
|```Exception``` |500|
|```InvalidFomatException```|400|
|```NotFoundException```|404| 

### Building the project
- After all changes are done, build all the projects like so:

```
cd your/project/root/dir

mvn clean install
```


## Roadmap

**Note:** Global exception handling for message handlers is not handled.

[ ] Handle global exception handling for message handlers including retry able exception. [WMSREBLD-2129](https://track.autozone.com/browse/WMSREBLD-2129)

[ ] Utilize the Response entites from commons-io instead of Spring's ```ResponseEntity```. [WMSREBLD-2130](https://track.autozone.com/browse/WMSREBLD-2130)