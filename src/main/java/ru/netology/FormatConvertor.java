package ru.netology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FormatConvertor {
    private String baseFileName;
    private String exportDirectory;

    private FileFormat baseExtension;

    public FormatConvertor(String exportDirectory) {

        if (exportDirectory.isEmpty()) {
            exportDirectory = "";
        }

        this.exportDirectory = exportDirectory;
    }

    public List<Employee> convertJsonToList(String baseFileName) {
        this.baseFileName = baseFileName;
        this.baseExtension = getExtension(baseFileName);

        if (!baseExtension.equals(FileFormat.JSON))
            throw new RuntimeException("Некорректный формат исходного файла. Необходимо указать файл с расширением *.JSON");

        String jsonString = readJsonToString();

        return  jsonToList(jsonString);
    }

    private List<Employee> jsonToList(String jsonString) {

        List<Employee> list = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();

        try {
            Object jsonObj = jsonParser.parse(jsonString);

            for (Object item : (JSONArray) jsonObj) {
                list.add(
                        new GsonBuilder().create().fromJson(
                                ((JSONObject) item).toJSONString(),
                                Employee.class)
                );
            }
            return list;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private String readJsonToString() {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(baseFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public void convertCsvTo(String baseFile, char splitter, FileFormat format) {
        this.baseExtension = getExtension(baseFile);
        this.baseFileName = baseFile;

        if (!baseExtension.equals(FileFormat.CSV))
            throw new RuntimeException("Некорректный формат исходного файла. Необходимо указать файл с расширением *.CSV");

        List<Employee> list = getListObjFromCsv(splitter);
        if (!list.isEmpty()) {
            writeFile(convertToJson(list), format, FileCreateOption.CURRENT);
        }
    }

    public void convertXmlTo(String baseFile, FileFormat format, FileCreateOption newName) {
        this.baseExtension = getExtension(baseFile);
        this.baseFileName = baseFile;

        if (!baseExtension.equals(FileFormat.XML))
            throw new RuntimeException("Некорректный формат исходного файла. Необходимо указать файл с расширением *.XML");

        List<Employee> employees = getListObjFromXml();

        if (!employees.isEmpty()) {
            writeFile(convertToJson(employees), format, FileCreateOption.NEW_NAME);
        }
    }

    private List<Employee> getListObjFromXml() {

        try {
            List<Employee> list = new ArrayList<>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(baseFileName));

            Node rootNode = doc.getDocumentElement();

            NodeList nodeList = rootNode.getChildNodes();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (Node.ELEMENT_NODE == node.getNodeType() && node.getNodeName().equals("employee")) {

                    NodeList employeeNodes = node.getChildNodes();
                    String nodeValue = "";
                    Employee employee = new Employee();

                    for (int n = 0; n < employeeNodes.getLength(); n++) {
                        Node childNode = employeeNodes.item(n);

                        if (Node.ELEMENT_NODE == childNode.getNodeType()) {


                            nodeValue = childNode.getTextContent();

                            switch (childNode.getNodeName()) {
                                case "id":
                                    employee.id = Integer.parseInt(nodeValue);
                                    break;
                                case "firstName":
                                    employee.firstName = nodeValue;
                                    break;
                                case "lastName":
                                    employee.lastName = nodeValue;
                                    break;
                                case "country":
                                    employee.country = nodeValue;
                                    break;
                                case "age":
                                    employee.age = Integer.parseInt(nodeValue);
                                    break;
                            }

                        }
                    }
                    list.add(employee);
                }
            }

            return list;

        } catch (IOException | ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();
    }


    private String convertToJson(List<Employee> list) {
        GsonBuilder gBuilder = new GsonBuilder();
        Gson gson = gBuilder.create();

        return gson.toJson(list);
    }

    private void writeFile(String fileContent, FileFormat exportFileFormat, FileCreateOption newName) {

        String fileName = changeExtansion(baseFileName, exportFileFormat, newName);

        File file = new File(fileName);

        if (file.exists()) {
            file.delete();
        }


        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(fileContent);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String changeExtansion(String fileName, FileFormat format, FileCreateOption newName) {
        String name = fileName.substring(0, fileName.indexOf("."));

        if (newName.equals(FileCreateOption.NEW_NAME)) {
            name = String.format("%s_%s", name, Integer.toString(new Random().nextInt(100)));
        }

        switch (format) {
            case JSON:
                name = name + ".json";
                break;
            case XML:
                name = name + ".xml";
                break;
            case CSV:
                name = name + ".csv";
            default:
                name = baseFileName;
                break;
        }
        return name;
    }

    private List<Employee> getListObjFromCsv(char splitter) {

        CSVParser csvParser = new CSVParserBuilder()
                .withSeparator(splitter)
                .build();

        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(baseFileName)).withCSVParser(csvParser).build()) {

            ColumnPositionMappingStrategy<Employee> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(Employee.class);
            strategy.setColumnMapping("id", "firstName", "lastName", "country", "age");

            CsvToBean<Employee> csvBean = new CsvToBeanBuilder<Employee>(csvReader)
                    .withMappingStrategy(strategy)
                    .build();

            return csvBean.parse();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    private FileFormat getExtension(String path) {
        File file = new File(path);

        if (!file.isDirectory()) {
            switch (file.getName().substring(file.getName().indexOf(".") + 1).toUpperCase()) {
                case "CSV":
                    return FileFormat.CSV;
                case "XML":
                    return FileFormat.XML;
                case "JSON":
                    return FileFormat.JSON;
                default:
                    return FileFormat.NONE;
            }
        }
        return FileFormat.NONE;
    }
}
