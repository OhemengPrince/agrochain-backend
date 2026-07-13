@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\user\Desktop\HTML\agrochain-backend\agrochain-backend"
echo [%TIME%] Starting AgroChain backend with Java 25...
"C:\Users\user\Desktop\HTML\agrochain-backend\agrochain-backend\mvnw.cmd" spring-boot:run
