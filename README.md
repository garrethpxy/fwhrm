# fwhrm
Foreign Worker HRM

## Building and Execution
* Execute "mvn clean package". Jar will be created in the target/ with it dependencies copied into lib/.
* Run 'java -jar "DocumentRecognition-1.0-SNAPSHOT.jar" "tessdata_path" "scans_directory_path"'

## Prerequisites
#### Win
* All dlls are included in the corresponding jars and deployed automatically.

#### Linux (*to be tested*)
1. Execute "sudo apt-get install tesseract-ocr".
3. Execute "sudo apt-get install ghostscript".

#### Mac OS
1. Install "Homebrew" software.
2. Via terminal execute "brew install tesseract".
3. Via terminal execute "brew install ghostscript".
