# Headers Servlet Filter

Provides control over HTTP response headers in a Servlet container like Tomcat. Open Source Java project under Apache License v2.0

### Current Stable Version is [1.0.0](https://search.maven.org/#search|ga|1|g%3Aorg.javastack%20a%3Aheaders-servlet-filter)

---

## DOC

#### Usage Example

```xml
<!-- Servlet Filter -->
<!-- tomcat/conf/web.xml or WEB-INF/web.xml -->
<filter>
    <filter-name>ResponseHeadersSetFilter</filter-name>
    <filter-class>org.javastack.servlet.filters.ResponseHeadersSetFilter</filter-class>
    <!-- The response header is set, replacing any previous header with this name -->
    <!-- Cache Control / Expiration -->
    <init-param>
        <param-name>Cache-Control</param-name>
        <param-value>no-cache, no-store, must-revalidate, max-age=0</param-value>
    </init-param>
    <init-param>
        <param-name>Expires</param-name>
        <param-value>0</param-value>
    </init-param>
    <!-- SSL/TLS Security -->
    <init-param>
        <param-name>Strict-Transport-Security</param-name>
        <param-value>max-age=15638400</param-value>
    </init-param>
    <init-param>
        <param-name>Public-Key-Pins</param-name>
        <param-value>pin-sha256="base64+primary=="; pin-sha256="b64+backup=="; max-age=604800</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>ResponseHeadersSetFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

---

## MAVEN

Add the dependency to your pom.xml:

    <dependency>
        <groupId>org.javastack</groupId>
        <artifactId>headers-servlet-filter</artifactId>
        <version>1.0.0</version>
    </dependency>

---
Inspired in [mod_headers](http://httpd.apache.org/docs/2.4/mod/mod_headers.html), this code is Java-minimalistic version.
