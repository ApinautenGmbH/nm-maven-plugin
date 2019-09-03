# nm-maven-plugin

The nm (Native Module) Maven Plugin helps you in the handling with native modules.


## Goals
The following goals are supported:  

| name | description |
| -------- | ------------------------------- |
| upload | Upload the native module |
| download | Download and extract the native module |
| updateAfterDatamodelChange | executes a download and upload |
| package | Package the native module artifact for upload |
| release | Release the native module on a host |
| unrelease | Unrelease the native module on a host |
| updateVersion | Update the native module code to a specified version |
| addDependency | Add a module dependency and download  |

### Configuration parameters

The following configuration parameters are supported:


| name | description | supported by goal | supported values | example value |
| -------- | ------------------------ | ------ | ------ | ------ |
| moduleName | Name of native module | all (required, see the additional notes below) |  | TestModule |
| host | URL to ApiOmat instance | all (required) |  | http://localhost:8080 |
| system | ApiOmat stage that should be used | all (required) | LIVE, STAGING, TEST | TEST |
| customerEmail | mail address of customer | upload, download, release, unrelease |  | customer@example.com |
| customerName | login name of customer | upload, download, release, unrelease |  | exampleCustomer |
| customerPassword | Password for customer | upload, download, release, unrelease |  | secret |
| merge |indicate whether the generated parts and the previously uploaded jar should be merged, or only the generated parts should be returned | download | true, false | true |
| eclipse | indicates whether the download should contain an eclipse project | download | true, false | true |
| overwriteHooks | whether to overwrite the local hook files with the remote contents | download | true, false | false |
| update | update native module | upload | true, false, overwrite | overwrite |
| noDownload | whether to download native module after upload | upload, addDependency | true, false | false |
| fromVersion | YambasVersion to update from | updateVersion | Version in Form x.y.z (or x.y) | 2.0.0 |
| toVersion | YambasVersion to update to | updateVersion | Version in Form x.y.z (or x.y) | 3.3.0 |
| usedModuleName | Name of the module to add as dependency | addDependency | a module name | MyModule |
| usedModuleVersion | Version of the module to add as dependency | addDependency | Version in Form x.y.z (or x.y) | 1.0.0 |
| nmSkip | skip execution of this goal | all | true, false | false |
| finalName | name of the generated jar | upload |  | mymodule-1.0.0 |

Every parameter can be either set via **command line argument or in the personal settings.xml** file:

    <settings>
    ...  
      <profiles>
        <profile>
          <id>testing</id>
          <properties>
            <host>https://my.testing-server.com</host>
            <customerName>john.doe</customerName>
          </properties>
          ....
        </profile>
        ....
      </profiles>
      ...
     </settings>

You can then activate this profile, which will be used within the build by appending `-P testing`

#### Additional notes:

* **WARNING**: Note that it is, due to Mavens nature, not possible to reload the pom.xml within a maven plugin (or within the entire maven environment). Therefore, you have to be careful by combining goals that may modify the pom.xml with other goals or phases that are following afterwards. The goals to be careful with are: `nm:download`, `nm:updateVersion` and `nm:upload` (as `nm:upload` does a download afterwards). If you want to execute one of these goals followed by other ones, you have to execute the following goals as another command execution, like `mvn nm:download && mvn clean package` . Thus, each command gets executed separately as own process and therefore the pom.xml will be re-read after the `nm:download`
* **moduleName** may be given, otherwise the plugin checks whether a name-tag is provided in the pom.xml, if there's none, it will be checked whether there is an old sdk.properties file containing the moduleName. If this also fails, it will try to find the module main class and get the module name from the filename. At least, if this was unsuccessful,  the artifactId gets used.
* either **customerEmail** or **customerName** has to be given for the supported goals, otherwise it will fail the build
* note that the execution of a plugin goal does not include the full execution of maven phases. thus, if you execute the goal "nm:package", it will not automatically execute the compilation of your module (as it will only execute the single goal). but if you execute the maven phases "clean package" (and you've added the snippet of the general section below to your pom.xml), it will execute the "nm:package" goal within the "package" phase and also generate the <ModuleName>-<ModuleVersion>-NM.jar artifact for the upload.


## Examples

Following snippet will show you how you will use this plugin.

#### General
After adding the plugin to the pom.xml of the module, you simply call it over `mvn nm:<goalName>` instead of calling `mvn com.apiomat.helper:nm-maven-plugin:<goalName>`

1. Add the following snippet to your pom.xml:

    ```
    <project>
        ...
        <build>
            ...
            <plugins>
                ...
                <plugin>
                    <groupId>com.apiomat.helper</groupId>
                    <artifactId>nm-maven-plugin</artifactId>
                    <version>1.0.0</version>
                </plugin>
            </plugins>
        </build>
    </project>
    ```
2. Execute the following command to upload your module

    ```
    mvn clean package nm:upload -DcustomerName=<yourCustomerName> -DcustomerPassword=<yourCustomerPassword> -Dhost=<yourYambasHost>
    ```



## Changelog
| Version | Changes |
| --------|------------------------:|
| 0.0.1-SNAPSHOT | Initial version |
| 1.0.0 | Improvements and migration to github |
| 1.1.0 | Improvements and adding possbility to skip execution |