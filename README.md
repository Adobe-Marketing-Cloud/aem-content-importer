# AEM Content Importer - Build

Maven Builds:

* Complete (content+bundle+saxon)
  Using profile `distribution` from either root or `aem-content-importer-content`.
  Assumes AEM 6.0 running on `localhost:4502`.

  For example:

  ```
  $ cd ~/my-code/aem-content-importer
  $ mvn -P distribution clean install
  ```

* Standard (only the selected project)
  With no profile specified maven installs only the selected project.
  Pay attention to aem-content-importer-bundle, the `/apps/${project.folder}/install` crx folder must exist
  
  *IMPORTANT: First install has to be done using profile `distribution`* 

Maven tests:

* Unit-tests are executed by default.
* For launching integration-tests, it's necessary to use profile `integration-tests`.


# AEM Content Importer -  Upload Page
Go to the upload page: `http://<host>:<port>/content/resources/importer-tool.html`

You have two options:

* Fill out form parameters:
    * *Transformer*: Select from list (currently only one, `XMLTransformerDITA`). The value saved it'll be the whole package(`com.adobe.aem.importer.impl.DocImporterImpl`).
	* *Source folder*: Specify the source path in the repository. The folder must be already created and populated with
	  all the source files (including the `ditamap` file), for example, through dragging and dropping the files into
	  the repository using a WebDAV client. A typical location for the source directory is `/var/aem-importer/import`.
	* *Destination folder*: Specify the destination folder in the repository. Whether destination directory doesn't exist it will be created. i.e `content/dita-import`. 
	* *Master File*: The name of the master file. For example, `mcloud.ditamap`.

* Upload a zip file containing all source files (contents+master+config) directly in the root or inside a folder. Note that the configuration file, which contains at least the same properties available in the form, must be the first file in the `zip` or `folder inside zip` package (alphabetical order).
  The upload can be performed either by dragging and dropping the `zip` file or by clicking the *Upload* button.
  Notice that config file values override the corresponding form parameters if they're filled them out.

  An example `config_param.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
 <entry key="xml-transformer">com.adobe.aem.importer.impl.DocImporterImpl</entry>
 <entry key="src">/var/aem-importer/import2</entry>
 <entry key="target">/content/pando</entry>
 <entry key="masterFile">mcloud.ditamap</entry>
 .....
 .....
</properties>
```

The result of upload page tool is to save a `.dita` file containing all configuration properties into path `/var/aem-importer/import`. This action fires the workflow model `aem-content-importer` in charge to process the importing


# Available Transformers

### XMLTransformerDITA

The XMLTransformerDITA transformer is currently the only one available and permits to import contents using an XSLT schema.
Here shown below the available parameters to set either in the form (custom properties input) or in zip (dita file):
* xslt-transformer: then class name of xslt transformer to use (i.e. net.sf.saxon.TransformerFactoryImpl). It's assume to be exported by an osgi bundle (it's available after first installation with `distribution` profile)
* xslt-file: the xslt file to use
* packageTpl: the path of package template to use by Jcr Archive Importer
* tempFolder: `optional, default '/var/aem-importer/tmp'` the temporary folder to use during processing
* graphicFolders: `optional, default 'images,graphics,Graphics'` the folder list separated by comma where to look up graphic resource to copy

An example of custom properties input in the form:
````
    xslt-transformer=net.sf.saxon.TransformerFactoryImpl
    xslt-file=/apps/aem-importer/resources/dita-to-content.xsl
    tempFolder=/var/aem-importer/tmp
    packageTpl=/apps/aem-importer/resources/package-tpl
    graphicFolders=images,graphics,Graphics
````
    
An example of `config_params.xml` containing XMLTransformerDITA properties also:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
 <entry key="xml-transformer">com.adobe.aem.importer.impl.DocImporterImpl</entry>
 <entry key="src">/var/aem-importer/import2</entry>
 <entry key="target">/content/pando</entry>
 <entry key="masterFile">mcloud.ditamap</entry>
 <entry key="xslt-transformer">net.sf.saxon.TransformerFactoryImpl</entry>
 <entry key="xslt-file">/apps/aem-importer/resources/dita-to-content.xsl</entry>
 <entry key="tempFolder">/var/aem-importer/tmp</entry>
 <entry key="packageTpl">/apps/aem-importer/resources/package-tpl</entry>
 <entry key="graphicFolders">images,graphics,Graphics</entry>
</properties>
```

### NoActionTransformer
Dummy transformer for testing service registration from external bundle


# Walkthrough test cases

### 1 - Zip File
*Manual steps*
* Go to page `http://<host>:<port>/content/resources/importer-tool.html`
* Zip folder `aem-content-importer-bundle/src/test/resources/exampleDITA` 
* Drop in or load the zip file just created.
* Click Execute

*Automatic steps*
* find and read properties from the first file in the zip (in this case `0_config_params.xml`) 
* all the files within zip are extracted into `/var/aem-importer/zip` (src param defined by `0_config_params.xml` file)
* a file `<id>.dita` is stored under `/var/aem-importer/import`
* workflow `aem-content-importer` is triggered and reads that configuration parameters
* check out if all parameters are set and invoke the selected transformer
* read and analyze master file `/var/aem-importer/zip/mcloud.ditamap`
* use the folder `/var/aem-importer/tmp` (creating an unique subfolder) as temporary repository (it will be deleted at the end)
* save final contents into `/content/dita-import`

### 2 - Input Form
*Manual steps*
* Go to page `http://<host>:<port>/content/resources/importer-tool.html`
* Fill in the **src** input the same folder used for previous test:
`/var/aem-importer/zip`
* Select from the **transformer** dropdown list:
`XMLTransformerDITA`
* Fill in the **master file** input:
`mcloud.ditamap`
* Fill in the **target** input a new destination path:
`/content/dita-import-2`
* Fill in the **custom properties** textarea all properties related to XSLT Transformer:
````
    xslt-transformer=net.sf.saxon.TransformerFactoryImpl
    xslt-file=/apps/aem-importer/resources/dita-to-content.xsl
    tempFolder=/var/aem-importer/tmp
    packageTpl=/apps/aem-importer/resources/package-tpl
    graphicFolders=images,graphics,Graphics
````
* Click Execute

*Automatic steps*
* save all parameters submitted in a file `<id>.dita` stored under `/var/aem-importer/import`
* workflow `aem-content-importer` is triggered and reads that configuration parameters
* check out if all parameters are set and invoke the selected transformer
* read and analyze master file `/var/aem-importer/zip/mcloud.ditamap`
* use the folder `/var/aem-importer/tmp` (creating an unique subfolder) as temporary repository (it will be deleted at the end)
* save final contents into `/content/dita-import-2`

### 3 - Workflow overriding
*Manual steps*
* Go to page `http://<host>:<port>/etc/workflow/models/aem-content-importer.html`
* Open edit dialog of `Content Transformer Process` step and select the tab `arguments`
* Insert into the input `target` the path `/content/dita-import-3`
* Click Save button on the top left corner
* Go to page `http://<host>:<port>/content/resources/importer-tool.html`
* Zip folder `aem-content-importer-bundle/src/test/resources/exampleDITA` 
* Drop in or load the zip file just created.
* Click Execute

*Automatic steps*
* find and read properties from the first file in the zip (in this case `0_config_params.xml`) 
* all the files within zip are extracted into `/var/aem-importer/zip` (src param defined by `0_config_params.xml` file)
* a file `<id>.dita` is stored under `/var/aem-importer/import`
* workflow `aem-content-importer` is triggered, reads that configuration parameters but gives the priority to those set inside
* check out if all parameters are set and invoke the selected transformer
* read and analyze master file `/var/aem-importer/zip/mcloud.ditamap`
* use the folder `/var/aem-importer/tmp` (creating an unique subfolder) as temporary repository (it will be deleted at the end)
* save final contents into `/content/dita-import-3`

# AEM Content Importer -  Command Line
* Maven install project aem-content-importer-cmd, that it's going to generate a lib folder with all necessary libraries and the runnable jar
file to execute inside project folder `aem-content-importer\tools-files`.
* There are two scripts in that project folder, one for linux (script.sh) and the other one for windows (script.bat) showing two examples: passing parameters, passing zip file that it's in that folder as well or a local folder with all source files for making the transformation.
* If it's necessary to pass custom properties, it's a mandatory to separate properties using the special character '#'. You can see it in script examples.
* If you have some doubts related to how to pass parameters execute the runnable jar without params. It'll be showed and explanation of each possible parameter.
An example:

````
java -jar aem-content-importer-cmd-0.0.1-SNAPSHOT.jar -src /var/aem-importer/import1 -transformer com.adobe.aem.importer.impl.DocImporterImpl -masterFile mcloud.ditamap -target /content/pando -customProps xslt-transformer=net.sf.saxon.TransformerFactoryImpl#xslt-file=/apps/aem-importer/resources/dita-to-content.xsl#tempFolder=/var/aem-importer/tmp#packageTpl=/apps/aem-importer/resources/package-tpl#graphicFolders=images,graphics,Graphics
````
