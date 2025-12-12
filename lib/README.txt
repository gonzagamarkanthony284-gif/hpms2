Place the MySQL Connector/J JDBC driver (Connector/J) into this folder and add it to the project's classpath.

Steps:
1. Download Connector/J from: https://dev.mysql.com/downloads/connector/j/
2. Copy the downloaded jar (e.g., mysql-connector-j-8.1.0.jar) into HPMS\lib\
3. In Eclipse: Right click project -> Properties -> Java Build Path -> Libraries -> Add JARs... -> select the jar in the lib folder.
4. If you run via command line, include the jar on the classpath when running Java, e.g.:

   javac -d bin @sources.txt
   java -cp "bin;lib\mysql-connector-j-8.1.0.jar" Util.DbTest

Notes:
- Keep the jar out of version control, or add lib/* to .gitignore if you prefer not to commit binary jars.
- Ensure module-info.java still requires java.sql; the driver integrates via the SPI and driver class name in config.
