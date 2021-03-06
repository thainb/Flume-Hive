package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import com.cloudera.flume.conf.FlumeBuilder;

public final class masterext_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static java.util.Vector _jspx_dependants;

  public java.util.List getDependants() {
    return _jspx_dependants;
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    JspFactory _jspxFactory = null;
    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      _jspxFactory = JspFactory.getDefaultFactory();
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("<!--\n Licensed to Cloudera, Inc. under one\n or more contributor license agreements.  See the NOTICE file\n distributed with this work for additional information\n regarding copyright ownership.  Cloudera, Inc. licenses this file\n to you under the Apache License, Version 2.0 (the\n \"License\"); you may not use this file except in compliance\n with the License.  You may obtain a copy of the License at\n\n     http://www.apache.org/licenses/LICENSE-2.0\n\n Unless required by applicable law or agreed to in writing, software\n distributed under the License is distributed on an \"AS IS\" BASIS,\n WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n See the License for the specific language governing permissions and\n limitations under the License.\n-->\n<html><head>\n<!-- Copyright (c) 2010 Cloudera, Inc.  All rights reserved. -->\n<!-- Retro web 1.0 flume Agent configuration display -->\n<title>Flume Master: Sources and Sinks</title>\n\n<link rel=\"stylesheet\" type=\"text/css\" href=\"/flume.css\" />\n\n</head>\n<body>\n");
      org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "menu.jsp", out, false);
      out.write("\n\n<h1>Flume Master : Extensions - Sink/Source/Decorator</h1>\n");
      org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "version.jsp", out, false);
      out.write("\n<hr>\n\n<div id=\"sinks\">\n<h2>Sinks</h2>\n<table>\n\n");

  // treeset so that the list is sorted
  for (String n: new TreeSet<String>(FlumeBuilder.getSinkNames())) {

      out.write("\n    <tr><td>");
      out.print( n );
      out.write("</td></tr>\n");

  }

      out.write("\n\n</table>\n</div>\n\n<div id=\"sources\">\n<h2>Sources</h2>\n<table>\n\n");

  // treeset so that the list is sorted
  for (String n: new TreeSet<String>(FlumeBuilder.getSourceNames())) {

      out.write("\n    <tr><td>");
      out.print( n );
      out.write("</td></tr>\n");

  }

      out.write("\n\n</table>\n</div>\n\n<div id=\"decos\">\n<h2>Decorators</h2>\n<table>\n\n");

  // treeset so that the list is sorted
  for (String n: new TreeSet<String>(FlumeBuilder.getDecoratorNames())) {

      out.write("\n    <tr><td>");
      out.print( n );
      out.write("</td></tr>\n");

  }

      out.write("\n\n</table>\n</div>\n\n</body></html>\n");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          out.clearBuffer();
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      if (_jspxFactory != null) _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
