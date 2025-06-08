package testcase;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvel2.MVEL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AllCombinationGen {

    static class AttributeSchema {
        String name;
        List<Object> values;
        String rule;

        AttributeSchema(String name, List<Object> values, String rule) {
            this.name = name;
            this.values = values;
            this.rule = rule;
        }
    }

    public static void main(String[] args) throws Exception {
        
    	File schemaFile = new File("C:\\Users\\Yuvraj\\Documents\\workspace\\automation\\testcase\\src\\main\\resources\\schema_neg.json");
       

       // File schemaFile = new File(args[0]);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(schemaFile);

        List<AttributeSchema> attributes = new ArrayList<>();
        for (JsonNode node : root.get("attributes")) {
            String name = node.get("name").asText();
            List<Object> values = new ArrayList<>();
            for (JsonNode val : node.get("values")) {
                if (val.isTextual()) values.add(val.asText());
                else if (val.isBoolean()) values.add(val.asBoolean());
                else if (val.isInt()) values.add(val.asInt());
            }
            String rule = node.has("rule") ? node.get("rule").asText() : null;
            attributes.add(new AttributeSchema(name, values, rule));
        }

        List<Map<String, Object>> allCombos = new ArrayList<>();
        generateCombinations(attributes, 0, new HashMap<>(), allCombos);

      //  System.out.println("\nValid Combinations:");
       // allCombos.stream().filter(row -> validate(row, attributes)).forEach(System.out::println);

        System.out.println("\nInvalid Combinations:");
        System.out.println("AssetClass,ReportingPartyID,NonReportingPartyId,BuyerID,SellerID,Leg1PayerID,Leg2PayerID,Leg1ReceiverID,Leg2ReceiverID");
        
        Path filePath = Paths.get("output.csv");
        allCombos.stream().filter(row -> !validate(row, attributes)).forEach( row -> {
        	System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    row.get("AssetClass"),
                    row.get("ReportingPartyID"),
                    row.get("NonReportingPartyId"),
                     row.get("BuyerID"),
                     row.get("SellerID"),
                    row.get("Leg1PayerID"),
                    row.get("Leg2PayerID"),
                    row.get("Leg1ReceiverID"),
                    row.get("Leg2ReceiverID"));
        	
        	try {
				Files.writeString(filePath, row.get("AssetClass") +","+
				        row.get("ReportingPartyID")+","+
				        row.get("NonReportingPartyId")+","+
				         row.get("BuyerID")+","+
				         row.get("SellerID")+","+
				        row.get("Leg1PayerID")+","+
				        row.get("Leg2PayerID")+","+
				        row.get("Leg1ReceiverID")+","+
				        row.get("Leg2ReceiverID"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
    }

    private static void generateCombinations(List<AttributeSchema> attrs, int index, Map<String, Object> current, List<Map<String, Object>> results) {
        if (index == attrs.size()) {
            results.add(new LinkedHashMap<>(current));
            return;
        }
        AttributeSchema attr = attrs.get(index);
        for (Object val : attr.values) {
            current.put(attr.name, val);
            generateCombinations(attrs, index + 1, current, results);
        }
    }

    private static boolean validate(Map<String, Object> row, List<AttributeSchema> attrs) {
        for (AttributeSchema attr : attrs) {
            if (attr.rule != null && !attr.rule.isEmpty()) {
                Map<String, Object> ctx = new HashMap<>(row);
                ctx.put("value", row.get(attr.name));
                try {
                    Object result = MVEL.eval(attr.rule, ctx);
                    if (!(result instanceof Boolean)) return false;
                    if (!((Boolean) result)) return false;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }
}
