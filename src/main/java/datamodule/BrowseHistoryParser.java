package datamodule;

import configuration.ConfigurationManager;
import dataconfiguration.BrowseHistoryConfiguration;
import dataconfiguration.CalendarConfiguration;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by henb on 7/3/2017.
 */
public class BrowseHistoryParser extends XryParser {
    public BrowseHistoryParser(String filePath, Logger logger) {
        super(filePath, logger);
        _jsonDocument = readJsonObject(ConfigurationManager.getInstance().browse_history_json_path);
        _jsonDocument = fillSolanJason(_jsonDocument, false);
        try {
            _jsonDocument.put("solan_type", "browseHistory");
        } catch (Exception e) {
            _logger.error(e);
        }
    }

    @Override
    public ArrayList Parse() {

        ArrayList result = new ArrayList();
        try {


            String fileTextContent = readFileText(new File(_filePath));

            if (fileTextContent != null) {
                ArrayList<String> calendarList = new ArrayList<>(Arrays.asList(fileTextContent.split("#")));

                for (String item : calendarList) {
                    if (item.contains("Related Application")) {
                        HashMap historyJsonDoc = extractHistory(item);
                        saveDocToDB(new JSONObject(historyJsonDoc).toString());
                    }
                }
            }
        } catch (Exception e) {
            _logger.error(e);
        }
        return result;
    }

    private HashMap extractHistory(String contact) {
        HashMap jsonContact = new HashMap(_jsonDocument);
        String eventText = textArragment(contact);
        ArrayList<String> eventLines = new ArrayList<>(Arrays.asList(eventText.split("%")));
        BrowseHistoryConfiguration browseHistoryConfiguration = new BrowseHistoryConfiguration();

        for (String item1 : eventLines) {
            ArrayList<String> line = new ArrayList<>(Arrays.asList(item1.split("::")));
            try {
                if (line.size() > 1) {
                    String field = line.get(0);
                    String value = line.get(1);

                    ArrayList<String> jsonFields = (ArrayList) browseHistoryConfiguration.fieldsMap.get(field);

                    if (jsonFields != null) {
                        for (String item : jsonFields) {
                            if (field.contains("Time") || field.contains("Created") || field.contains("Accessed")) {
                                String[] check = value.split(" ");
                                String format = null;

                                if(check.length == 4){
                                    format = "MM/dd/yyyy hh:mm:ss a z";
                                }else {
                                    format = "MM/dd/yyyy hh:mm:ss a";
                                }

                                DateTime date = DateTime.parse(value, DateTimeFormat.forPattern(format));
                                jsonContact.put(item.toString(), date.toString());
                            } else {
                                jsonContact.put(item, value);
                            }

                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        jsonContact.put("solan_inserted_timestamp", DateTime.now().toString());

        System.out.println(jsonContact);
        return jsonContact;
    }

    private String textArragment(String text) {
        String result = null;

        try {
            result = text.replace("\n", "%");
            result = result.replace("\t\t\t", ":");
            result = result.replace("\t\t", ":");
            result = result.replace("\t", ":");
            result = result.replace("\r", "");
            result = result.replace(" (Device)", "");
        } catch (Exception ex) {
            _logger.error(ex);
        }

        return result;
    }
}
