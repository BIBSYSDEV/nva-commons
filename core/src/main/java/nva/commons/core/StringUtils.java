package nva.commons.core;

import static java.util.Objects.isNull;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class StringUtils {

    private static final String BASIC_OUTER_XML_TAGS_TEMPLATE = "<naive>%s</naive>";
    private static final String PATH_TO_TEXT = "//text()";
    public static final String DOUBLE_WHITESPACE = "\\s\\s";
    public static final String WHITESPACES = "\\s+";
    public static final String SPACE = " ";
    public static final String EMPTY_STRING = "";

    private StringUtils() {
    }

    /**
     * Replaces multiple consecutive whitespaces with a single whitespace.
     *
     * @param input A string with or without multiple consecutive whitespaces.
     * @return A string without multiple consecutive whitespaces where are whitespaces have been replaced by a space.
     */
    public static String removeMultipleWhiteSpaces(String input) {
        String buffer = input.trim();
        String result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        while (!result.equals(buffer)) {
            buffer = result;
            result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        }
        return result;
    }

    /**
     * Remove all whitespaces.
     *
     * @param input A string with or without multiple consecutive whitespaces.
     * @return a string without spaces.
     */
    public static String removeWhiteSpaces(String input) {
        return input.replaceAll(WHITESPACES, EMPTY_STRING);
    }

    /**
     * Checks if string input is blank or null.
     *
     * @param input input string
     * @return <code>true</code> if blank.
     */
    public static boolean isBlank(String input) {
        return isNull(input) || input.isBlank();
    }

    /**
     * Checks if string input is empty or null.
     *
     * @param input input string
     * @return <code>true</code> if empty.
     */
    public static boolean isEmpty(String input) {
        return isNull(input) || input.isEmpty();
    }

    /**
     * Replaces  whitespaces with space.
     *
     * @param str input string.
     * @return string with all whitespaces replaced by spaces
     */
    public static String replaceWhiteSpacesWithSpace(String str) {
        return str.replaceAll("\\s", " ");
    }

    /**
     * Checks if string is neither null or empty.
     *
     * @param string string to check
     * @return <code>true</code> if string is neither null or empty.
     */
    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    /**
     * Checks if string is neither null or blank.
     *
     * @param string string to check
     * @return <code>true</code> if string is neither null or empty.
     */
    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    public static String removeXmlTags(String input) {
        String output = null;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        try {
            Document document = createXmlDocumentFromInput(input, documentBuilderFactory);
            NodeList nodeList = getDocumentNodes(document);
            output = textWithoutXmlTags(nodeList);
        } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
            System.out.println(e.getMessage());
        } finally {
            if (isNull(output)) {
                output = input;
            }
        }
        return removeMultipleWhiteSpaces(output).trim();
    }

    private static String textWithoutXmlTags(NodeList nodeList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int counter = 0; counter < nodeList.getLength(); counter++) {
            stringBuilder.append(EMPTY_STRING).append(nodeList.item(counter).getTextContent());
        }
        return stringBuilder.toString();
    }

    private static NodeList getDocumentNodes(Document document) throws XPathExpressionException {
        XPathFactory xpathFactory = XPathFactory.newDefaultInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile(PATH_TO_TEXT);
        return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
    }

    private static Document createXmlDocumentFromInput(String input, DocumentBuilderFactory documentBuilderFactory)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (Reader reader = new StringReader(wrapInXml(input))) {
            InputSource inputSource = new InputSource(reader);
            inputSource.setEncoding(StandardCharsets.UTF_8.name());
            Document document = documentBuilder.parse(inputSource);
            document.getDocumentElement().normalize();
            return document;
        }
    }

    private static String wrapInXml(String input) {
        return String.format(BASIC_OUTER_XML_TAGS_TEMPLATE, input);
    }
}
