package testcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mvel2.MVEL;

import java.io.File;
import java.util.*;

public class NegativeDataGen {

    static class AttributeSchema {
        String name;
        String type;
        String ruleType;
        String rule;
        boolean nullable;
        List<String> dependencies;

        public AttributeSchema(JsonNode node) {
            name = node.get("name").asText();
            type = node.get("type").asText();
            ruleType = node.get("rule_type").asText();
            rule = node.has("rule") ? node.get("rule").asText() : null;
            nullable = node.get("nullable").asBoolean();
            dependencies = new ArrayList<>();
            node.get("dependencies").forEach(d -> dependencies.add(d.asText()));
        }
    }

    public static void main(String[] args) throws Exception {
		/*
		 * if (args.length < 2) {
		 * System.out.println("Usage: java NegativeDataGen <schema-file> <record-count>"
		 * ); return; }
		 */
        File schemaFile = new File("C:\\Users\\Yuvraj\\Documents\\workspace\\automation\\testcase\\src\\main\\resources\\schema.json");
        int count = Integer.parseInt("10");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(schemaFile);

        List<AttributeSchema> schema = new ArrayList<>();
        for (JsonNode node : root.get("attributes")) {
            schema.add(new AttributeSchema(node));
        }

        List<Map<String, Object>> negativeRows = new ArrayList<>();
        Random random = new Random();

        while (negativeRows.size() < count) {
            Map<String, Object> row = new HashMap<>();
            String rpId = "RP" + (100 + random.nextInt(900));
            String nrpId = "NP" + (100 + random.nextInt(900));
            row.put("ReportingPartyID", null); // invalid on purpose
            row.put("NonReportingPartyId", nrpId);
            row.put("AssetClass", "XXX"); // invalid asset class

            boolean setBuyerSeller = random.nextBoolean();

            if (setBuyerSeller) {
                row.put("BuyerID", "INVALID_ID"); // not matching RP or NRP
                row.put("SellerID", "INVALID_ID");
                row.put("Leg1PayerID", null);
                row.put("Leg2PayerID", null);
                row.put("Leg1ReceiverID", null);
                row.put("Leg2ReceiverID", null);
            } else {
                row.put("BuyerID", null);
                row.put("SellerID", null);
                row.put("Leg1PayerID", "INVALID_ID");
                row.put("Leg2PayerID", "INVALID_ID");
                row.put("Leg1ReceiverID", "INVALID_ID");
                row.put("Leg2ReceiverID", "INVALID_ID");
            }

            if (!validateRowWithSchema(row, schema)) {
                negativeRows.add(row);
            }
        }

        System.out.println("AssetClass,ReportingPartyID,NonReportingPartyId,BuyerID,SellerID,Leg1PayerID,Leg2PayerID,Leg1ReceiverID,Leg2ReceiverID");
        for (Map<String, Object> row : negativeRows) {
            System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    row.get("AssetClass"),
                    row.get("ReportingPartyID"),
                    row.get("NonReportingPartyId"),
                    row.get("BuyerID") == null ? "" : row.get("BuyerID"),
                    row.get("SellerID") == null ? "" : row.get("SellerID"),
                    row.get("Leg1PayerID") == null ? "" : row.get("Leg1PayerID"),
                    row.get("Leg2PayerID") == null ? "" : row.get("Leg2PayerID"),
                    row.get("Leg1ReceiverID") == null ? "" : row.get("Leg1ReceiverID"),
                    row.get("Leg2ReceiverID") == null ? "" : row.get("Leg2ReceiverID"));
        }
    }

    private static boolean validateRowWithSchema(Map<String, Object> row, List<AttributeSchema> schema) {
        for (AttributeSchema attr : schema) {
            if (attr.rule != null && !attr.rule.isEmpty()) {
                Map<String, Object> context = new HashMap<>(row);
                context.put("value", row.get(attr.name));
                try {
                    Object result = MVEL.eval(attr.rule, context);
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
