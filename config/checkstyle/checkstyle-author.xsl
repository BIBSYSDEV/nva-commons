<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:decimal-format decimal-separator="." grouping-separator=","/>
  <xsl:key match="//file/error[contains(@message,'@author')]" name="keyAuthorID" use="@message"/>
  <!--
  Checkstyle XML Style Sheet by Daniel Grenner
  <daniel DOT grenner AT enea DOT se>
  This stylesheet groups the errors by author name, if used  in combination
  with the WriteTag check:

      <module name="WriteTag">
          <property name="tag" value="@author"/>
          <property name="tagFormat" value="\S"/>
          <property name="severity" value="ignore"/>
      </module>

  The output contains both error and warning messages.
  Files without errors or warnings are not included.
  This stylesheet is based on checkstyle-noframes.xsl.
  -->

  <xsl:key match="//file/error" name="keySeverityID" use="@severity"/>
  <xsl:output indent="yes" method="html"/>
  <xsl:template match="checkstyle">
    <html>
      <body>
        <a name="#top"/>
        <!-- jakarta logo -->
        <hr size="1"/>
        <hr align="left" size="1" width="100%"/>

        <!-- Summary part -->
        <hr align="left" size="1" width="100%"/>
        <hr align="left" size="1" width="100%"/>

        <!-- Author List part -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%">
          <tr>
            <td class="bannercell" rowspan="2">
              <!--a href="http://jakarta.apache.org/">
<img src="http://jakarta.apache.org/images/jakarta-logo.gif" alt="http://jakarta.apache.org" align="left" border="0"/>
</a-->
            </td>
            <td class="text-align:right">
              <h2>CheckStyle Audit</h2>
            </td>
          </tr>
          <tr>
            <td class="text-align:right">Designed for use with <a
              href="http://checkstyle.sourceforge.net/">CheckStyle
            </a> and <a href="http://jakarta.apache.org">Ant</a>.
            </td>
          </tr>
        </table>
        <xsl:apply-templates mode="summary" select="."/>

        <!-- For each package create its part -->
        <xsl:apply-templates mode="authorlist" select="."/>
        <xsl:for-each select="file">
          <p/>
          <p/>
          <xsl:apply-templates select="."/>
          <xsl:sort select="./error[contains(@message,'@author=')]/@message"/>
        </xsl:for-each>
      </body>
      <head>
        <style type="text/css">
          .bannercell {
          border: 0px;
          padding: 0px;
          }
          body {
          margin-left: 10;
          margin-right: 10;
          font:normal 68% verdana,arial,helvetica;
          background-color:#FFFFFF;
          color:#000000;
          }
          .a td {
          background: #efefef;
          }
          .b td {
          background: #fff;
          }
          th, td {
          text-align: left;
          vertical-align: top;
          }
          th {
          font-weight:bold;
          background: #ccc;
          color: black;
          }
          table, th, td {
          font-size:100%;
          border: none
          }
          table.log tr td, tr th {

          }
          h2 {
          font-weight:bold;
          font-size:140%;
          margin-bottom: 5;
          }
          h3 {
          font-size:100%;
          font-weight:bold;
          background: #525D76;
          color: white;
          text-decoration: none;
          padding: 5px;
          margin-right: 2px;
          margin-left: 2px;
          margin-bottom: 0;
          }
        </style>
      </head>
    </html>
  </xsl:template>
  <xsl:template match="checkstyle" mode="summary">
    <h3>Summary</h3>
    <table border="0" cellpadding="5" cellspacing="2" class="log" width="100%">
      <tr>
        <th>Files</th>
        <th>Errors</th>
        <th>Warnings</th>
      </tr>
      <tr>
        <td>
          <xsl:value-of select="$fileCount"/>
        </td>
        <td>
          <xsl:value-of select="$errorCount"/>
        </td>
        <td>
          <xsl:value-of select="$warningCount"/>
        </td>
        <xsl:call-template name="alternated-row"/>
      </tr>
    </table>
    <xsl:variable name="errorCount" select="count(key('keySeverityID', 'error'))"/>
    <xsl:variable name="warningCount" select="count(key('keySeverityID', 'warning'))"/>
    <xsl:variable name="fileCount" select="count(file)"/>
  </xsl:template>
  <xsl:template match="checkstyle" mode="authorlist">
    <h3>Authors</h3>

    <table border="0" cellpadding="5" cellspacing="2" class="log" width="100%">
      <tr>
        <th>Name</th>
        <th>Errors</th>
        <th>Warnings</th>
      </tr>

      <!-- Process each Author -->
      <xsl:for-each
        select="file/error[generate-id(.) = generate-id(key('keyAuthorID', @message)[1])]">
        <xsl:if test="not ($author='' or ($errors + $warnings = 0))">
          <tr>
            <td>
              <a href="#{$author}">
                <xsl:value-of select="$author"/>
              </a>
            </td>
            <td>
              <xsl:value-of select="$errors"/>
            </td>
            <td>
              <xsl:value-of select="$warnings"/>
            </td>
            <xsl:call-template name="alternated-row"/>
          </tr>
        </xsl:if>

        <xsl:sort select="@message"/>
        <xsl:variable name="author" select="substring-after($authorFull,'@author=')"/>
        <xsl:variable name="errors"
          select="count(key('keyAuthorID', @message)/../error[@severity='error'])"/>
        <xsl:variable name="warnings"
          select="count(key('keyAuthorID', @message)/../error[@severity='warning'])"/>
        <xsl:variable name="authorFull" select="@message"/>
      </xsl:for-each>

    </table>
  </xsl:template>

  <xsl:template match="file">
    <xsl:if test="not ($errorCount=0)">

      <a name="#{$author}"/>

      <a href="#top">Back to top</a>
      <h3>File
        <br/>
        <xsl:value-of select="@name"/>
        Author
        <xsl:value-of select="$author"/>
      </h3>
      <table border="0" cellpadding="5" cellspacing="2" class="log" width="100%">
        <tr>
          <th>Error Description</th>
          <th>Line</th>
        </tr>
        <xsl:for-each select="error[not(@severity='info')]">
          <tr>
            <td>
              <xsl:value-of select="@message"/>
            </td>
            <td>
              <xsl:value-of select="@line"/>
            </td>
            <xsl:call-template name="alternated-row"/>
          </tr>
          <xsl:sort data-type="number" select="@line"/>
        </xsl:for-each>
      </table>
      <xsl:variable name="author"
        select="substring-after(./error[contains(@message,'@author')]/@message,'@author=')"/>
    </xsl:if>
    <xsl:variable name="errorCount"
      select="count(error[@severity='error']) + count(error[@severity='warning'])"/>
  </xsl:template>


  <xsl:template name="basename">
    <xsl:choose>
      <xsl:otherwise>
        <xsl:value-of select="$path"/>
      </xsl:otherwise>
      <xsl:when test="contains($path, '\')">
        <xsl:call-template name="basename">
          <xsl:with-param name="path">substring-after($path, '\')</xsl:with-param>
        </xsl:call-template>
      </xsl:when>
    </xsl:choose>
    <xsl:param name="path"/>
  </xsl:template>


  <xsl:template name="alternated-row">
    <xsl:attribute name="class">
      <xsl:if test="position() mod 2 = 1">a</xsl:if>
      <xsl:if test="position() mod 2 = 0">b</xsl:if>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>
