<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="/">
    <html>
      <body bgcolor="#FFFFEF">
        <hr align="left" size="1" width="95%"/>
        <p>
          <b>Coding Style Check Results</b>
        </p>
        <p>The following are violations of the Sun Coding-Style Standards:</p>
        <p/>
        <table border="1" cellpadding="2" cellspacing="0">
          <tr bgcolor="#CC9966">
            <th colspan="2">
              <b>Summary</b>
            </th>
          </tr>
          <tr bgcolor="#CCF3D0">
            <td>Total files checked</td>
            <td>
              <xsl:number level="any" value="count(descendant::file)"/>
            </td>
          </tr>
          <tr bgcolor="#F3F3E1">
            <td>Files with errors</td>
            <td>
              <xsl:number level="any" value="count(descendant::file[error])"/>
            </td>
          </tr>
          <tr bgcolor="#CCF3D0">
            <td>Total errors</td>
            <td>
              <xsl:number level="any" value="count(descendant::error)"/>
            </td>
          </tr>
          <tr bgcolor="#F3F3E1">
            <td>Errors per file</td>
            <td>
              <xsl:number level="any" value="count(descendant::error) div count(descendant::file)"/>
            </td>
          </tr>
        </table>
        <xsl:apply-templates/>
      </body>
      <head>
        <title>Sun Coding Style Violations</title>
      </head>
    </html>
  </xsl:template>

  <xsl:template match="file[error]">
    <p/>
    <table bgcolor="#EEEEEE" border="1" cellpadding="2" cellspacing="0" width="95%">
      <tr>
        <th>Line Number</th>
        <th>Error Message</th>
      </tr>
      <xsl:apply-templates select="error"/>
    </table>
    <table bgcolor="#55BBDD" border="1" cellpadding="2" cellspacing="0" width="95%">
      <tr>
        <td>
          <xsl:value-of select="@name"/>
        </td>
        <th>File:</th>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="error">
    <tr>
      <td>
        <xsl:value-of select="@line"/>
      </td>
      <td>
        <xsl:value-of select="@message"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
