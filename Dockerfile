# ---- build stage ----
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
# cache deps
RUN mvn -q -e -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- run stage ----
# VulnBank (Java) - deliberately vulnerable training image. Do NOT expose publicly.
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /build/target/vulnbank.jar /app/vulnbank.jar
# seed the reports dir used by the path-traversal module
COPY reports /app/reports
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/vulnbank.jar"]
