package parsedata.yellowPages;

import com.princeton.database.directory.entity.*;
import com.princeton.database.global.entity.Keyword;
import java.io.*;
import java.util.*;
import parsedata.DBObject;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ParseDataYellowPages {
    
    private int divCount;
    private String dataString;
    private File dataFile;
    private BufferedReader dataReader;
    private Vector<DBObject> dboVector;
    
    public ParseDataYellowPages() {
        this.divCount = 0;
        this.dboVector = new Vector<DBObject>();
    }
    
    public Vector<DBObject> getDBOVector() {
        return this.dboVector;
    }

    public void parseString(LinkedList<String> filePathNames) {
       
        // Debugging 
        dboVector.removeAll(dboVector);
        System.out.println();
        for (int i = 0; i < filePathNames.size(); i++) {
            try {
                this.dataFile = new File(filePathNames.get(i));
                this.dataReader = new BufferedReader(new FileReader(dataFile));
            } catch (FileNotFoundException e) {
                System.err.println("Error: file not found.\n");
                continue;
            }
            try {
                while ((this.dataString = this.dataReader.readLine()) != null) {
                    if (this.dataString.matches("<div\\s*?class=\"contentlist\\s*?mainlist"
                                + "\\s*?faiklisting\\s*?\">")) {
                        this.divCount++;
                        StringBuffer businessInfoBuffer = 
                                new StringBuffer(this.dataString);
                        matchDivisions(this.dataReader, businessInfoBuffer, 
                                this.dataString, this.divCount); 
                        this.divCount = 0;
                        DBObject dbo = new DBObject();
                        boolean dboWasCreated = createDBO(businessInfoBuffer, dbo);
                        if (dboWasCreated) {
                            this.getDBOVector().add(dbo);
                            System.out.println("DBObject: " + 
                                    dbo.getOrganizationName());
                        }
                    } 
                }
            } catch (IOException e) {
                System.err.println("Input/output exception thrown.\n");
            }
        }
        System.out.println();
    }
    
    private static void matchDivisions(BufferedReader dataReader, 
            StringBuffer businessInfoBuffer, String dataString, int divCount) {
        try {
            dataString = dataReader.readLine();
            while (divCount != 0) {
                businessInfoBuffer.append(dataString);
                if (dataString.matches(".*<div.*")) {
                    String[] divDivision = dataString.split("<div");
                    divCount += (divDivision.length - 1);
                }
                if (dataString.matches(".*</div.*")) {
                    String[] endDivDivision = dataString.split("</div");
                    divCount -= (endDivDivision.length - 1);
                }
                dataString = dataReader.readLine();
            }
            businessInfoBuffer.append("</div>");
        } catch (IOException e) {
            System.err.println("Input/output exception thrown.\n");
        }
    }
    
    private static boolean createDBO(StringBuffer businessInfoBuffer, 
            DBObject dbo) {
        boolean orgNameAssigned = parseOrganizationName(businessInfoBuffer, dbo);
        if (!orgNameAssigned) {
            return false;
        }
        parseAddress(businessInfoBuffer, dbo);
        parsePhoneAndFax(businessInfoBuffer, dbo);
        setSpecialty(businessInfoBuffer, dbo);
        parseKeywordsAndIndustries(businessInfoBuffer, dbo);
        return true;
    }
    
    private static boolean parseOrganizationName(StringBuffer businessInfoBuffer,
            DBObject dbo) {
        String parsedOrgName = null;
        String businessInfoString = businessInfoBuffer.toString();
        String[] orgArray = 
                businessInfoString.split("<a\\s*?href=\".*?/profile.*?>");
        if (orgArray.length > 1) {
            String unparsedOrgName = orgArray[1];
            String[] parsedOrgNameArray = unparsedOrgName.split("</a>");
            parsedOrgName = parsedOrgNameArray[0].trim();
            if (parsedOrgName.length() > 200) {
                parsedOrgName = parsedOrgName.substring(0, 200);
            }
        }
        if (parsedOrgName != null) {
            dbo.setOrgName(parsedOrgName);
            return true;
        } else {
            return false;
        }
    }
    
    private static void parseAddress(StringBuffer businessInfoBuffer, 
            DBObject dbo) {
        String[] parsedAddressArray = 
                getAddressArray(businessInfoBuffer, dbo);
        truncateAddressArray(parsedAddressArray);
        setAddressToDBO(parsedAddressArray, dbo);
    }
    
    private static String[] getAddressArray(StringBuffer businessInfoBuffer,
            DBObject dbo) {
        String businessInfoString = businessInfoBuffer.toString();
        String[] addressArray = 
                businessInfoString.split("<div\\s*?style=\"width:\\d*?px;\">");
        String unparsedAddress = addressArray[1];
        String[] unparsedAddressArray = unparsedAddress.split("<table");
        String[] parsedAddressArray = unparsedAddressArray[0].split("<br>");
        return parsedAddressArray;
    }
    
    private static void truncateAddressArray(String[] addressArray) {
        if (addressArray.length >= 1) {
            if (addressArray[0].length() > 200) {
                addressArray[0] = addressArray[0].substring(0, 200);
            }
        }
        if (addressArray.length >= 2) {
            if (addressArray[1].length() > 200) {
                addressArray[1] = addressArray[1].substring(0, 200);
            }
        }
        if (addressArray.length >= 3) {
            if (addressArray[2].length() > 200) {
                addressArray[2] = addressArray[2].substring(0, 200);
            }
        }
        if (addressArray.length > 3) {
            if (addressArray[3].length() > 200) {
                addressArray[3] = addressArray[3].substring(0, 200);
            }
        }
    }
    
    // Not really happy with this; go back and finish at work    
    private static void setAddressToDBO(String[] addressParsedArray, 
            DBObject dbo) {
        if (addressParsedArray.length > 3) {
            dbo.setStreetName(addressParsedArray[0].trim());
            if (addressParsedArray[2].matches(".*[lL][aA][nN][dD][mM][aA][rR][kK].*")) {
                dbo.setCrossStreet(addressParsedArray[2].trim());
                String state =  separateCityAndState(addressParsedArray[1]);
                dbo.setState(state);
                String[] cityArray = addressParsedArray[1].split(",");
                
                dbo.setCity(cityArray[0].trim());
            } else {
                dbo.setCity(addressParsedArray[1].trim() + "\n" 
                        + addressParsedArray[2].trim());
            }
            
            // Check unit for city here
            String address = pullOutPOAndState(addressParsedArray[3].trim(), dbo);
            //dbo.setUnit(addressParsedArray[3].trim());
        } else if (addressParsedArray.length > 2) {
            dbo.setStreetName(addressParsedArray[0].trim());
            if (addressParsedArray[1].matches(".*[lL][aA][nN][dD][mM][aA][rR][kK].*")) {
                dbo.setCrossStreet(addressParsedArray[1].trim());
            } else {
                String[] cityArray = addressParsedArray[1].split(",");
                
                dbo.setCity(cityArray[0].trim());
            }
            
            // check unit for city here
            String address = pullOutPOAndState(addressParsedArray[2].trim(), dbo);
            if (address.matches("^[\\W]*$")) {
                return;
            } else {
                dbo.setUnit(address.trim());
            }
        } else if (addressParsedArray.length > 1) {
            dbo.setStreetName(addressParsedArray[0].trim());
            
            String address = pullOutPOAndState(addressParsedArray[1].trim(), dbo);
            if (dbo.getUnit() ==  null) {
                dbo.setUnit(addressParsedArray[1].trim());
            }
        } else if (addressParsedArray.length == 1) {
            String addressString = addressParsedArray[0];
            String address = pullOutPOAndState(addressString, dbo);
            if (address.matches("^[\\W]*$")) {
                return;
            } else {
                dbo.setStreetName(address.replaceAll("^[\\W]", "").trim());
                
            }
        }
    }
    
    private static String separateCityAndState(String cityAndState) {
        String cityAndStateUpperCase = cityAndState.toUpperCase();
        StringTokenizer cityAndStateTokenized = 
                new StringTokenizer(cityAndStateUpperCase, ",");
        while(cityAndStateTokenized.hasMoreTokens()) {
            String curToken = cityAndStateTokenized.nextToken();
            if (curToken.contains("DUBAI")) {
                return "Dubai";
            } else if (curToken.contains("ABU DHABI")) {
                return "Abu Dhabi";
            } else if (curToken.contains("AJMAN")) {
                return "Ajman";
            } else if (curToken.contains("FUJAIRAH")) {
                return "Fujairah";
            } else if (curToken.contains("RAS AL KHAIMAH")) {
                return "Ras al-Khaimah";
            } else if (curToken.contains("RAS AL-KHAIMAH")) {
                return "Ras al-Khaimah";
            } else if (curToken.contains("SHARJAH")) {
                return "Sharjah";
            } else if (curToken.contains("UMM AL QUWAIN")) {
                return "Umm al-Quwain";
            } else if (curToken.contains("UMM AL-QUWAIN")) {
                return "Umm al-Quwain";
            }
        }
        return null;
    }
    
    private static String pullOutPOAndState(String oneLineAddress
            , DBObject dbo) {
        Pattern POPatt = Pattern.compile("(.*)([pP][.]?[oO][.]?[bB][oO][xX][:]?[\\s]?[\\d]*)(.*)");
        Matcher POMatch = POPatt.matcher(oneLineAddress);
        if (POMatch.matches()) {
            dbo.setUnit(POMatch.group(2));
            oneLineAddress = POMatch.group(1) + POMatch.group(3);
        }
        Pattern dubaiPatt = Pattern.compile("(.*)([dD][uU][bB][aA][iI])(.*)");
        Pattern abuDhabiPatt = Pattern.compile("(.*)([aA][bB][uU][\\s-]*[dD][hH][aA][bB][iI])(.*)");
        Pattern ajmanPatt =  Pattern.compile("(.*)([aA][jJ][mM][aA][nN])(.*)");
        Pattern fujairahPatt = Pattern.compile("(.*)([fF][uU][jJ][aA][iI][rR][aA][hH])(.*)");
        Pattern rasAlKhaimahPatt = Pattern.compile("(.*)([rR][aA][sS][\\s-]*[aA][lL][\\s-]*[kK][hH][aA][iI][mM][aA][hH])(.*)");
        Pattern sharjahPatt =  Pattern.compile("(.*)([sS][hH][aA][rR][jJ][aA][hH])(.*)");
        Pattern ummAlQuwainPatt = Pattern.compile("(.*)([uU][mM][mM]?[\\s-]*[aA][lL][\\s-]*[qQ][uU][wW][aA][iI][nN])(.*)");
        Matcher dubaiMatch = dubaiPatt.matcher(oneLineAddress);
        Matcher abuDhabiMatch = abuDhabiPatt.matcher(oneLineAddress);
        Matcher ajmanMatch = ajmanPatt.matcher(oneLineAddress);
        Matcher fujairahMatch = fujairahPatt.matcher(oneLineAddress);
        Matcher rasAlKhaimahMatch = rasAlKhaimahPatt.matcher(oneLineAddress);
        Matcher sharjahMatch = sharjahPatt.matcher(oneLineAddress);
        Matcher ummAlQuwainMatch = ummAlQuwainPatt.matcher(oneLineAddress);
        if (dubaiMatch.matches()) {
            dbo.setState(dubaiMatch.group(2));
            oneLineAddress = dubaiMatch.group(1) + dubaiMatch.group(3);
        } else if (abuDhabiMatch.matches()) {
            dbo.setState(abuDhabiMatch.group(2));
            oneLineAddress = abuDhabiMatch.group(1) + abuDhabiMatch.group(3);
        } else if (ajmanMatch.matches()) {
            dbo.setState(ajmanMatch.group(2));
            oneLineAddress = ajmanMatch.group(1) + ajmanMatch.group(3);
        } else if (fujairahMatch.matches()) {
            dbo.setState(fujairahMatch.group(2));
            oneLineAddress = fujairahMatch.group(1) + fujairahMatch.group(3);
        } else if (rasAlKhaimahMatch.matches()) {
            dbo.setState(rasAlKhaimahMatch.group(2));
            oneLineAddress = rasAlKhaimahMatch.group(1) + rasAlKhaimahMatch.group(3);
        } else if (sharjahMatch.matches()) {
            dbo.setState(sharjahMatch.group(2));
            oneLineAddress = sharjahMatch.group(1) + sharjahMatch.group(3);
        } else if (ummAlQuwainMatch.matches()) {
            dbo.setState(ummAlQuwainMatch.group(2));
            oneLineAddress = ummAlQuwainMatch.group(1) + ummAlQuwainMatch.group(3);
        }
        return oneLineAddress;
    }
    
    private static void parsePhoneAndFax(StringBuffer businessInfoBuffer, 
            DBObject dbo) {
        String[] telCountArray = getTelCountArray(businessInfoBuffer);
        String[] faxCountArray = getFaxCountArray(businessInfoBuffer);
        String[] parsedPhoneArray = getParsedPhoneString(telCountArray);
        String parsedFaxString = getParsedFaxArray(faxCountArray);
        setAndTruncatePhones(parsedPhoneArray, dbo);
        setAndTruncateFax(parsedFaxString, dbo);
    }
    
    private static String[] getTelCountArray(StringBuffer businessInfoBuffer) {
        String businessInfoString = businessInfoBuffer.toString();
        String[] telCountArray = businessInfoString.split("Tel:");
        return telCountArray;
    }
    
    private static String[] getFaxCountArray(StringBuffer businessInfoBuffer) {
        String businessInfoString = businessInfoBuffer.toString();
        String[] faxCountArray = businessInfoString.split("Fax:");
        return faxCountArray;
    }
    
    private static String[] getParsedPhoneString(String[] telCountArray) {
        if (telCountArray.length > 1) {
            String[] unparsedPhoneArray = 
                    telCountArray[1].split("<td\\s*?style=\"direction:ltr\">");
            if (unparsedPhoneArray.length > 1) {
                String unparsedPhoneString = unparsedPhoneArray[1];
                String[] parsedPhoneArray = unparsedPhoneString.split("</td");
                String parsedPhoneString = parsedPhoneArray[0];
                parsedPhoneArray = parsedPhoneString.split(
                        "<b>");
                if (parsedPhoneArray.length > 1) {
                    parsedPhoneArray = parsedPhoneArray[1].split(
                            "</b>");
                }
                parsedPhoneString = parsedPhoneArray[0].trim();
                parsedPhoneArray = 
                        parsedPhoneString.split(",");
                return parsedPhoneArray;
            } else {
                return null;
            }
        } else {
             return null;
        }
    }
    
    private static String getParsedFaxArray(String[] faxCountArray) {
        if (faxCountArray.length > 1) { 
            String [] faxArray = faxCountArray[1].
                    split("<td\\s*?style=\"direction:ltr\">");
           if (faxArray.length > 1) {
               String unparsedFaxString = faxArray[1];
               String[] parsedFaxArray = unparsedFaxString.split("</td>");
               String parsedFaxString = parsedFaxArray[0].trim();
               return parsedFaxString;
           } else {
               return null;
           }
        } else {
            return null;
        }
    }
    
    // Not really happy with this either; change at work
    private static void setAndTruncatePhones(String[] parsedPhoneArray,
            DBObject dbo) {
        if (parsedPhoneArray != null) {
            if (parsedPhoneArray.length > 1) {
                if (parsedPhoneArray[0].length() > 200) {
                    parsedPhoneArray[0] = parsedPhoneArray[0].substring(0, 200);
                }
                parsedPhoneArray = formatPhones(parsedPhoneArray);
                if (!parsedPhoneArray[0].equals("")) {
                    dbo.setPhone1(parsedPhoneArray[0]);
                    dbo.setPhone1Type("PRIMARY");
                }
                if (parsedPhoneArray[1].length() > 200) {
                    parsedPhoneArray[1] = parsedPhoneArray[1].substring(0, 200);
                }
                if (!parsedPhoneArray[1].equals("") && 
                        !parsedPhoneArray[1].equals(parsedPhoneArray[0])) {
                    dbo.setPhone2(parsedPhoneArray[1]);
                    dbo.setPhone2Type("SECONDARY");
                }
            } else {
                if (parsedPhoneArray[0].length() > 200) {
                    parsedPhoneArray[0] = parsedPhoneArray[0].substring(0, 200);
                }
                String phone = formatPhone(parsedPhoneArray[0]);
                if (!phone.equals("")) {
                    dbo.setPhone1(phone);
                    dbo.setPhone1Type("PRIMARY");
                }
            }
        }
    }
    
    private static String[] formatPhones(String[] unformattedPhones) {
        unformattedPhones[0] = unformattedPhones[0].replaceAll("[\\W]", "");
        unformattedPhones[1] = unformattedPhones[1].replaceAll("[\\W]", "");
        Pattern sevenDigitPatt = Pattern.compile("(^[\\d]{3})([\\d]{4}$)");
        Pattern eightDigitPatt = Pattern.compile("(^[\\d]{1,2}?)([\\d]{3})([\\d]{4}$)");
        Pattern tenPlusDigitPatt = Pattern.compile("(^[\\d]{3})([\\d]{0,2}?)([\\d]{3})([\\d]{4}$)");
        Matcher sevenDigitMatchOne = 
                        sevenDigitPatt.matcher(unformattedPhones[0]);
        Matcher sevenDigitMatchTwo = 
                sevenDigitPatt.matcher(unformattedPhones[1]);
        Matcher eightDigitMatchOne = 
                eightDigitPatt.matcher(unformattedPhones[0]);
        Matcher eightDigitMatchTwo = 
                eightDigitPatt.matcher(unformattedPhones[1]);
        Matcher tenPlusDigitMatchOne = 
                tenPlusDigitPatt.matcher(unformattedPhones[0]);
        Matcher tenPlusDigitMatchTwo = 
                tenPlusDigitPatt.matcher(unformattedPhones[1]);
        if (sevenDigitMatchOne.matches()) {
            unformattedPhones[0] = sevenDigitMatchOne.group(1) + "-" +
                    sevenDigitMatchOne.group(2);
        } else if (eightDigitMatchOne.matches()) {
            unformattedPhones[0] = eightDigitMatchOne.group(1) + "-" +
                    eightDigitMatchOne.group(2) + "-" + 
                    eightDigitMatchOne.group(3);
        } else if (tenPlusDigitMatchOne.matches()) {
            unformattedPhones[0] = tenPlusDigitMatchOne.group(1) + "-" + 
                    tenPlusDigitMatchOne.group(2) + "-" + 
                    tenPlusDigitMatchOne.group(3) + "-" + 
                    tenPlusDigitMatchOne.group(4);
        } else {
            unformattedPhones[0] = "";
        }
        if (sevenDigitMatchTwo.matches()) {
            unformattedPhones[1] = sevenDigitMatchTwo.group(1) + "-" +
                    sevenDigitMatchTwo.group(2);
        } else if (eightDigitMatchTwo.matches()) {
            unformattedPhones[1] = eightDigitMatchTwo.group(1) + "-" +
                    eightDigitMatchTwo.group(2) + "-" + 
                    eightDigitMatchTwo.group(3);
        } else if (tenPlusDigitMatchTwo.matches()) {
             unformattedPhones[1] = tenPlusDigitMatchTwo.group(1) + "-" + 
                    tenPlusDigitMatchTwo.group(2) + "-" + 
                    tenPlusDigitMatchTwo.group(3) + "-" + 
                    tenPlusDigitMatchTwo.group(4);
        } else {
            unformattedPhones[1] = "";
        }
        
        return unformattedPhones;
    }
    
    private static String formatPhone(String unformattedPhone) {
        unformattedPhone = unformattedPhone.replaceAll("[\\W]", "");
        Pattern sevenDigitPatt = Pattern.compile("(^[\\d]{3})([\\d]{4}$)");
        Pattern eightDigitPatt = Pattern.compile("(^[\\d]{1,2}?)([\\d]{3})([\\d]{4}$)");
        Pattern tenPlusDigitPatt = Pattern.compile("(^[\\d]{3})([\\d]{0,2}?)([\\d]{3})([\\d]{4}$)");
        Matcher sevenDigitMatch = 
                        sevenDigitPatt.matcher(unformattedPhone);
        Matcher eightDigitMatch = 
                eightDigitPatt.matcher(unformattedPhone);
        Matcher tenPlusDigitMatch = 
                tenPlusDigitPatt.matcher(unformattedPhone);
        if (sevenDigitMatch.matches()) {
            unformattedPhone = sevenDigitMatch.group(1) + "-" +
                    sevenDigitMatch.group(2);
        } else if (eightDigitMatch.matches()) {
            unformattedPhone = eightDigitMatch.group(1) + "-" +
                    eightDigitMatch.group(2) + "-" + eightDigitMatch.group(3);
        } else if (tenPlusDigitMatch.matches()) {
            unformattedPhone = tenPlusDigitMatch.group(1) + "-" + 
                    tenPlusDigitMatch.group(2) + "-" + tenPlusDigitMatch.group(3)
                    + "-" + tenPlusDigitMatch.group(4);
        } else {
            unformattedPhone = "";
        }
        return unformattedPhone;
    }
    
    private static void setAndTruncateFax(String parsedFaxString, 
            DBObject dbo) {
        if (parsedFaxString != null) {
            if (parsedFaxString.length() > 200) {
                parsedFaxString = parsedFaxString.substring(0, 200);
            }
            dbo.setFax(parsedFaxString);
        }
    }
    
    private static void setSpecialty(StringBuffer businessInfoBuffer, 
            DBObject dbo) {
        String parsedSpecialtyString = 
                getParsedSpecialtyString(businessInfoBuffer);
        setSpecialtyString(parsedSpecialtyString, dbo);
    }
    
    private static String getParsedSpecialtyString(StringBuffer 
            businessInfoBuffer) {
        String businessInfoString = businessInfoBuffer.toString();
        String[] unparsedSpecialtyArray = 
        businessInfoString.split("<div\\s*?class=\"faikheading_red_txt\"\\s*?id="
                + "\"faikheading_red_txt\\d*\"\\s*?>");
        String parsedSpecialtyString = null;
        if (unparsedSpecialtyArray.length > 1) {
            String[] parsedSpecialtyArray = unparsedSpecialtyArray[1].
                    split("</div>");
            parsedSpecialtyString = parsedSpecialtyArray[0].trim();
            if (parsedSpecialtyString.length() > 200) {
                parsedSpecialtyString = parsedSpecialtyString.substring(0, 200);
            }
            return parsedSpecialtyString;
        } else {
            return "";
        }
    }
    
    private static void setSpecialtyString(String parsedSpecialtyString, 
            DBObject dbo) {
        dbo.setSpecialty(parsedSpecialtyString);
    }
    
    private static void parseKeywordsAndIndustries(StringBuffer 
            businessInfoBuffer, DBObject dbo) {
        getKeywordsAndIndustries(businessInfoBuffer, dbo);
    }
    
    // This should really be multiple methods
    private static void getKeywordsAndIndustries(StringBuffer businessInfoBuffer, 
            DBObject dbo) {
        String businessInfoString = businessInfoBuffer.toString();
        String[] unparsedKeywordAndIndustryArray = 
                businessInfoString.split("<div\\s*?class=\"catkeys.*?\"");
        if (unparsedKeywordAndIndustryArray.length > 2) {
            String[] unparsedKeywordArray = unparsedKeywordAndIndustryArray[1].
                    split("Keyword\\(s\\):");
            if (unparsedKeywordArray.length > 1) {
                String[] parsedKeywordArray = unparsedKeywordArray[1].
                split("</div>");
                String[] keywords = parsedKeywordArray[0].split("\\s*\\|\\s*");
                for (String keyword:  keywords) {
                    setKeyword(keyword, dbo);
                }
            }
            String[] unparsedIndustryArray = unparsedKeywordAndIndustryArray[2].
                    split("Category\\(s\\):(.*index.html\">)*");
            if (unparsedIndustryArray.length > 1) {
                String[] industryArrayParsed = unparsedIndustryArray[1].
                        split("(</div>|</a>)");
                String[] industries = 
                        industryArrayParsed[0].split("\\s*\\|\\s*");
                for (String industry:  industries) {
                    setIndustry(industry, dbo);
                }
            }
        } else if(unparsedKeywordAndIndustryArray.length > 1) {
            if (unparsedKeywordAndIndustryArray[1].
                    matches(".*?Keyword\\(s\\):.*?")) {
                String[] unparsedKeywordArray = unparsedKeywordAndIndustryArray[1].
                        split("Keyword\\(s\\):");
                String[] keywords = isolateKeywords(unparsedKeywordArray, dbo);
                for (String keyword: keywords) {
                    setKeyword(keyword, dbo);
                }
            } else if (unparsedKeywordAndIndustryArray[1].
                    matches(".*?Category\\(s\\):.*?")) {
                String[] unparsedIndustryArray = unparsedKeywordAndIndustryArray[1].
                        split("Category\\(s\\):(.*index.html\">)*");
                String[] industries = isolateIndustries(unparsedIndustryArray, 
                        dbo);
                for (String industry:  industries) {
                    setIndustry(industry, dbo);
                }
            }
        }
    }
    
    private static void setKeyword(String keyword, DBObject dbo) {
        keyword = keyword.trim();
        if (keyword.length() > 200) {
            keyword = keyword.substring(0, 200);
        }
        Keyword key = new Keyword();
        key.setKeyword(keyword);
        dbo.getKeywords().add(key);
    }
    
    private static void setIndustry(String industry, DBObject dbo) {
        industry = industry.trim();
        if (industry.length() > 200) {
            industry = industry.substring(0, 200);
        }
        Industry ind = new Industry();
        ind.setName(industry);
        dbo.getIndustries().add(ind);
    }
    
    private static String[] isolateKeywords(String[] unparsedKeywordArray, 
            DBObject dbo) {
        if (unparsedKeywordArray.length > 1) {
            String[] parsedKeywordArray = unparsedKeywordArray[1].
            split("</div>");
            String[] keywords = parsedKeywordArray[0].split("\\|");
            return keywords;
        } else {
            return null;
        }
    }
    
    private static String[] isolateIndustries(String[] unparsedIndustryArray,
            DBObject dbo) {
        if (unparsedIndustryArray.length > 1) {
            String[] parsedIndustryArray = unparsedIndustryArray[1].
                    split("(</div>|</a>)");
            String[] industries = parsedIndustryArray[0].split("\\|");
            return industries;
        } else {
            return null;
        }
    }
}