//
//   Copyright 2018  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.quantum;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

  private Properties props = null;
  private int port = 0;
  private String host = null;
  private final static int DEFAULT_PORT = 8090;
  private final static String DEFAULT_HOST = "127.0.0.1";

  public static void main(String... args) {

    Main main = new Main();

    try {

      Properties props = new Properties();

      if (args.length > 0) {
        File propertiesFile = new File(args[0]);

        FileInputStream fis = new FileInputStream(propertiesFile);
        props.load(fis);
      } else {
        main.port = DEFAULT_PORT;
        main.host = DEFAULT_HOST;
      }

      main.init(props);

    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  public void init(Properties properties) throws Exception {

    if (null != properties) {
      this.props = properties;
    } else {
      this.props = new Properties();
    }

    if (null == this.props.getProperty("quantum.port")){
      this.port = DEFAULT_PORT;
    } else {
      this.port = Integer.valueOf(this.props.getProperty("quantum.port"));
    }

    if (null != (this.host = this.props.getProperty("quantum.host"))){
      System.out.println("Property quantum.host found, value: ${main.host}");
      Pattern VAR = Pattern.compile(".*\\$\\{([^}]+)\\}.*");
      Matcher m = VAR.matcher(this.host);
      if (m.matches()) {
        String var = m.group(1);

        System.out.println("Substitution pattern found for quantum.host, value: ${var}");

        if (this.props.containsKey(var)) {
          System.out.println("Substitution pattern found in properties file, value: ${ main.props.getProperty(var)}");
          this.host = this.props.getProperty(var);
        } else {
          System.err.println("Property '" + var + "' referenced in property quantum.host is unset, unsetting");
          this.host = DEFAULT_HOST;
        }
        System.out.println("main.host value: ${main.host}");
      }

    } else {
      this.host = DEFAULT_HOST;
    }

    run();
    
  }

  public void run() throws Exception {

    // The Jetty Server.
    Server server = new Server();

    // Common HTTP configuration.
    HttpConfiguration config = new HttpConfiguration();

    // HTTP/1.1 support.
    HttpConnectionFactory http1 = new HttpConnectionFactory(config);

    // HTTP/2 cleartext support.
    HTTP2CServerConnectionFactory http2c = new HTTP2CServerConnectionFactory(config);

    ServerConnector connector = new ServerConnector(server, http1, http2c);

    System.out.println("host: " + this.host);
    System.out.println("port: "+ this.port);

    connector.setHost(this.host);
    connector.setPort(this.port);
    server.addConnector(connector);

    URL webRootLocation = this.getClass().getResource("/public_html/.touch");
    if (webRootLocation == null) {
      throw new IllegalStateException("Unable to determine webroot URL location");
    }

    URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/.touch$","/"));
    System.err.printf("Web Root URI: %s%n",webRootUri);

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.setBaseResource(Resource.newResource(webRootUri));
    context.setWelcomeFiles(new String[]{"index.html"});
    context.getMimeTypes().addMimeMapping("txt","text/plain;charset=utf-8");

    ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
    holderPwd.setInitParameter("dirAllowed","true");
    context.addServlet(holderPwd,"/");

    server.setHandler(context);

    JettyUtil.setSendServerVersion(server, false);

    System.out.println("#### quantum.endpoint " + InetAddress.getByName(this.host) + ":" + this.port);

    server.start();
    server.dumpStdErr();
    server.join();
  }
}
